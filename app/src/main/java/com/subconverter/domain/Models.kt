package com.subconverter.domain

import com.subconverter.data.SubscriptionSourceEntity

data class SubscriptionUserInfo(
    val uploadBytes: Long? = null,
    val downloadBytes: Long? = null,
    val totalBytes: Long? = null,
    val expireAtSeconds: Long? = null,
) {
    val usedBytes: Long?
        get() = listOfNotNull(uploadBytes, downloadBytes).takeIf { it.isNotEmpty() }?.sum()

    val remainingBytes: Long?
        get() {
            val total = totalBytes ?: return null
            val used = usedBytes ?: return null
            return (total - used).coerceAtLeast(0)
        }

    fun toHeaderValue(): String {
        val parts = mutableListOf<String>()
        uploadBytes?.let { parts += "upload=$it" }
        downloadBytes?.let { parts += "download=$it" }
        totalBytes?.let { parts += "total=$it" }
        expireAtSeconds?.let { parts += "expire=$it" }
        return parts.joinToString("; ")
    }
}

data class TransformRules(
    val prefix: String = "",
    val includeRegex: String = "",
    val excludeRegex: String = "",
)

data class FetchResult(
    val yamlBody: String,
    val statusCode: Int,
    val userInfo: SubscriptionUserInfo?,
    val profileTitle: String? = null,
    val profileWebPageUrl: String? = null,
    val profileUpdateIntervalHours: Int? = null,
)

data class RefreshOutcome(
    val sourceId: Long,
    val success: Boolean,
    val message: String,
)

data class RenderedSubscription(
    val yamlBody: String,
    val userInfo: SubscriptionUserInfo?,
    val updateIntervalHours: Int,
    val profileTitle: String? = null,
    val profileWebPageUrl: String? = null,
)

fun SubscriptionSourceEntity.userInfo(): SubscriptionUserInfo? {
    if (uploadBytes == null && downloadBytes == null && totalBytes == null && expireAtSeconds == null) {
        return null
    }
    return SubscriptionUserInfo(
        uploadBytes = uploadBytes,
        downloadBytes = downloadBytes,
        totalBytes = totalBytes,
        expireAtSeconds = expireAtSeconds,
    )
}

data class GistResult(
    val success: Boolean,
    val gistId: String? = null,
    val rawUrl: String? = null,
    val message: String,
)

data class GistUploadSummary(
    val tokenMissing: Boolean,
    val attempted: Int = 0,
    val succeeded: Int = 0,
    val firstError: String? = null,
)
