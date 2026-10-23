package app.spotamp.data

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class LibraryRepository(private val context: Context, private val scope: CoroutineScope) {
    private val store = Store(context.filesDir)
    private val scanner = FolderScanner(context)
    private val scanLock = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tagReaders = Dispatchers.IO.limitedParallelism(4)
    val prefs = Prefs(context)

    private val _folders = MutableStateFlow(store.loadFolders())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _tracks = MutableStateFlow(store.loadTracks())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    val trackMap: StateFlow<Map<String, Track>> = _tracks
        .map { list -> list.associateBy { it.id } }
        .stateIn(scope, SharingStarted.Eagerly, _tracks.value.associateBy { it.id })

    private val _playlists = MutableStateFlow(store.loadPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    /** Folder id to progress, only while that folder is being read. */
    private val _scans = MutableStateFlow<Map<String, ScanProgress>>(emptyMap())
    val scans: StateFlow<Map<String, ScanProgress>> = _scans.asStateFlow()

    private val _favorites = MutableStateFlow(store.loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // Folder watching: while the app runs, changes in a picked folder trigger a re-read.
    private val handler = Handler(Looper.getMainLooper())
    private val observers = mutableMapOf<String, ContentObserver>()
    private val pendingRescans = mutableMapOf<String, Runnable>()

    init {
        scope.launch(Dispatchers.Main) {
            combine(_folders, prefs.settings.map { it.watchFolders }.distinctUntilChanged()) { list, on ->
                if (on) list.filterNot { it.unavailable }.map { it.id }.toSet() else emptySet()
            }.distinctUntilChanged().collect { syncObservers(it) }
        }
    }

    /** Also finds tracks added a moment ago, before the lookup map has caught up. */
    fun track(id: String): Track? = trackMap.value[id] ?: _tracks.value.firstOrNull { it.id == id }

    fun toggleFavorite(id: String) {
        _favorites.update { if (id in it) it - id else it + id }
        val snapshot = _favorites.value
        scope.launch(Dispatchers.IO) { store.saveFavorites(snapshot) }
    }

    private fun syncObservers(ids: Set<String>) {
        val resolver = context.contentResolver
        (observers.keys - ids).forEach { id -> observers.remove(id)?.let { resolver.unregisterContentObserver(it) } }
        (ids - observers.keys).forEach { id ->
            val tree = Uri.parse(id)
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) = scheduleRescan(id)
            }
            runCatching {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                resolver.registerContentObserver(children, true, observer)
                observers[id] = observer
            }
        }
    }

    /** Waits for copying to settle before reading the folder again. */
    private fun scheduleRescan(id: String) {
        pendingRescans.remove(id)?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            pendingRescans.remove(id)
            rescan(id)
        }
        pendingRescans[id] = r
        handler.postDelayed(r, 4_000)
    }

    // ---- Folders ----

    fun addFolder(treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val id = treeUri.toString()
        if (_folders.value.any { it.id == id }) {
            rescan(id)
            return
        }
        val (name, path) = FolderScanner.describe(treeUri)
        val folder = MusicFolder(id = id, name = name, path = path, trackCount = 0, lastScan = 0L)
        _folders.update { it + folder }
        store.saveFolders(_folders.value)
        rescan(id)
    }

    fun removeFolder(id: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(id), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        _folders.update { list -> list.filterNot { it.id == id } }
        _tracks.update { list -> list.filterNot { it.folderId == id } }
        scope.launch(Dispatchers.IO) {
            store.saveFolders(_folders.value)
            store.saveTracks(_tracks.value)
        }
    }

    fun rescanAll() = _folders.value.forEach { rescan(it.id) }

    fun rescan(folderId: String) {
        scope.launch(Dispatchers.IO) {
            scanLock.withLock { scanFolder(folderId) }
        }
    }

    private suspend fun scanFolder(folderId: String) {
        if (_folders.value.none { it.id == folderId }) return
        val settings = prefs.settings.value
        // total 0 = still looking for files
        _scans.update { it + (folderId to ScanProgress(0, 0)) }
        val entries = try {
            scanner.listAudio(Uri.parse(folderId), settings.includeSubfolders)
        } catch (e: Exception) {
            // Folder moved, card removed or access revoked: keep known tracks, mark the folder.
            _folders.update { list -> list.map { if (it.id == folderId) it.copy(unavailable = true) else it } }
            _scans.update { it - folderId }
            return
        }
        val known = _tracks.value.filter { it.folderId == folderId }.associateBy { it.id }
        val now = System.currentTimeMillis()
        val result = ArrayList<Track>(entries.size)
        var processed = 0
        _scans.update { it + (folderId to ScanProgress(0, entries.size)) }
        // Tags are read a few files at a time; each batch shows up in the library right away.
        entries.chunked(SCAN_BATCH).forEach { batch ->
            val read = coroutineScope {
                batch.map { entry ->
                    async(tagReaders) {
                        val old = known[entry.uri.toString()]
                        if (old != null && old.modified == entry.modified && old.durationMs > 0) {
                            old
                        } else {
                            scanner.readTrack(entry, folderId, old?.added ?: now)
                        }
                    }
                }.awaitAll()
            }
            read.filterTo(result) { !(settings.skipShort && it.durationMs in 1 until 30_000) }
            processed += batch.size
            val notYetRead = entries.drop(processed).mapNotNull { known[it.uri.toString()] }
            val snapshot = result + notYetRead
            _tracks.update { list -> list.filterNot { it.folderId == folderId } + snapshot }
            _scans.update { it + (folderId to ScanProgress(processed, entries.size)) }
        }
        _tracks.update { list -> list.filterNot { it.folderId == folderId } + result }
        _folders.update { list ->
            list.map {
                if (it.id == folderId) it.copy(trackCount = result.size, lastScan = now, unavailable = false) else it
            }
        }
        _scans.update { it - folderId }
        store.saveTracks(_tracks.value)
        store.saveFolders(_folders.value)
    }

    // ---- Playlists ----

    fun createPlaylist(name: String, trackIds: List<String> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        _playlists.update { it + Playlist(id, name.trim().ifEmpty { "New playlist" }, trackIds) }
        savePlaylists()
        return id
    }

    fun renamePlaylist(id: String, name: String) = editPlaylist(id) { it.copy(name = name.trim().ifEmpty { it.name }) }

    fun deletePlaylist(id: String) {
        _playlists.update { list -> list.filterNot { it.id == id } }
        savePlaylists()
    }

    fun restorePlaylist(playlist: Playlist, index: Int) {
        _playlists.update { list ->
            list.toMutableList().apply { add(index.coerceIn(0, size), playlist) }
        }
        savePlaylists()
    }

    fun addToPlaylist(id: String, trackIds: List<String>) = editPlaylist(id) { it.copy(trackIds = it.trackIds + trackIds) }

    fun removeFromPlaylist(id: String, index: Int) = editPlaylist(id) {
        it.copy(trackIds = it.trackIds.toMutableList().apply { if (index in indices) removeAt(index) })
    }

    fun moveInPlaylist(id: String, from: Int, to: Int) = editPlaylist(id) {
        val ids = it.trackIds.toMutableList()
        if (from in ids.indices && to in ids.indices) ids.add(to, ids.removeAt(from))
        it.copy(trackIds = ids)
    }

    fun movePlaylist(from: Int, to: Int) {
        _playlists.update { list ->
            val l = list.toMutableList()
            if (from in l.indices && to in l.indices) l.add(to, l.removeAt(from))
            l
        }
        savePlaylists()
    }

    private fun editPlaylist(id: String, change: (Playlist) -> Playlist) {
        _playlists.update { list -> list.map { if (it.id == id) change(it) else it } }
        savePlaylists()
    }

    private companion object {
        const val SCAN_BATCH = 25
    }

    private fun savePlaylists() {
        val snapshot = _playlists.value
        scope.launch(Dispatchers.IO) { store.savePlaylists(snapshot) }
    }
}
