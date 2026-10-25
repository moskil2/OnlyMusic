package app.spotamp.data

/** One audio file. [id] is its document Uri, which is also the Media3 media id. */
data class Track(
    val id: String,
    val folderId: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val format: String,
    val modified: Long,
    val added: Long,
    /** The file carries its own cover image. Without one, SpotAmp shows its wave artwork. */
    val hasArt: Boolean = true,
)

/** A folder the user picked. [id] is the tree Uri with a persisted read grant. */
data class MusicFolder(
    val id: String,
    val name: String,
    val path: String,
    val trackCount: Int,
    val lastScan: Long,
    val unavailable: Boolean = false,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)

data class ScanProgress(val done: Int, val total: Int)

data class Settings(
    val accent: Int = 0,
    val waveStyle: Int = 0,
    val skipShort: Boolean = true,
    val includeSubfolders: Boolean = true,
    val watchFolders: Boolean = true,
    val gapless: Boolean = true,
    val pauseOnDisconnect: Boolean = true,
    val resumeOnConnect: Boolean = false,
    /** Crossfade length in seconds, 0 = off. */
    val crossfadeSec: Int = 0,
    /** Player panel folded up, the wave drawn behind the title and buttons. */
    val playerCollapsed: Boolean = false,
)

val CROSSFADE_STEPS = listOf(0, 2, 4, 6, 8)
