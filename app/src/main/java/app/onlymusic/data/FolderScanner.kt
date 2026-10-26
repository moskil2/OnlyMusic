package app.onlymusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * Reads audio files from a folder picked with the system folder picker.
 * Uses only the read grant for that folder, no storage permission.
 */
class FolderScanner(private val context: Context) {

    class Entry(val uri: Uri, val name: String, val mime: String, val modified: Long)

    /**
     * Audio files in folder order, as Winamp adds a folder: the files of a folder by name,
     * then each subfolder by name, the same way down.
     */
    fun listAudio(treeUri: Uri, includeSubfolders: Boolean): List<Entry> {
        val out = ArrayList<Entry>()
        collect(treeUri, DocumentsContract.getTreeDocumentId(treeUri), includeSubfolders, out)
        return out
    }

    private fun collect(treeUri: Uri, docId: String, includeSubfolders: Boolean, out: MutableList<Entry>) {
        val files = ArrayList<Entry>()
        val folders = ArrayList<Pair<String, String>>()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        context.contentResolver.query(children, PROJECTION, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1) ?: ""
                val mime = c.getString(2) ?: ""
                val modified = if (c.isNull(3)) 0L else c.getLong(3)
                if (id == null) {
                    // Skip entries the provider could not describe.
                } else if (mime == Document.MIME_TYPE_DIR) {
                    if (includeSubfolders && !name.startsWith(".")) folders.add(id to name)
                } else if (isAudio(mime, name)) {
                    files.add(Entry(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), name, mime, modified))
                }
            }
        }
        files.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        out.addAll(files)
        folders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.second })
            .forEach { (id, _) -> collect(treeUri, id, includeSubfolders, out) }
    }

    fun readTrack(entry: Entry, folderId: String, added: Long): Track {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        var hasArt = false
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, entry.uri)
            hasArt = retriever.embeddedPicture != null
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            // Unreadable tags: fall back to the file name below.
        } finally {
            runCatching { retriever.release() }
        }
        return Track(
            id = entry.uri.toString(),
            folderId = folderId,
            fileName = entry.name,
            title = title?.trim().takeUnless { it.isNullOrEmpty() } ?: entry.name.substringBeforeLast('.'),
            artist = artist?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unknown artist",
            album = album?.trim() ?: "",
            durationMs = duration,
            format = formatOf(entry.mime, entry.name),
            modified = entry.modified,
            added = added,
            hasArt = hasArt,
        )
    }

    companion object {
        private val PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_LAST_MODIFIED,
        )

        private val EXTENSIONS = setOf("mp3", "flac", "ogg", "oga", "opus", "m4a", "aac", "wav", "wma", "mka")

        fun isAudio(mime: String, name: String): Boolean =
            mime.startsWith("audio/") || name.substringAfterLast('.', "").lowercase() in EXTENSIONS

        fun formatOf(mime: String, name: String): String {
            val ext = name.substringAfterLast('.', "").uppercase()
            if (ext.isNotEmpty() && ext.length <= 4) return ext
            return mime.substringAfter("audio/", "").uppercase()
        }

        /** "primary:Music/Rips" becomes "Internal storage / Music / Rips". */
        fun describe(treeUri: Uri): Pair<String, String> {
            val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrDefault("")
            val volume = docId.substringBefore(':', "")
            val rel = docId.substringAfter(':', docId).trim('/')
            val volumeName = if (volume.equals("primary", true)) "Internal storage" else "SD card"
            val parts = rel.split('/').filter { it.isNotEmpty() }
            val name = parts.lastOrNull() ?: volumeName
            val path = (listOf(volumeName) + parts).joinToString(" / ")
            return name to path
        }
    }
}
