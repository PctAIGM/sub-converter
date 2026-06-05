package com.subconverter.domain

import com.subconverter.data.SubscriptionSourceDao
import com.subconverter.data.SubscriptionSourceEntity
import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(
    private val dao: SubscriptionSourceDao,
    private val fetcher: SubscriptionFetcher,
    private val refreshScheduler: RefreshScheduler,
) {
    val sources: Flow<List<SubscriptionSourceEntity>> = dao.observeAll()

    suspend fun add(source: SubscriptionSourceEntity): Long {
        val id = dao.insert(source)
        refreshScheduler.reschedule(source.copy(id = id))
        return id
    }

    suspend fun update(source: SubscriptionSourceEntity) {
        dao.update(source)
        refreshScheduler.reschedule(source)
    }

    suspend fun delete(source: SubscriptionSourceEntity) {
        dao.delete(source)
        refreshScheduler.cancel(source.id)
    }

    suspend fun refreshSource(
        sourceId: Long,
        globalUserAgent: String = SubscriptionFetcher.DEFAULT_USER_AGENT,
    ): RefreshOutcome {
        val source = dao.getById(sourceId)
            ?: return RefreshOutcome(sourceId, success = false, message = "订阅不存在")

        dao.update(source.copy(lastError = ""))

        return runCatching {
            val sourceUserAgent = source.userAgent.trim()
            val globalValue = globalUserAgent.trim().ifBlank { SubscriptionFetcher.DEFAULT_USER_AGENT }
            val effectiveUserAgent = when {
                sourceUserAgent.isBlank() -> globalValue
                // Existing rows defaulted to legacy UA should follow global UA once user customizes it.
                sourceUserAgent == SubscriptionFetcher.MIHOMO_USER_AGENT &&
                    globalValue != SubscriptionFetcher.MIHOMO_USER_AGENT -> globalValue
                else -> sourceUserAgent
            }
            val result = fetcher.fetch(source.url, effectiveUserAgent)
            val resolvedName = source.name.takeIf { it.isNotBlank() }
                ?: result.profileTitle
                ?: "Sub-${System.currentTimeMillis().toString(36)}"
            val resolvedWebsite = source.website.takeIf { it.isNotBlank() }
                ?: result.profileWebPageUrl
                ?: source.website
            val resolvedAutoRefresh = source.autoRefreshEnabled || result.profileUpdateIntervalHours != null
            val resolvedInterval = result.profileUpdateIntervalHours?.toLong()?.times(60)
                ?: source.refreshIntervalMinutes
            dao.update(
                source.copy(
                    name = resolvedName,
                    website = resolvedWebsite,
                    cachedYaml = result.yamlBody,
                    lastRefreshAt = System.currentTimeMillis(),
                    lastStatusCode = result.statusCode,
                    lastError = "",
                    uploadBytes = result.userInfo?.uploadBytes,
                    downloadBytes = result.userInfo?.downloadBytes,
                    totalBytes = result.userInfo?.totalBytes,
                    expireAtSeconds = result.userInfo?.expireAtSeconds,
                    autoRefreshEnabled = resolvedAutoRefresh,
                    refreshIntervalMinutes = resolvedInterval.coerceAtLeast(15),
                ),
            )
            RefreshOutcome(sourceId, success = true, message = "刷新成功")
        }.getOrElse { throwable ->
            dao.update(
                source.copy(
                    lastError = throwable.message ?: throwable::class.java.simpleName,
                ),
            )
            RefreshOutcome(sourceId, success = false, message = throwable.message ?: "刷新失败")
        }
    }
}
