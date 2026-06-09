package com.subconverter.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.subconverter.domain.SubscriptionFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.serverSettingsDataStore by preferencesDataStore("server_settings")

data class ServerSettings(
    val enabled: Boolean = false,
    val autoStartOnBoot: Boolean = false,
    val allowLan: Boolean = false,
    val port: Int = 9876,
    val token: String = "",
    val globalUserAgent: String = SubscriptionFetcher.DEFAULT_USER_AGENT,
)

class ServerSettingsStore(private val context: Context) {
    val settings: Flow<ServerSettings> = context.serverSettingsDataStore.data.map { preferences ->
        ServerSettings(
            enabled = preferences[Keys.Enabled] ?: false,
            autoStartOnBoot = preferences[Keys.AutoStartOnBoot] ?: false,
            allowLan = preferences[Keys.AllowLan] ?: false,
            port = preferences[Keys.Port] ?: 9876,
            token = preferences[Keys.Token].orEmpty(),
            globalUserAgent = preferences[Keys.GlobalUserAgent]
                .orEmpty()
                .ifBlank { SubscriptionFetcher.DEFAULT_USER_AGENT },
        )
    }

    suspend fun update(settings: ServerSettings) {
        context.serverSettingsDataStore.edit { preferences ->
            preferences[Keys.Enabled] = settings.enabled
            preferences[Keys.AutoStartOnBoot] = settings.autoStartOnBoot
            preferences[Keys.AllowLan] = settings.allowLan
            preferences[Keys.Port] = settings.port.coerceIn(1024, 65535)
            preferences[Keys.Token] = settings.token.trim()
            preferences[Keys.GlobalUserAgent] = settings.globalUserAgent
                .trim()
                .ifBlank { SubscriptionFetcher.DEFAULT_USER_AGENT }
        }
    }

    suspend fun updateAutoStartOnBoot(enabled: Boolean) {
        context.serverSettingsDataStore.edit { preferences ->
            preferences[Keys.AutoStartOnBoot] = enabled
        }
    }

    suspend fun current(): ServerSettings = settings.first()

    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val AutoStartOnBoot = booleanPreferencesKey("auto_start_on_boot")
        val AllowLan = booleanPreferencesKey("allow_lan")
        val Port = intPreferencesKey("port")
        val Token = stringPreferencesKey("token")
        val GlobalUserAgent = stringPreferencesKey("global_user_agent")
    }
}
