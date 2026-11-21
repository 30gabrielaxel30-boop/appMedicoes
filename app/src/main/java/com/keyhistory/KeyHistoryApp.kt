package com.keyhistory

import android.app.Application
import com.keyhistory.database.KeyHistoryDatabase
import com.keyhistory.repository.HistoryRepository
import com.keyhistory.repository.SettingsRepository
import com.keyhistory.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Contêiner simples de dependências (injeção manual). */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val crypto = CryptoManager(app)
    val database = KeyHistoryDatabase.build(app)
    val historyRepository = HistoryRepository(app, database.historyDao(), crypto)
    val settingsRepository = SettingsRepository(app)
}

class KeyHistoryApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
