package app.spotamp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("spotamp", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        Settings(
            accent = sp.getInt("accent", 0),
            waveStyle = sp.getInt("wave", 0),
            skipShort = sp.getBoolean("skipShort", true),
            includeSubfolders = sp.getBoolean("subfolders", true),
            watchFolders = sp.getBoolean("watchFolders", true),
            gapless = sp.getBoolean("gapless", true),
            pauseOnDisconnect = sp.getBoolean("pauseOnDisconnect", true),
            resumeOnConnect = sp.getBoolean("resumeOnConnect", false),
            crossfadeSec = sp.getInt("crossfade", 0),
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(change: (Settings) -> Settings) {
        val s = change(_settings.value)
        _settings.value = s
        sp.edit()
            .putInt("accent", s.accent)
            .putInt("wave", s.waveStyle)
            .putBoolean("skipShort", s.skipShort)
            .putBoolean("subfolders", s.includeSubfolders)
            .putBoolean("watchFolders", s.watchFolders)
            .putBoolean("gapless", s.gapless)
            .putBoolean("pauseOnDisconnect", s.pauseOnDisconnect)
            .putBoolean("resumeOnConnect", s.resumeOnConnect)
            .putInt("crossfade", s.crossfadeSec)
            .apply()
    }

    // ---- Recent searches (kept on this phone only) ----

    private val _recentSearches = MutableStateFlow(readList("recentSearches"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val list = (listOf(q) + _recentSearches.value.filterNot { it.equals(q, ignoreCase = true) }).take(6)
        _recentSearches.value = list
        sp.edit().putString("recentSearches", JSONArray(list).toString()).apply()
    }

    // ---- Queue saved by the playback service so playback can resume after a restart ----

    data class SavedQueue(val name: String, val ids: List<String>, val index: Int, val positionMs: Long)

    fun saveQueue(q: SavedQueue) {
        sp.edit()
            .putString("queueName", q.name)
            .putString("queueIds", JSONArray(q.ids).toString())
            .putInt("queueIndex", q.index)
            .putLong("queuePos", q.positionMs)
            .apply()
    }

    fun savePosition(index: Int, positionMs: Long) {
        sp.edit().putInt("queueIndex", index).putLong("queuePos", positionMs).apply()
    }

    fun loadQueue(): SavedQueue = SavedQueue(
        name = sp.getString("queueName", "") ?: "",
        ids = readList("queueIds"),
        index = sp.getInt("queueIndex", 0),
        positionMs = sp.getLong("queuePos", 0L),
    )

    var queueName: String
        get() = sp.getString("queueName", "") ?: ""
        set(value) {
            sp.edit().putString("queueName", value).apply()
        }

    private fun readList(key: String): List<String> = try {
        val arr = JSONArray(sp.getString(key, "[]"))
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}
