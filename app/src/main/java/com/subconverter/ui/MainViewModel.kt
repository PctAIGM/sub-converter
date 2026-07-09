package com.subconverter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subconverter.core.AppContainer
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.TemplateType
import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.DEFAULT_OVERRIDE_JS
import com.subconverter.domain.DEFAULT_OVERRIDE_YAML
import com.subconverter.domain.DnsConnectionMode
import com.subconverter.domain.SubscriptionDnsConfig
import com.subconverter.domain.nodeResolutionFingerprint
import com.subconverter.i18n.AppI18n
import com.subconverter.server.LocalHttpServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val sources: List<SubscriptionSourceEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val profiles: List<OutputProfileEntity> = emptyList(),
    val settings: ServerSettings = ServerSettings(),
    val serverRunning: Boolean = false,
    val message: String = "",
    val refreshingSourceIds: Set<Long> = emptySet(),
)

sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data class Success(val yaml: String, val profileTitle: String?) : PreviewState
    data class Error(val message: String) : PreviewState
}

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val messages = MutableStateFlow("")
    private val refreshingIds = MutableStateFlow<Set<Long>>(emptySet())
    val previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)

    fun showMessage(message: String) {
        messages.value = AppI18n.message(container.appContext, message)
    }

    fun clearMessage() {
        messages.value = ""
    }

    fun previewProfile(profileId: Long) {
        previewState.value = PreviewState.Loading
        viewModelScope.launch {
            val result = runCatching { container.outputRepository.renderProfile(profileId) }
            previewState.value = result.fold(
                onSuccess = { rendered ->
                    rendered?.let { PreviewState.Success(it.yamlBody, it.profileTitle) }
                        ?: PreviewState.Error("输出不存在")
                },
                onFailure = { PreviewState.Error(AppI18n.message(container.appContext, it.message ?: "渲染失败")) },
            )
        }
    }

    fun clearPreview() {
        previewState.value = PreviewState.Idle
    }

    private val dataState = combine(
        container.subscriptionRepository.sources,
        container.outputRepository.templates,
        container.outputRepository.profiles,
        container.settingsStore.settings,
        container.localHttpServer.running,
    ) { sources: List<SubscriptionSourceEntity>,
        templates: List<TemplateEntity>,
        profiles: List<OutputProfileEntity>,
        settings: ServerSettings,
        running: Boolean ->
        MainUiState(
            sources = sources,
            templates = templates,
            profiles = profiles,
            settings = settings,
            serverRunning = running,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        dataState,
        messages,
        refreshingIds,
    ) { state, message, ids ->
        state.copy(message = message, refreshingSourceIds = ids)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            val settings = container.settingsStore.current()
            if (settings.enabled && !container.localHttpServer.running.value) {
                runCatching { LocalHttpServerService.start(container.appContext) }
                    .onFailure { messages.value = AppI18n.message(container.appContext, it.message ?: "HTTP 服务启动失败") }
            }
        }
    }

    fun saveSource(
        existing: SubscriptionSourceEntity?,
        name: String,
        url: String,
        userAgent: String,
        prefix: String,
        includeRegex: String,
        excludeRegex: String,
        autoRefreshEnabled: Boolean,
        refreshIntervalMinutes: Long,
        preResolveNodes: Boolean,
        dnsConfig: SubscriptionDnsConfig,
    ) {
        viewModelScope.launch {
            if (url.isBlank()) {
                messages.value = AppI18n.text(container.appContext, "订阅地址不能为空")
                return@launch
            }
            dnsConfig.validate()?.let {
                messages.value = AppI18n.message(container.appContext, it)
                return@launch
            }
            val resolvedName = name.trim().ifBlank {
                existing?.name?.takeIf { it.isNotBlank() } ?: ""
            }
            val source = (existing ?: SubscriptionSourceEntity(name = "", url = "")).copy(
                name = resolvedName,
                url = url.trim(),
                userAgent = userAgent.trim(),
                prefix = prefix.trim(),
                includeRegex = includeRegex.trim(),
                excludeRegex = excludeRegex.trim(),
                autoRefreshEnabled = autoRefreshEnabled,
                refreshIntervalMinutes = refreshIntervalMinutes.coerceAtLeast(15),
                dnsProtocol = dnsConfig.protocol?.name.orEmpty(),
                dnsServer = if (dnsConfig.usesSystemDns) "" else dnsConfig.server.trim(),
                dnsConnectionMode = if (dnsConfig.usesSystemDns) {
                    DnsConnectionMode.PRESERVE_DOMAIN.name
                } else {
                    dnsConfig.connectionMode.name
                },
                allowHostnameMismatch = !dnsConfig.usesSystemDns &&
                    dnsConfig.connectionMode == DnsConnectionMode.IP_URL &&
                    dnsConfig.allowHostnameMismatch,
                preResolveNodes = preResolveNodes,
            )

            val shouldRefresh = preResolveNodes && (
                existing == null ||
                    !existing.preResolveNodes ||
                    existing.url.trim() != source.url ||
                    SubscriptionDnsConfig.from(existing).nodeResolutionFingerprint() !=
                    dnsConfig.nodeResolutionFingerprint()
                )
            val sourceId = if (existing == null) {
                container.subscriptionRepository.add(source)
            } else {
                container.subscriptionRepository.update(source)
                source.id
            }

            if (shouldRefresh) {
                refreshingIds.update { it + sourceId }
                val globalUserAgent = container.settingsStore.current().globalUserAgent
                val outcome = container.subscriptionRepository.refreshSource(sourceId, globalUserAgent)
                messages.value = AppI18n.message(container.appContext, outcome.message)
                refreshingIds.update { it - sourceId }
            } else {
                messages.value = if (existing == null) {
                    AppI18n.message(container.appContext, "已添加订阅 #$sourceId")
                } else {
                    AppI18n.text(container.appContext, "订阅已保存")
                }
            }
        }
    }

    fun deleteSource(source: SubscriptionSourceEntity) {
        viewModelScope.launch {
            container.subscriptionRepository.delete(source)
            messages.value = AppI18n.text(container.appContext, "订阅已删除")
        }
    }

    fun addSource(
        name: String,
        url: String,
        userAgent: String,
        prefix: String,
        includeRegex: String,
        excludeRegex: String,
        autoRefreshEnabled: Boolean,
        refreshIntervalMinutes: Long,
        preResolveNodes: Boolean = false,
        dnsConfig: SubscriptionDnsConfig = SubscriptionDnsConfig(),
    ) {
        saveSource(
            existing = null,
            name = name,
            url = url,
            userAgent = userAgent,
            prefix = prefix,
            includeRegex = includeRegex,
            excludeRegex = excludeRegex,
            autoRefreshEnabled = autoRefreshEnabled,
            refreshIntervalMinutes = refreshIntervalMinutes,
            preResolveNodes = preResolveNodes,
            dnsConfig = dnsConfig,
        )
    }

    fun saveTemplate(
        existing: TemplateEntity?,
        name: String,
        remoteUrl: String,
        yamlBody: String,
        enabled: Boolean,
        global: Boolean,
        type: String,
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                messages.value = AppI18n.text(container.appContext, "覆写名称不能为空")
                return@launch
            }
            container.yamlService.validateOverride(type, yamlBody)?.let { error ->
                messages.value = AppI18n.message(container.appContext, error)
                return@launch
            }
            val template = (existing ?: TemplateEntity(name = "", yamlBody = "")).copy(
                name = name.trim(),
                remoteUrl = remoteUrl.trim(),
                yamlBody = yamlBody,
                enabled = enabled,
                global = global,
                type = type,
                updatedAt = System.currentTimeMillis(),
            )

            val templateId = if (existing == null) {
                container.outputRepository.addTemplate(template)
            } else {
                container.outputRepository.updateTemplate(template)
                template.id
            }

            if (remoteUrl.isNotBlank()) {
                val outcome = container.outputRepository.refreshTemplate(templateId)
                messages.value = AppI18n.message(container.appContext, outcome.message)
            } else {
                messages.value = AppI18n.text(container.appContext, "覆写已保存")
            }
        }
    }

    fun deleteTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            container.outputRepository.deleteTemplate(template)
            messages.value = AppI18n.text(container.appContext, "覆写已删除")
        }
    }

    fun refreshTemplate(templateId: Long) {
        viewModelScope.launch {
            val outcome = container.outputRepository.refreshTemplate(templateId)
            messages.value = AppI18n.message(container.appContext, outcome.message)
        }
    }

    fun addTemplate(name: String, yamlBody: String, type: String = TemplateType.YAML) {
        val defaultBody = when (type) {
            TemplateType.JS -> DEFAULT_OVERRIDE_JS.trimIndent()
            else -> yamlBody.ifBlank { DEFAULT_OVERRIDE_YAML.trimIndent() }
        }
        saveTemplate(
            existing = null,
            name = name,
            remoteUrl = "",
            yamlBody = defaultBody,
            enabled = true,
            global = false,
            type = type,
        )
    }

    fun moveTemplate(templateId: Long, offset: Int) {
        viewModelScope.launch {
            container.outputRepository.moveTemplate(templateId, offset)
        }
    }

    fun saveProfile(
        existing: OutputProfileEntity?,
        name: String,
        sourceIds: List<Long>,
        overrideIds: List<Long>,
        updateIntervalHours: Int,
        uploadToGist: Boolean,
    ) {
        viewModelScope.launch {
            if (name.isBlank() || sourceIds.isEmpty()) {
                messages.value = AppI18n.text(container.appContext, "输出名称和订阅源不能为空")
                return@launch
            }
            val profile = (existing ?: OutputProfileEntity(name = "", sourceIds = "", templateId = 0)).copy(
                name = name.trim(),
                sourceIds = sourceIds.distinct().joinToString(","),
                prefix = "",
                includeRegex = "",
                excludeRegex = "",
                overrideIds = overrideIds.distinct().joinToString(","),
                updateIntervalHours = updateIntervalHours.coerceAtLeast(1),
                uploadToGist = uploadToGist,
                gistId = existing?.gistId ?: "",
            )

            if (existing == null) {
                container.outputRepository.addProfile(profile)
            } else {
                container.outputRepository.updateProfile(profile)
            }
            messages.value = AppI18n.text(container.appContext, "输出配置已保存")
        }
    }

    fun deleteProfile(profile: OutputProfileEntity) {
        viewModelScope.launch {
            container.outputRepository.deleteProfile(profile)
            messages.value = AppI18n.text(container.appContext, "输出配置已删除")
        }
    }

    fun addProfile(
        name: String,
        sourceIds: String,
        overrideIds: String,
        updateIntervalHours: Int,
        uploadToGist: Boolean = false,
    ) {
        saveProfile(
            existing = null,
            name = name,
            sourceIds = sourceIds.split(',').mapNotNull { it.trim().toLongOrNull() },
            overrideIds = overrideIds.split(',').mapNotNull { it.trim().toLongOrNull() },
            updateIntervalHours = updateIntervalHours,
            uploadToGist = uploadToGist,
        )
    }

    fun refreshSource(sourceId: Long) {
        viewModelScope.launch {
            refreshingIds.update { it + sourceId }
            val globalUserAgent = container.settingsStore.current().globalUserAgent
            val outcome = container.subscriptionRepository.refreshSource(sourceId, globalUserAgent)
            messages.value = AppI18n.message(container.appContext, outcome.message)
            refreshingIds.update { it - sourceId }
        }
    }

    fun updateServerSettings(settings: ServerSettings) {
        viewModelScope.launch {
            container.settingsStore.update(settings)
            if (settings.enabled) {
                runCatching {
                    container.localHttpServer.start(settings)
                    LocalHttpServerService.start(container.appContext)
                    messages.value = AppI18n.text(container.appContext, "HTTP 服务已启动")
                }.onFailure {
                    container.settingsStore.update(settings.copy(enabled = false))
                    container.localHttpServer.stop()
                    messages.value = AppI18n.message(container.appContext, it.message ?: "HTTP 服务启动失败")
                }
            } else {
                LocalHttpServerService.stop(container.appContext)
                container.localHttpServer.stop()
                messages.value = AppI18n.text(container.appContext, "HTTP 服务已停止")
            }
        }
    }

    fun updateAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsStore.updateAutoStartOnBoot(enabled)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container) as T
                }
            }
    }
}
