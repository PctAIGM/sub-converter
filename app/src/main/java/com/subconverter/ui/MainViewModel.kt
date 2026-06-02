package com.subconverter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subconverter.core.AppContainer
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.DEFAULT_MIHOMO_TEMPLATE
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

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val messages = MutableStateFlow("")
    private val refreshingIds = MutableStateFlow<Set<Long>>(emptySet())

    fun showMessage(message: String) {
        messages.value = message
    }

    fun clearMessage() {
        messages.value = ""
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
            container.outputRepository.ensureDefaultTemplate()
        }
        viewModelScope.launch {
            container.settingsStore.settings.collect { settings ->
                if (settings.enabled && !container.localHttpServer.running.value) {
                    runCatching { container.localHttpServer.start(settings) }
                        .onFailure { messages.value = it.message ?: "HTTP 服务启动失败" }
                }
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
    ) {
        viewModelScope.launch {
            if (url.isBlank()) {
                messages.value = "订阅地址不能为空"
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
            )

            if (existing == null) {
                val id = container.subscriptionRepository.add(source)
                messages.value = "已添加订阅 #$id"
            } else {
                container.subscriptionRepository.update(source)
                messages.value = "订阅已保存"
            }
        }
    }

    fun deleteSource(source: SubscriptionSourceEntity) {
        viewModelScope.launch {
            container.subscriptionRepository.delete(source)
            messages.value = "订阅已删除"
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
        )
    }

    fun saveTemplate(existing: TemplateEntity?, name: String, remoteUrl: String, yamlBody: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                messages.value = "模板名称不能为空"
                return@launch
            }
            val template = (existing ?: TemplateEntity(name = "", yamlBody = "")).copy(
                name = name.trim(),
                remoteUrl = remoteUrl.trim(),
                yamlBody = yamlBody.ifBlank { DEFAULT_MIHOMO_TEMPLATE.trimIndent() },
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
                messages.value = outcome.message
            } else {
                messages.value = "模板已保存"
            }
        }
    }

    fun deleteTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            container.outputRepository.deleteTemplate(template)
            messages.value = "模板已删除"
        }
    }

    fun refreshTemplate(templateId: Long) {
        viewModelScope.launch {
            val outcome = container.outputRepository.refreshTemplate(templateId)
            messages.value = outcome.message
        }
    }

    fun addTemplate(name: String, yamlBody: String) {
        saveTemplate(existing = null, name = name, remoteUrl = "", yamlBody = yamlBody)
    }

    fun saveProfile(
        existing: OutputProfileEntity?,
        name: String,
        sourceIds: List<Long>,
        templateId: Long,
        prefix: String,
        includeRegex: String,
        excludeRegex: String,
        updateIntervalHours: Int,
    ) {
        viewModelScope.launch {
            if (name.isBlank() || templateId <= 0 || sourceIds.isEmpty()) {
                messages.value = "输出名称、订阅源和模板不能为空"
                return@launch
            }
            val profile = (existing ?: OutputProfileEntity(name = "", sourceIds = "", templateId = templateId)).copy(
                name = name.trim(),
                sourceIds = sourceIds.distinct().joinToString(","),
                templateId = templateId,
                prefix = prefix.trim(),
                includeRegex = includeRegex.trim(),
                excludeRegex = excludeRegex.trim(),
                updateIntervalHours = updateIntervalHours.coerceAtLeast(1),
            )

            if (existing == null) {
                container.outputRepository.addProfile(profile)
            } else {
                container.outputRepository.updateProfile(profile)
            }
            messages.value = "输出配置已保存"
        }
    }

    fun deleteProfile(profile: OutputProfileEntity) {
        viewModelScope.launch {
            container.outputRepository.deleteProfile(profile)
            messages.value = "输出配置已删除"
        }
    }

    fun addProfile(
        name: String,
        sourceIds: String,
        templateId: Long,
        prefix: String,
        includeRegex: String,
        excludeRegex: String,
        updateIntervalHours: Int,
    ) {
        saveProfile(
            existing = null,
            name = name,
            sourceIds = sourceIds.split(',').mapNotNull { it.trim().toLongOrNull() },
            templateId = templateId,
            prefix = prefix,
            includeRegex = includeRegex,
            excludeRegex = excludeRegex,
            updateIntervalHours = updateIntervalHours,
        )
    }

    fun refreshSource(sourceId: Long) {
        viewModelScope.launch {
            refreshingIds.update { it + sourceId }
            val globalUserAgent = container.settingsStore.current().globalUserAgent
            val outcome = container.subscriptionRepository.refreshSource(sourceId, globalUserAgent)
            messages.value = outcome.message
            refreshingIds.update { it - sourceId }
        }
    }

    fun updateServerSettings(settings: ServerSettings) {
        viewModelScope.launch {
            container.settingsStore.update(settings)
            if (settings.enabled) {
                runCatching {
                    container.localHttpServer.start(settings)
                    messages.value = "HTTP 服务已启动"
                }.onFailure {
                    container.settingsStore.update(settings.copy(enabled = false))
                    messages.value = it.message ?: "HTTP 服务启动失败"
                }
            } else {
                container.localHttpServer.stop()
                messages.value = "HTTP 服务已停止"
            }
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
