package com.subconverter.core

import android.content.Context
import androidx.room.Room
import com.subconverter.data.AppDatabase
import com.subconverter.data.settings.ServerSettingsStore
import com.subconverter.domain.MihomoYamlService
import com.subconverter.domain.NodePreResolver
import com.subconverter.domain.OutputRepository
import com.subconverter.domain.RefreshScheduler
import com.subconverter.domain.RemoteTextFetcher
import com.subconverter.domain.SubscriptionFetcher
import com.subconverter.domain.SubscriptionRepository
import com.subconverter.server.LocalHttpServer

class AppContainer private constructor(context: Context) {
    val appContext: Context = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "sub_converter.db",
    )
        .addMigrations(AppDatabase.Migration1To2)
        .addMigrations(AppDatabase.Migration2To3)
        .addMigrations(AppDatabase.Migration3To4)
        .addMigrations(AppDatabase.Migration4To5)
        .addMigrations(AppDatabase.Migration5To6)
        .addMigrations(AppDatabase.Migration6To7)
        .addMigrations(AppDatabase.Migration7To8)
        .build()

    val settingsStore = ServerSettingsStore(appContext)
    val yamlService = MihomoYamlService()
    val subscriptionFetcher = SubscriptionFetcher()
    val remoteTextFetcher = RemoteTextFetcher()
    private val nodePreResolver = NodePreResolver()
    val refreshScheduler = RefreshScheduler(appContext)

    val subscriptionRepository = SubscriptionRepository(
        dao = database.subscriptionSourceDao(),
        nodeDnsCacheDao = database.nodeDnsCacheDao(),
        fetcher = subscriptionFetcher,
        yamlService = yamlService,
        nodePreResolver = nodePreResolver,
        refreshScheduler = refreshScheduler,
    )

    val outputRepository = OutputRepository(
        sourceDao = database.subscriptionSourceDao(),
        nodeDnsCacheDao = database.nodeDnsCacheDao(),
        templateDao = database.templateDao(),
        outputDao = database.outputProfileDao(),
        yamlService = yamlService,
        remoteTextFetcher = remoteTextFetcher,
    )

    val localHttpServer = LocalHttpServer(outputRepository)

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
