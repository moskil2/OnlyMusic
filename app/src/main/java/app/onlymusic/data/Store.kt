package app.onlymusic.data

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Plain JSON files in the app's private storage. Small, no database library needed. */
class Store(dir: File) {
    private val tracksFile = AtomicFile(File(dir, "tracks.json"))
    private val foldersFile = AtomicFile(File(dir, "folders.json"))
    private val playlistsFile = AtomicFile(File(dir, "playlists.json"))
    private val favoritesFile = AtomicFile(File(dir, "favorites.json"))

    fun loadTracks(): List<Track> = readArray(tracksFile) { o ->
        Track(
            id = o.getString("id"),
            folderId = o.getString("folder"),
            fileName = o.optString("file"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            album = o.optString("album"),
            durationMs = o.optLong("dur"),
            format = o.optString("fmt"),
            modified = o.optLong("mod"),
            added = o.optLong("added"),
            hasArt = o.optBoolean("art", true),
        )
    }

    fun saveTracks(list: List<Track>) = writeArray(tracksFile, list) { t ->
        JSONObject()
            .put("id", t.id).put("folder", t.folderId).put("file", t.fileName)
            .put("title", t.title).put("artist", t.artist).put("album", t.album)
            .put("dur", t.durationMs).put("fmt", t.format).put("mod", t.modified).put("added", t.added)
            .put("art", t.hasArt)
    }

    fun loadFavorites(): Set<String> = try {
        val arr = JSONArray(String(favoritesFile.readFully(), Charsets.UTF_8))
        List(arr.length()) { arr.getString(it) }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    @Synchronized
    fun saveFavorites(ids: Set<String>) {
        val out = favoritesFile.startWrite()
        try {
            out.write(JSONArray(ids.toList()).toString().toByteArray(Charsets.UTF_8))
            favoritesFile.finishWrite(out)
        } catch (e: Exception) {
            favoritesFile.failWrite(out)
        }
    }

    fun loadFolders(): List<MusicFolder> = readArray(foldersFile) { o ->
        MusicFolder(
            id = o.getString("id"),
            name = o.optString("name"),
            path = o.optString("path"),
            trackCount = o.optInt("count"),
            lastScan = o.optLong("scan"),
        )
    }

    fun saveFolders(list: List<MusicFolder>) = writeArray(foldersFile, list) { f ->
        JSONObject().put("id", f.id).put("name", f.name).put("path", f.path)
            .put("count", f.trackCount).put("scan", f.lastScan)
    }

    fun loadPlaylists(): List<Playlist> = readArray(playlistsFile) { o ->
        val ids = o.optJSONArray("tracks") ?: JSONArray()
        Playlist(
            id = o.getString("id"),
            name = o.optString("name"),
            trackIds = List(ids.length()) { ids.getString(it) },
        )
    }

    fun savePlaylists(list: List<Playlist>) = writeArray(playlistsFile, list) { p ->
        JSONObject().put("id", p.id).put("name", p.name).put("tracks", JSONArray(p.trackIds))
    }

    private fun <T> readArray(file: AtomicFile, parse: (JSONObject) -> T): List<T> = try {
        val arr = JSONArray(String(file.readFully(), Charsets.UTF_8))
        List(arr.length()) { parse(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    private fun <T> writeArray(file: AtomicFile, list: List<T>, toJson: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        val out = file.startWrite()
        try {
            out.write(arr.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
        }
    }
}
