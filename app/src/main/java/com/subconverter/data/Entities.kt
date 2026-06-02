package com.subconverter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_sources")
data class SubscriptionSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val website: String = "",
    val userAgent: String = "ClashforWindows/0.20.39",
    val enabled: Boolean = true,
    val autoRefreshEnabled: Boolean = false,
    val refreshIntervalMinutes: Long = 720,
    val prefix: String = "",
    val includeRegex: String = "",
    val excludeRegex: String = "",
    val cachedYaml: String = "",
    val lastRefreshAt: Long? = null,
    val lastStatusCode: Int? = null,
    val lastError: String = "",
    val uploadBytes: Long? = null,
    val downloadBytes: Long? = null,
    val totalBytes: Long? = null,
    val expireAtSeconds: Long? = null,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val yamlBody: String,
    val remoteUrl: String = "",
    val isDefault: Boolean = false,
    val lastRefreshAt: Long? = null,
    val lastError: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "output_profiles")
data class OutputProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceIds: String,
    val templateId: Long,
    val enabled: Boolean = true,
    val prefix: String = "",
    val includeRegex: String = "",
    val excludeRegex: String = "",
    val updateIntervalHours: Int = 12,
    val fetchCount: Long = 0,
)
