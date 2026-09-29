package com.hp.novatv

import android.app.Application
import com.hp.novatv.data.db.NovaDatabase
import com.hp.novatv.data.prefs.SettingsStore
import com.hp.novatv.data.repo.ChannelRepository
import com.hp.novatv.player.ExoPlayerEngine
import com.hp.novatv.player.Recorder
import com.hp.novatv.player.VlcEngine
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Uygulama girisi ve elle DI konteyneri.
 *
 * Hilt bilerek kullanilmadi: tek modullu projede DI katmani icin
 * AGP 9 + KSP2 + Kotlin 2.4 uyum riski almaya deger degil.
 */
class NovaTvApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Tum bagimliliklarin tek sahibi. */
class AppContainer(app: Application) {

    private val app = app.applicationContext as Application

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .build()

    val database: NovaDatabase by lazy { NovaDatabase.get(app) }

    val settings: SettingsStore by lazy { SettingsStore(app) }

    val repository: ChannelRepository by lazy {
        ChannelRepository(database, settings, httpClient)
    }

    val exoEngine: ExoPlayerEngine by lazy { ExoPlayerEngine(app, httpClient) }

    val vlcEngine: VlcEngine by lazy { VlcEngine(app) }

    val recorder: Recorder by lazy { Recorder(app, httpClient) }

    suspend fun ensureVlc(): Boolean = vlcEngine.initialize()
}
