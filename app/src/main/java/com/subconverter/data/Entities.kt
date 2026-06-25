package com.subconverter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
    val dnsProtocol: String = "",
    val dnsServer: String = "",
    val dnsConnectionMode: String = "PRESERVE_DOMAIN",
    val allowHostnameMismatch: Boolean = false,
    val preResolveNodes: Boolean = false,
    val nodeResolveSuccessCount: Int = 0,
    val nodeResolveFailureCount: Int = 0,
)

@Entity(
    tableName = "node_dns_cache",
    primaryKeys = ["sourceId", "hostname"],
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sourceId")],
)
data class NodeDnsCacheEntity(
    val sourceId: Long,
    val hostname: String,
    val ipAddress: String,
    val expiresAt: Long,
    val configFingerprint: String,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val yamlBody: String,
    val remoteUrl: String = "",
    val isDefault: Boolean = false,
    val enabled: Boolean = true,
    val global: Boolean = false,
    val sortOrder: Int = 0,
    val type: String = TemplateType.YAML,
    val lastRefreshAt: Long? = null,
    val lastError: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

object TemplateType {
    const val YAML = "YAML"
    const val JS = "JS"
}

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
    val overrideIds: String = "",
    val updateIntervalHours: Int = 12,
    val fetchCount: Long = 0,
    val uploadToGist: Boolean = false,
    val gistId: String = "",
)
