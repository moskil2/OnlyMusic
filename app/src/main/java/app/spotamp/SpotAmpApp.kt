package app.spotamp

import android.app.Application
import app.spotamp.data.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SpotAmpApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val library: LibraryRepository by lazy { LibraryRepository(this, appScope) }
}
