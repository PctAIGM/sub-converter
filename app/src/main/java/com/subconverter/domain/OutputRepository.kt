package com.subconverter.domain

import com.subconverter.data.OutputProfileDao
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceDao
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateDao
import com.subconverter.data.TemplateEntity
import kotlinx.coroutines.flow.Flow

class OutputRepository(
    private val sourceDao: SubscriptionSourceDao,
    private val templateDao: TemplateDao,
    private val outputDao: OutputProfileDao,
    private val yamlService: MihomoYamlService,
    private val remoteTextFetcher: RemoteTextFetcher,
) {
    val templates: Flow<List<TemplateEntity>> = templateDao.observeAll()
    val profiles: Flow<List<OutputProfileEntity>> = outputDao.observeAll()

    suspend fun ensureDefaultTemplate() {
        if (templateDao.count() == 0) {
            templateDao.insert(
                TemplateEntity(
                    name = DEFAULT_TEMPLATE_NAME,
                    yamlBody = DEFAULT_MIHOMO_TEMPLATE.trimIndent(),
                    isDefault = true,
                ),
            )
        }
    }

    suspend fun addTemplate(template: TemplateEntity): Long = templateDao.insert(template)

    suspend fun updateTemplate(template: TemplateEntity) {
        templateDao.update(template.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTemplate(template: TemplateEntity) {
        templateDao.delete(template)
    }

    suspend fun refreshTemplate(templateId: Long): RefreshOutcome {
        val template = templateDao.getById(templateId)
            ?: return RefreshOutcome(templateId, success = false, message = "模板不存在")
        if (template.remoteUrl.isBlank()) {
            return RefreshOutcome(templateId, success = false, message = "模板没有远程地址")
        }

        templateDao.update(template.copy(lastError = ""))

        return runCatching {
            val body = remoteTextFetcher.fetch(template.remoteUrl)
            templateDao.update(
                template.copy(
                    yamlBody = body,
                    lastRefreshAt = System.currentTimeMillis(),
                    lastError = "",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            RefreshOutcome(templateId, success = true, message = "模板刷新成功")
        }.getOrElse { throwable ->
            templateDao.update(
                template.copy(
                    lastRefreshAt = System.currentTimeMillis(),
                    lastError = throwable.message ?: throwable::class.java.simpleName,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            RefreshOutcome(templateId, success = false, message = throwable.message ?: "模板刷新失败")
        }
    }

    suspend fun incrementFetchCount(profileId: Long) {
        outputDao.incrementFetchCount(profileId)
    }

    suspend fun addProfile(profile: OutputProfileEntity): Long = outputDao.insert(profile)

    suspend fun updateProfile(profile: OutputProfileEntity) {
        outputDao.update(profile)
    }

    suspend fun deleteProfile(profile: OutputProfileEntity) {
        outputDao.delete(profile)
    }

    suspend fun renderProfile(profileId: Long): RenderedSubscription? {
        val profile = outputDao.getById(profileId)?.takeIf { it.enabled } ?: return null
        val template = templateDao.getById(profile.templateId) ?: templateDao.getAll().firstOrNull()
        val sourceIds = profile.sourceIds
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()
        val sources = sourceDao.getByIds(sourceIds)
            .filter { it.enabled && it.cachedYaml.isNotBlank() }
            .sortedBy { source -> sourceIds.indexOf(source.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }

        val sourceProxies = sources.flatMap { source ->
            val proxies = yamlService.extractProxies(source.cachedYaml)
            yamlService.transformProxies(
                proxies = proxies,
                rules = TransformRules(
                    prefix = source.prefix,
                    includeRegex = source.includeRegex,
                    excludeRegex = source.excludeRegex,
                ),
            )
        }

        val outputProxies = yamlService.transformProxies(
            proxies = sourceProxies,
            rules = TransformRules(
                prefix = profile.prefix,
                includeRegex = profile.includeRegex,
                excludeRegex = profile.excludeRegex,
            ),
        )

        val body = yamlService.renderTemplate(
            templateYaml = template?.yamlBody ?: DEFAULT_MIHOMO_TEMPLATE,
            proxies = outputProxies,
        )

        val profileTitle = profile.name.takeIf { it.isNotBlank() }
        val websites = sources.map { it.website.trim() }.filter { it.isNotBlank() }.distinct()
        val profileWebPageUrl = websites.firstOrNull()

        return RenderedSubscription(
            yamlBody = body,
            userInfo = aggregateUserInfo(sources),
            updateIntervalHours = profile.updateIntervalHours,
            profileTitle = profileTitle,
            profileWebPageUrl = profileWebPageUrl,
        )
    }

    private fun aggregateUserInfo(sources: List<SubscriptionSourceEntity>): SubscriptionUserInfo? {
        val infos = sources.mapNotNull { it.userInfo() }
        if (infos.isEmpty()) return null

        fun sum(selector: (SubscriptionUserInfo) -> Long?): Long? =
            infos.mapNotNull(selector).takeIf { it.isNotEmpty() }?.sum()

        return SubscriptionUserInfo(
            uploadBytes = sum { it.uploadBytes },
            downloadBytes = sum { it.downloadBytes },
            totalBytes = sum { it.totalBytes },
            expireAtSeconds = infos.mapNotNull { it.expireAtSeconds }.minOrNull(),
        )
    }
}
