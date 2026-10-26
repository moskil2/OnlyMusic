package app.onlymusic

import android.app.Application
import app.onlymusic.data.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OnlyMusicApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val library: LibraryRepository by lazy { LibraryRepository(this, appScope) }
}
