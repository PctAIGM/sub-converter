package com.subconverter.domain

import com.subconverter.data.NodeDnsCacheDao
import com.subconverter.data.SubscriptionSourceDao
import com.subconverter.data.SubscriptionSourceEntity
import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(
    private val dao: SubscriptionSourceDao,
    private val nodeDnsCacheDao: NodeDnsCacheDao,
    private val fetcher: SubscriptionFetcher,
    private val yamlService: MihomoYamlService,
    private val nodePreResolver: NodePreResolver,
    private val refreshScheduler: RefreshScheduler,
    private val outputRepository: OutputRepository,
) {
    val sources: Flow<List<SubscriptionSourceEntity>> = dao.observeAll()

    suspend fun add(source: SubscriptionSourceEntity): Long {
        val id = dao.insert(source)
        refreshScheduler.reschedule(source.copy(id = id))
        return id
    }

    suspend fun update(source: SubscriptionSourceEntity) {
        val previous = dao.getById(source.id)
        val dnsChanged = previous != null &&
            SubscriptionDnsConfig.from(previous).nodeResolutionFingerprint() !=
            SubscriptionDnsConfig.from(source).nodeResolutionFingerprint()
        val updatedSource = if (source.preResolveNodes && dnsChanged) {
            nodeDnsCacheDao.deleteBySourceId(source.id)
            source.copy(nodeResolveSuccessCount = 0, nodeResolveFailureCount = 0)
        } else {
            source
        }
        dao.update(updatedSource)
        refreshScheduler.reschedule(updatedSource)
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
            val result = fetcher.fetch(
                url = source.url,
                userAgent = effectiveUserAgent,
                dnsConfig = SubscriptionDnsConfig.from(source),
            )
            val resolvedName = source.name.takeIf { it.isNotBlank() }
                ?: result.profileTitle
                ?: "Sub-${System.currentTimeMillis().toString(36)}"
            val resolvedWebsite = source.website.takeIf { it.isNotBlank() }
                ?: result.profileWebPageUrl
                ?: source.website
            val resolvedAutoRefresh = source.autoRefreshEnabled || result.profileUpdateIntervalHours != null
            val resolvedInterval = result.profileUpdateIntervalHours?.toLong()?.times(60)
                ?: source.refreshIntervalMinutes
            val nodeResolution = if (source.preResolveNodes) {
                nodePreResolver.refresh(
                    sourceId = source.id,
                    hostnames = yamlService.extractProxyServerHostnames(result.yamlBody),
                    existingEntries = nodeDnsCacheDao.getBySourceId(source.id),
                    config = SubscriptionDnsConfig.from(source),
                ).also {
                    nodeDnsCacheDao.replaceForSource(source.id, it.entries)
                }
            } else {
                null
            }
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
                    nodeResolveSuccessCount = nodeResolution?.successCount
                        ?: source.nodeResolveSuccessCount,
                    nodeResolveFailureCount = nodeResolution?.failureCount
                        ?: source.nodeResolveFailureCount,
                ),
            )
            val message = nodeResolution?.let {
                if (it.failureCount == 0) {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount}"
                } else {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount + it.failureCount}（${it.failureCount} 失败）"
                }
            } ?: "刷新成功"
            val summary = runCatching { outputRepository.uploadAffectedProfiles(sourceId) }
                .getOrElse { GistUploadSummary(tokenMissing = true) }
            RefreshOutcome(sourceId, success = true, message = message + gistSuffix(summary))
        }.getOrElse { throwable ->
            dao.update(
                source.copy(
                    lastError = throwable.message ?: throwable::class.java.simpleName,
                ),
            )
            RefreshOutcome(sourceId, success = false, message = throwable.message ?: "刷新失败")
        }
    }

    private fun gistSuffix(summary: GistUploadSummary): String = when {
        summary.tokenMissing && summary.pendingCount > 0 -> " · ${summary.pendingCount} 个输出待上传 Gist，但未配置 Token"
        summary.tokenMissing -> ""
        summary.attempted == 0 -> ""
        summary.succeeded == summary.attempted -> " · Gist 已更新"
        summary.succeeded == 0 && summary.firstError != null -> " · Gist 上传失败: ${summary.firstError}"
        else -> " · Gist 部分成功 ${summary.succeeded}/${summary.attempted}"
    }
}
