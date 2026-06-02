package com.subconverter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.DEFAULT_MIHOMO_TEMPLATE
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Sources("订阅", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    Outputs("输出", Icons.Filled.Dns, Icons.Outlined.Dns),
    Templates("模板", Icons.AutoMirrored.Filled.Article, Icons.AutoMirrored.Outlined.Article),
    Server("服务", Icons.Filled.Settings, Icons.Outlined.Settings),
}

private enum class EditScreen { None, Source, Template, Output, Nodes }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Sources) }
    var editScreen by rememberSaveable { mutableStateOf(EditScreen.None) }
    var editingSource by remember { mutableStateOf<SubscriptionSourceEntity?>(null) }
    var editingTemplate by remember { mutableStateOf<TemplateEntity?>(null) }
    var editingProfile by remember { mutableStateOf<OutputProfileEntity?>(null) }
    var viewingSource by remember { mutableStateOf<SubscriptionSourceEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedTab.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    when (selectedTab) {
                        MainTab.Sources -> {
                            IconButton(onClick = {
                                state.sources.forEach { viewModel.refreshSource(it.id) }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "全部刷新")
                            }
                            IconButton(onClick = {
                                editingSource = null
                                editScreen = EditScreen.Source
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加订阅")
                            }
                        }
                        MainTab.Outputs -> {
                            IconButton(onClick = {
                                editingProfile = null
                                editScreen = EditScreen.Output
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加输出")
                            }
                        }
                        MainTab.Templates -> {
                            IconButton(onClick = {
                                editingTemplate = null
                                editScreen = EditScreen.Template
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加模板")
                            }
                        }
                        MainTab.Server -> Unit
                    }
                },
            )
        },
        bottomBar = {
            iOSStyleNavigationBar(selectedTab = selectedTab, onTabSelected = {
                selectedTab = it
                viewModel.clearMessage()
            })
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Sources -> SourcesScreen(
                sources = state.sources,
                refreshingSourceIds = state.refreshingSourceIds,
                onRefresh = viewModel::refreshSource,
                onEdit = {
                    editingSource = it
                    editScreen = EditScreen.Source
                },
                onDelete = viewModel::deleteSource,
                onViewNodes = {
                    viewingSource = it
                    editScreen = EditScreen.Nodes
                },
                modifier = Modifier.padding(padding),
            )

            MainTab.Outputs -> OutputsScreen(
                profiles = state.profiles,
                sources = state.sources,
                templates = state.templates,
                settings = state.settings,
                onEdit = {
                    editingProfile = it
                    editScreen = EditScreen.Output
                },
                onDelete = viewModel::deleteProfile,
                onCopied = { viewModel.showMessage("订阅 URL 已复制") },
                modifier = Modifier.padding(padding),
            )

            MainTab.Templates -> TemplatesScreen(
                templates = state.templates,
                onRefresh = viewModel::refreshTemplate,
                onEdit = {
                    editingTemplate = it
                    editScreen = EditScreen.Template
                },
                onDelete = viewModel::deleteTemplate,
                modifier = Modifier.padding(padding),
            )

            MainTab.Server -> ServerScreen(
                settings = state.settings,
                running = state.serverRunning,
                onSave = viewModel::updateServerSettings,
                modifier = Modifier.padding(padding),
            )
        }
    }

    when (editScreen) {
        EditScreen.Source -> SourceEditScreen(
            source = editingSource,
            allSources = state.sources,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { source, name, url, userAgent, prefix, include, exclude, auto, interval ->
                viewModel.saveSource(source, name, url, userAgent, prefix, include, exclude, auto, interval)
                editScreen = EditScreen.None
            },
        )

        EditScreen.Template -> TemplateEditScreen(
            template = editingTemplate,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { template, name, remoteUrl, body ->
                viewModel.saveTemplate(template, name, remoteUrl, body)
                editScreen = EditScreen.None
            },
        )

        EditScreen.Output -> OutputEditScreen(
            profile = editingProfile,
            sources = state.sources,
            templates = state.templates,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { profile, name, sourceIds, templateId, prefix, include, exclude, interval ->
                viewModel.saveProfile(profile, name, sourceIds, templateId, prefix, include, exclude, interval)
                editScreen = EditScreen.None
            },
        )

        EditScreen.Nodes -> viewingSource?.let { src ->
            NodePreviewScreen(
                source = src,
                onDismiss = { editScreen = EditScreen.None },
            )
        }

        EditScreen.None -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreenScaffold(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = saveEnabled,
                    ) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceEditScreen(
    source: SubscriptionSourceEntity?,
    allSources: List<SubscriptionSourceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (SubscriptionSourceEntity?, String, String, String, String, String, String, Boolean, Long) -> Unit,
) {
    var name by rememberSaveable(source?.id) { mutableStateOf(source?.name.orEmpty()) }
    var url by rememberSaveable(source?.id) { mutableStateOf(source?.url.orEmpty()) }
    var userAgent by rememberSaveable(source?.id) { mutableStateOf(source?.userAgent.orEmpty()) }
    var prefix by rememberSaveable(source?.id) { mutableStateOf(source?.prefix.orEmpty()) }
    var include by rememberSaveable(source?.id) { mutableStateOf(source?.includeRegex.orEmpty()) }
    var exclude by rememberSaveable(source?.id) { mutableStateOf(source?.excludeRegex.orEmpty()) }
    var auto by rememberSaveable(source?.id) { mutableStateOf(source?.autoRefreshEnabled ?: false) }
    var interval by rememberSaveable(source?.id) { mutableStateOf((source?.refreshIntervalMinutes ?: 720).toString()) }

    val nodeNames = remember(source?.cachedYaml) {
        extractNodeNames(source?.cachedYaml.orEmpty())
    }

    EditScreenScaffold(
        title = if (source == null) "添加订阅" else "编辑订阅",
        onDismiss = onDismiss,
        onSave = {
            onConfirm(source, name, url, userAgent, prefix, include, exclude, auto, interval.toLongOrNull() ?: 720)
        },
        saveEnabled = url.isNotBlank(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                iOSGroupedCard {
                    SmallFormField("名称", name, { name = it }, "留空则自动从订阅获取")
                    FieldDivider()
                    SmallFormField("订阅 URL", url, { url = it }, "https://...")
                    FieldDivider()
                    SmallFormField("User-Agent", userAgent, { userAgent = it }, "留空使用全局 UA")
                }
            }

            item {
                SectionHeader("节点筛选")
                iOSGroupedCard {
                    SmallFormField("节点前缀", prefix, { prefix = it }, "添加到节点名前，如: [A] ")
                    FieldDivider()
                    SmallFormField("保留正则", include, { include = it }, "如: 香港|台湾")
                    if (nodeNames.isNotEmpty()) {
                        RegexPreview(nodeNames, include, exclude)
                    } else {
                        RegexHint()
                    }
                    FieldDivider()
                    SmallFormField("排除正则", exclude, { exclude = it }, "如: 实验|过期|流量")
                    if (nodeNames.isNotEmpty()) {
                        RegexPreview(nodeNames, include, exclude)
                    }
                }
            }

            item {
                iOSGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动刷新", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = auto,
                            onCheckedChange = { auto = it },
                            modifier = Modifier.height(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    }
                    if (auto) {
                        FieldDivider()
                        SmallFormField("刷新间隔（分钟）", interval, { interval = it.filter(Char::isDigit).take(6) }, "720")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditScreen(
    template: TemplateEntity?,
    onDismiss: () -> Unit,
    onConfirm: (TemplateEntity?, String, String, String) -> Unit,
) {
    var name by rememberSaveable(template?.id) { mutableStateOf(template?.name ?: "Mihomo Template") }
    var remoteUrl by rememberSaveable(template?.id) { mutableStateOf(template?.remoteUrl.orEmpty()) }
    var body by rememberSaveable(template?.id) {
        mutableStateOf(template?.yamlBody ?: DEFAULT_MIHOMO_TEMPLATE.trimIndent())
    }

    EditScreenScaffold(
        title = if (template == null) "添加模板" else "编辑模板",
        onDismiss = onDismiss,
        onSave = { onConfirm(template, name, remoteUrl, body) },
        saveEnabled = name.isNotBlank(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                iOSGroupedCard {
                    SmallFormField("名称", name, { name = it }, "模板名称")
                    FieldDivider()
                    SmallFormField("远程 URL", remoteUrl, { remoteUrl = it }, "留空为纯本地模板")
                }
            }
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("YAML 内容", style = MaterialTheme.typography.bodySmall) },
                    minLines = 12,
                    maxLines = 30,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputEditScreen(
    profile: OutputProfileEntity?,
    sources: List<SubscriptionSourceEntity>,
    templates: List<TemplateEntity>,
    onDismiss: () -> Unit,
    onConfirm: (OutputProfileEntity?, String, List<Long>, Long, String, String, String, Int) -> Unit,
) {
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name ?: "Mihomo Output") }
    var selectedSourceIds by rememberSaveable(profile?.id, sources.size) {
        mutableStateOf(profile?.sourceIds ?: sources.joinToString(",") { it.id.toString() })
    }
    var selectedTemplateId by rememberSaveable(profile?.id, templates.size) {
        mutableStateOf(profile?.templateId ?: (templates.firstOrNull()?.id ?: 0L))
    }
    var prefix by rememberSaveable(profile?.id) { mutableStateOf(profile?.prefix.orEmpty()) }
    var include by rememberSaveable(profile?.id) { mutableStateOf(profile?.includeRegex.orEmpty()) }
    var exclude by rememberSaveable(profile?.id) { mutableStateOf(profile?.excludeRegex.orEmpty()) }
    var interval by rememberSaveable(profile?.id) { mutableStateOf((profile?.updateIntervalHours ?: 12).toString()) }

    val selectedSet = selectedSourceIds.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

    val mergedNodeNames = remember(sources.filter { it.id in selectedSet }.map { it.cachedYaml }) {
        sources.filter { it.id in selectedSet }.flatMap { extractNodeNames(it.cachedYaml.orEmpty()) }
    }

    EditScreenScaffold(
        title = if (profile == null) "添加输出" else "编辑输出",
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                profile,
                name,
                selectedSourceIds.split(',').mapNotNull { it.trim().toLongOrNull() },
                selectedTemplateId,
                prefix,
                include,
                exclude,
                interval.toIntOrNull() ?: 12,
            )
        },
        saveEnabled = name.isNotBlank() && selectedTemplateId > 0 && selectedSet.isNotEmpty(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                iOSGroupedCard {
                    SmallFormField("名称", name, { name = it }, "输出配置名称")
                }
            }

            item {
                SectionHeader("订阅源")
                iOSGroupedCard {
                    if (sources.isEmpty()) {
                        Text(
                            "请先添加订阅源",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        sources.forEachIndexed { index, src ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val next = if (src.id in selectedSet) selectedSet - src.id else selectedSet + src.id
                                        selectedSourceIds = next.sorted().joinToString(",")
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = src.id in selectedSet,
                                    onCheckedChange = { checked ->
                                        val next = if (checked) selectedSet + src.id else selectedSet - src.id
                                        selectedSourceIds = next.sorted().joinToString(",")
                                    },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(src.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (index < sources.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 42.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("模板")
                TemplateDropdown(templates, selectedTemplateId) { selectedTemplateId = it }
            }

            item {
                SectionHeader("节点筛选")
                iOSGroupedCard {
                    SmallFormField("节点前缀", prefix, { prefix = it }, "添加到节点名前")
                    FieldDivider()
                    SmallFormField("保留正则", include, { include = it }, "如: 香港|台湾|日本")
                    if (mergedNodeNames.isNotEmpty()) {
                        RegexPreview(mergedNodeNames, include, exclude)
                    } else {
                        RegexHint()
                    }
                    FieldDivider()
                    SmallFormField("排除正则", exclude, { exclude = it }, "如: 实验|过期")
                    if (mergedNodeNames.isNotEmpty()) {
                        RegexPreview(mergedNodeNames, include, exclude)
                    }
                }
            }

            item {
                iOSGroupedCard {
                    SmallFormField("更新间隔（小时）", interval, { interval = it.filter(Char::isDigit).take(4) }, "12")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodePreviewScreen(
    source: SubscriptionSourceEntity,
    onDismiss: () -> Unit,
) {
    val allNames = remember(source.cachedYaml) { extractNodeNames(source.cachedYaml.orEmpty()) }

    val includeRe = if (source.includeRegex.isNotBlank()) runCatching { Regex(source.includeRegex) }.getOrNull() else null
    val excludeRe = if (source.excludeRegex.isNotBlank()) runCatching { Regex(source.excludeRegex) }.getOrNull() else null

    val filtered = remember(allNames, source.includeRegex, source.excludeRegex) {
        allNames.map { name ->
            val included = includeRe == null || includeRe.containsMatchIn(name)
            val excluded = excludeRe != null && excludeRe.containsMatchIn(name)
            Triple(name, included && !excluded, source.prefix + name)
        }
    }
    val matchCount = filtered.count { it.second }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$matchCount / ${allNames.size} 个节点",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (allNames.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无节点数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("请先刷新订阅", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (source.prefix.isNotBlank() || source.includeRegex.isNotBlank() || source.excludeRegex.isNotBlank()) {
                    item {
                        Column(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (source.prefix.isNotBlank()) {
                                Text(
                                    "前缀: ${source.prefix}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (source.includeRegex.isNotBlank()) {
                                Text(
                                    "保留: ${source.includeRegex}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (source.excludeRegex.isNotBlank()) {
                                Text(
                                    "排除: ${source.excludeRegex}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                items(filtered.size) { index ->
                    val (originalName, matched, renamed) = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (matched) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (matched) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (matched) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                renamed,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (matched) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            if (renamed != originalName && matched) {
                                Text(
                                    originalName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(label, style = MaterialTheme.typography.labelMedium)
            },
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            textStyle = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FieldDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 14.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun RegexHint() {
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            "支持正则表达式，匹配节点名称。常见写法:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) {
                    append("香港|台湾     ")
                }
                append("→ ")
                withStyle(SpanStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) {
                    append("包含\"香港\"或\"台湾\"的节点")
                }
            },
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) {
                    append("(?=.*港)(?=.*BGP)")
                }
                append(" → ")
                withStyle(SpanStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) {
                    append("同时包含\"港\"和\"BGP\"")
                }
            },
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun RegexPreview(nodeNames: List<String>, includeRegex: String, excludeRegex: String) {
    val filtered = remember(nodeNames, includeRegex, excludeRegex) {
        val includeRe = if (includeRegex.isNotBlank()) runCatching { Regex(includeRegex) }.getOrNull() else null
        val excludeRe = if (excludeRegex.isNotBlank()) runCatching { Regex(excludeRegex) }.getOrNull() else null
        val isIncludeError = includeRegex.isNotBlank() && includeRe == null
        val isExcludeError = excludeRegex.isNotBlank() && excludeRe == null

        val results = nodeNames.map { name ->
            val included = includeRe == null || includeRe.containsMatchIn(name)
            val excluded = excludeRe != null && excludeRe.containsMatchIn(name)
            Triple(name, included && !excluded, isIncludeError || isExcludeError)
        }
        val matchCount = results.count { it.second }
        Pair(results, matchCount)
    }

    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "预览: ${filtered.second}/${nodeNames.size} 个节点匹配",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            filtered.first.take(20).forEach { (name, matched, error) ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = when {
                        error -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        matched -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (nodeNames.size > 20) {
                Text(
                    "... 还有 ${nodeNames.size - 20} 个节点",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun iOSStyleNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(82.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(tab.title, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun iOSGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun iOSTintedIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SourcesScreen(
    sources: List<SubscriptionSourceEntity>,
    refreshingSourceIds: Set<Long>,
    onRefresh: (Long) -> Unit,
    onEdit: (SubscriptionSourceEntity) -> Unit,
    onDelete: (SubscriptionSourceEntity) -> Unit,
    onViewNodes: (SubscriptionSourceEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (sources.isEmpty()) {
            item {
                iOSEmptyState(
                    icon = Icons.Outlined.CloudDownload,
                    title = "还没有订阅源",
                    subtitle = "点击右上角 + 添加你的第一个订阅",
                )
            }
        }
        items(sources, key = { it.id }) { source ->
            SourceCard(
                source = source,
                refreshing = source.id in refreshingSourceIds,
                onRefresh = { onRefresh(source.id) },
                onEdit = { onEdit(source) },
                onDelete = { onDelete(source) },
                onViewNodes = { onViewNodes(source) },
            )
        }
    }
}

@Composable
private fun SourceCard(
    source: SubscriptionSourceEntity,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewNodes: () -> Unit,
) {
    iOSGroupedCard(
        modifier = Modifier.clickable(onClick = onViewNodes),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iOSTintedIcon(Icons.Default.CloudDownload, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        source.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (refreshing) {
                        Text(
                            "正在刷新...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                } else {
                    iOSIconButton(Icons.Default.Refresh, "刷新", onRefresh)
                }
                iOSIconButton(Icons.Default.Edit, "编辑", onEdit)
                iOSIconButton(Icons.Default.Delete, "删除", onDelete, tint = MaterialTheme.colorScheme.error)
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val used = listOfNotNull(source.uploadBytes, source.downloadBytes).takeIf { it.isNotEmpty() }?.sum()
                val total = source.totalBytes

                if (total != null && total > 0) {
                    val usagePercent = ((used?.toFloat() ?: 0f) / total.toFloat() * 100).coerceIn(0f, 100f)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${formatBytes(used ?: 0)} / ${formatBytes(total)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${"%.1f".format(Locale.US, usagePercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(usagePercent / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        when {
                                            usagePercent > 90 -> MaterialTheme.colorScheme.error
                                            usagePercent > 70 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                    ),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    source.expireAtSeconds?.let {
                        Text(
                            "到期 ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: Text(
                        "到期未知",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    source.lastRefreshAt?.let {
                        Text(
                            "刷新于 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            source.lastError.takeIf { it.isNotBlank() }?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputsScreen(
    profiles: List<OutputProfileEntity>,
    sources: List<SubscriptionSourceEntity>,
    templates: List<TemplateEntity>,
    settings: ServerSettings,
    onEdit: (OutputProfileEntity) -> Unit,
    onDelete: (OutputProfileEntity) -> Unit,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lanAddress = remember { localLanAddress() }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (profiles.isEmpty()) {
            item {
                iOSEmptyState(
                    icon = Icons.Outlined.Dns,
                    title = "还没有输出订阅",
                    subtitle = "点击右上角 + 创建输出配置",
                )
            }
        }
        items(profiles, key = { it.id }) { profile ->
            val url = subscriptionUrl(settings, profile.id, lanAddress)
            OutputCard(
                profile = profile,
                url = url,
                sources = sources,
                templates = templates,
                onEdit = { onEdit(profile) },
                onDelete = { onDelete(profile) },
                onCopied = onCopied,
            )
        }
    }
}

@Composable
private fun OutputCard(
    profile: OutputProfileEntity,
    url: String,
    sources: List<SubscriptionSourceEntity>,
    templates: List<TemplateEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    iOSGroupedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iOSTintedIcon(Icons.Default.Dns, MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${sourceNames(profile, sources)} · ${templateName(profile.templateId, templates)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                iOSIconButton(Icons.Default.ContentCopy, "复制", {
                    clipboard.setText(AnnotatedString(url))
                    onCopied()
                })
                iOSIconButton(Icons.Default.Edit, "编辑", onEdit)
                iOSIconButton(Icons.Default.Delete, "删除", onDelete, tint = MaterialTheme.colorScheme.error)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .clickable {
                        clipboard.setText(AnnotatedString(url))
                        onCopied()
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesScreen(
    templates: List<TemplateEntity>,
    onRefresh: (Long) -> Unit,
    onEdit: (TemplateEntity) -> Unit,
    onDelete: (TemplateEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (templates.isEmpty()) {
            item {
                iOSEmptyState(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = "还没有模板",
                    subtitle = "点击右上角 + 创建配置模板",
                )
            }
        }
        items(templates, key = { it.id }) { template ->
            TemplateCard(
                template = template,
                onRefresh = { onRefresh(template.id) },
                onEdit = { onEdit(template) },
                onDelete = { onDelete(template) },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateEntity,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    iOSGroupedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iOSTintedIcon(
                    Icons.AutoMirrored.Filled.Article,
                    if (template.remoteUrl.isNotBlank()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        templateRefreshText(template),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (template.remoteUrl.isNotBlank()) {
                    iOSIconButton(Icons.Default.Refresh, "刷新", onRefresh)
                }
                iOSIconButton(Icons.Default.Edit, "编辑", onEdit)
                iOSIconButton(Icons.Default.Delete, "删除", onDelete, tint = MaterialTheme.colorScheme.error)
            }

            template.lastError.takeIf { it.isNotBlank() }?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (template.remoteUrl.isNotBlank()) {
                Text(
                    template.remoteUrl,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(8.dp),
            ) {
                Text(
                    template.yamlBody,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServerScreen(
    settings: ServerSettings,
    running: Boolean,
    onSave: (ServerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var port by rememberSaveable(settings.port) { mutableStateOf(settings.port.toString()) }
    var token by rememberSaveable(settings.token) { mutableStateOf(settings.token) }
    var allowLan by rememberSaveable(settings.allowLan) { mutableStateOf(settings.allowLan) }
    var globalUserAgent by rememberSaveable(settings.globalUserAgent) { mutableStateOf(settings.globalUserAgent) }
    val lanAddress = remember { localLanAddress() }
    val previewSettings = settings.copy(
        port = port.toIntOrNull() ?: settings.port,
        token = token,
        allowLan = allowLan,
        globalUserAgent = globalUserAgent,
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ServerStatusCard(
                running = running,
                onToggle = {
                    onSave(
                        ServerSettings(
                            enabled = !running,
                            allowLan = allowLan,
                            port = port.toIntOrNull() ?: 9876,
                            token = token,
                            globalUserAgent = globalUserAgent,
                        ),
                    )
                },
            )
        }

        item {
            iOSGroupedCard {
                iOSFormTextField("端口", port, { port = it.filter(Char::isDigit).take(5) }, "9876")
                FieldDivider()
                iOSFormTextField("访问 Token", token, { token = it }, "可选，留空则无需验证")
            }
        }

        item {
            iOSGroupedCard {
                iOSFormSwitch(
                    "允许局域网访问",
                    "开启后使用 ${lanAddress ?: "手机局域网 IP"} 分享",
                    allowLan,
                    { allowLan = it },
                )
            }
        }

        item {
            iOSGroupedCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "订阅拉取 User-Agent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = globalUserAgent,
                        onValueChange = { globalUserAgent = it },
                        placeholder = {
                            Text(
                                "全局请求 UA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "拉取订阅时使用的默认 User-Agent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onSave(previewSettings.copy(enabled = running)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("保存配置", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ServerStatusCard(
    running: Boolean,
    onToggle: () -> Unit,
) {
    iOSGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (running) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (running) Icons.Default.Check else Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (running) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "本地 HTTP 服务",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (running) "运行中" else "已停止",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (running) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onToggle,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(
                    if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(if (running) "停止" else "启动", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun iOSFormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            textStyle = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun iOSFormSwitch(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
            ),
        )
    }
}

@Composable
private fun iOSIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun IOSInfoRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun iOSEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TemplateDropdown(
    templates: List<TemplateEntity>,
    selectedTemplateId: Long,
    onSelected: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = templates.firstOrNull { it.id == selectedTemplateId }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                selected?.name ?: "选择模板",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
        ) {
            templates.forEach { template ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(template.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            if (template.id == selectedTemplateId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(template.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun extractNodeNames(yamlBody: String): List<String> {
    if (yamlBody.isBlank()) return emptyList()
    val root = runCatching {
        org.yaml.snakeyaml.Yaml().load<Map<String, Any?>>(yamlBody)
    }.getOrNull() ?: return emptyList()
    val proxies = (root["proxies"] as? List<*>).orEmpty()
    return proxies.mapNotNull { proxy ->
        (proxy as? Map<*, *>)?.get("name")?.toString()?.takeIf { it.isNotBlank() }
    }
}

private fun trafficText(source: SubscriptionSourceEntity): String {
    val total = source.totalBytes?.let(::formatBytes) ?: "未知"
    val used = listOfNotNull(source.uploadBytes, source.downloadBytes).takeIf { it.isNotEmpty() }?.sum()?.let(::formatBytes)
        ?: "未知"
    val remaining = source.totalBytes?.let { totalBytes ->
        val usedBytes = listOfNotNull(source.uploadBytes, source.downloadBytes).sum()
        formatBytes((totalBytes - usedBytes).coerceAtLeast(0))
    } ?: "未知"
    return "已用 $used / 剩余 $remaining / 总量 $total"
}

private fun templateRefreshText(template: TemplateEntity): String {
    val refreshTime = template.lastRefreshAt?.let {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))
    } ?: "未刷新"
    return if (template.remoteUrl.isBlank()) "本地模板" else "远程模板 · $refreshTime"
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(Locale.US, value, units[index])
}

private fun sourceNames(profile: OutputProfileEntity, sources: List<SubscriptionSourceEntity>): String {
    val ids = profile.sourceIds.split(',').mapNotNull { it.trim().toLongOrNull() }
    val names = ids.map { id -> sources.firstOrNull { it.id == id }?.name ?: "#$id" }
    return "订阅源: ${names.joinToString("、")}"
}

private fun templateName(templateId: Long, templates: List<TemplateEntity>): String {
    val name = templates.firstOrNull { it.id == templateId }?.name ?: "#$templateId"
    return "模板: $name"
}

private fun subscriptionUrl(settings: ServerSettings, profileId: Long, lanAddress: String?): String {
    val host = if (settings.allowLan) lanAddress ?: "PHONE_IP" else "127.0.0.1"
    val token = settings.token.takeIf { it.isNotBlank() }?.let { "?token=$it" }.orEmpty()
    return "http://$host:${settings.port}/subscriptions/$profileId.yaml$token"
}

private fun localLanAddress(): String? =
    runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()
