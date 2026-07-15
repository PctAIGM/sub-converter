package com.subconverter.data.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.subconverter.core.AppContainer
import com.subconverter.data.NodeDnsCacheEntity
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.settings.ServerSettings

data class AppBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val settings: ServerSettings,
    val sources: List<SubscriptionSourceEntity>,
    val nodeDnsCaches: List<NodeDnsCacheEntity>,
    val templates: List<TemplateEntity>,
    val outputProfiles: List<OutputProfileEntity>,
) {
    companion object { const val FORMAT = "subconverter-backup"; const val VERSION = 1 }
}

class AppBackupService(private val container: AppContainer) {
    private val gson = Gson()

    suspend fun exportJson(settingsOverride: ServerSettings? = null): String = gson.toJson(
        AppBackup(
            settings = settingsOverride ?: container.settingsStore.current(),
            sources = container.database.subscriptionSourceDao().getAll(),
            nodeDnsCaches = container.database.nodeDnsCacheDao().getAll(),
            templates = container.database.templateDao().getAll(),
            outputProfiles = container.database.outputProfileDao().getAll(),
        ),
    )

    suspend fun importJson(content: String) {
        val backup = gson.fromJson(content, AppBackup::class.java)
            ?: throw IllegalArgumentException("备份文件为空")
        require(backup.format == AppBackup.FORMAT && backup.version == AppBackup.VERSION) {
            "不是兼容的 SubConverter 备份文件"
        }
        val db = container.database
        db.withTransaction {
            db.outputProfileDao().deleteAll()
            db.nodeDnsCacheDao().deleteAll()
            db.templateDao().deleteAll()
            db.subscriptionSourceDao().deleteAll()
            db.subscriptionSourceDao().insertAll(backup.sources)
            db.templateDao().insertAll(backup.templates)
            db.outputProfileDao().insertAll(backup.outputProfiles)
            db.nodeDnsCacheDao().insertAll(backup.nodeDnsCaches)
        }
        container.settingsStore.update(backup.settings)
        backup.sources.forEach(container.refreshScheduler::reschedule)
    }
}
