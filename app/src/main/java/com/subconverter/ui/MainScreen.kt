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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.DEFAULT_OVERRIDE_YAML
import com.subconverter.domain.DnsConnectionMode
import com.subconverter.domain.DnsProtocol
import com.subconverter.domain.PublicDnsPresets
import com.subconverter.domain.SubscriptionDnsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    Templates("覆写", Icons.AutoMirrored.Filled.Article, Icons.AutoMirrored.Outlined.Article),
    Server("服务", Icons.Filled.Settings, Icons.Outlined.Settings),
}

private enum class EditScreen { None, Source, Template, Output, Nodes, OutputNodes, OverrideHelp, Scan, QrShare }

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
    var viewingProfile by remember { mutableStateOf<OutputProfileEntity?>(null) }
    var scannedUrl by remember { mutableStateOf("") }
    var sharingUrl by remember { mutableStateOf("") }

    if (editScreen == EditScreen.None && selectedTab != MainTab.Sources) {
        androidx.activity.compose.BackHandler { selectedTab = MainTab.Sources }
    }

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
                                editScreen = EditScreen.Scan
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "扫码添加")
                            }
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
                                editScreen = EditScreen.OverrideHelp
                            }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "覆写说明")
                            }
                            IconButton(onClick = {
                                editingTemplate = null
                                editScreen = EditScreen.Template
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加覆写")
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
                onViewNodes = {
                    viewingProfile = it
                    editScreen = EditScreen.OutputNodes
                },
                onQrShare = {
                    sharingUrl = it
                    editScreen = EditScreen.QrShare
                },
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
                onMove = viewModel::moveTemplate,
                modifier = Modifier.padding(padding),
            )

            MainTab.Server -> ServerScreen(
                settings = state.settings,
                running = state.serverRunning,
                onSave = viewModel::updateServerSettings,
                onAutoStartOnBootChange = viewModel::updateAutoStartOnBoot,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (editScreen != EditScreen.None) {
        androidx.activity.compose.BackHandler { editScreen = EditScreen.None }
    }

    when (editScreen) {
        EditScreen.Source -> SourceEditScreen(
            source = editingSource,
            initialUrl = scannedUrl,
            allSources = state.sources,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { source, name, url, userAgent, prefix, include, exclude, auto, interval, preResolve, dnsConfig ->
                viewModel.saveSource(
                    source,
                    name,
                    url,
                    userAgent,
                    prefix,
                    include,
                    exclude,
                    auto,
                    interval,
                    preResolve,
                    dnsConfig,
                )
                editScreen = EditScreen.None
            },
        )

        EditScreen.Template -> TemplateEditScreen(
            template = editingTemplate,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { template, name, remoteUrl, body, enabled, global ->
                viewModel.saveTemplate(template, name, remoteUrl, body, enabled, global)
                editScreen = EditScreen.None
            },
        )

        EditScreen.Output -> OutputEditScreen(
            profile = editingProfile,
            sources = state.sources,
            templates = state.templates,
            gistToken = state.settings.gistToken,
            onDismiss = { editScreen = EditScreen.None },
            onConfirm = { profile, name, sourceIds, overrideIds, interval, uploadToGist ->
                viewModel.saveProfile(profile, name, sourceIds, overrideIds, interval, uploadToGist)
                editScreen = EditScreen.None
            },
        )

        EditScreen.Nodes -> viewingSource?.let { src ->
            NodePreviewScreen(
                source = src,
                onDismiss = { editScreen = EditScreen.None },
            )
        }

        EditScreen.OutputNodes -> viewingProfile?.let { profile ->
            OutputNodePreviewScreen(
                profile = profile,
                sources = state.sources,
                onDismiss = { editScreen = EditScreen.None },
            )
        }

        EditScreen.OverrideHelp -> OverrideHelpScreen(
            onDismiss = { editScreen = EditScreen.None },
        )

        EditScreen.Scan -> QrScanScreen(
            onScanned = { url ->
                scannedUrl = url
                editScreen = EditScreen.Source
            },
            onDismiss = { editScreen = EditScreen.None },
        )

        EditScreen.QrShare -> QrShareScreen(
            url = sharingUrl,
            onDismiss = { editScreen = EditScreen.None },
        )

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
    initialUrl: String,
    allSources: List<SubscriptionSourceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        SubscriptionSourceEntity?,
        String,
        String,
        String,
        String,
        String,
        String,
        Boolean,
        Long,
        Boolean,
        SubscriptionDnsConfig,
    ) -> Unit,
) {
    var name by rememberSaveable(source?.id) { mutableStateOf(source?.name.orEmpty()) }
    var url by rememberSaveable(source?.id) { mutableStateOf(source?.url.orEmpty().ifBlank { initialUrl }) }
    var userAgent by rememberSaveable(source?.id) { mutableStateOf(source?.userAgent.orEmpty()) }
    var prefix by rememberSaveable(source?.id) { mutableStateOf(source?.prefix.orEmpty()) }
    var include by rememberSaveable(source?.id) { mutableStateOf(source?.includeRegex.orEmpty()) }
    var exclude by rememberSaveable(source?.id) { mutableStateOf(source?.excludeRegex.orEmpty()) }
    var auto by rememberSaveable(source?.id) { mutableStateOf(source?.autoRefreshEnabled ?: false) }
    var interval by rememberSaveable(source?.id) { mutableStateOf((source?.refreshIntervalMinutes ?: 720).toString()) }
    var dnsProtocol by rememberSaveable(source?.id) { mutableStateOf(source?.dnsProtocol.orEmpty()) }
    var dnsServer by rememberSaveable(source?.id) { mutableStateOf(source?.dnsServer.orEmpty()) }
    var dnsConnectionMode by rememberSaveable(source?.id) {
        mutableStateOf(source?.dnsConnectionMode ?: DnsConnectionMode.PRESERVE_DOMAIN.name)
    }
    var allowHostnameMismatch by rememberSaveable(source?.id) {
        mutableStateOf(source?.allowHostnameMismatch ?: false)
    }
    var preResolveNodes by rememberSaveable(source?.id) {
        mutableStateOf(source?.preResolveNodes ?: false)
    }

    val nodeNames = remember(source?.cachedYaml) {
        extractNodeNames(source?.cachedYaml.orEmpty())
    }
    val parsedDnsProtocol = DnsProtocol.fromStorage(dnsProtocol)
    val parsedConnectionMode = DnsConnectionMode.fromStorage(dnsConnectionMode)
    val dnsConfig = SubscriptionDnsConfig(
        protocol = parsedDnsProtocol,
        server = dnsServer,
        connectionMode = parsedConnectionMode,
        allowHostnameMismatch = allowHostnameMismatch,
    )
    val selectedPreset = PublicDnsPresets.all.firstOrNull {
        it.protocol == parsedDnsProtocol && it.server.equals(dnsServer.trim(), ignoreCase = true)
    }
    val selectedDnsOption = when {
        parsedDnsProtocol == null -> "system"
        selectedPreset != null -> selectedPreset.id
        parsedDnsProtocol == DnsProtocol.DOH -> "custom_doh"
        else -> "custom_dot"
    }
    val dnsOptions = listOf("system" to "系统 DNS") +
        PublicDnsPresets.all.map { it.id to it.label } +
        listOf(
            "custom_doh" to "自定义 · DoH",
            "custom_dot" to "自定义 · DoT",
        )
    val dnsError = dnsConfig.validate()
    val isCustomDns = parsedDnsProtocol != null && selectedPreset == null

    EditScreenScaffold(
        title = if (source == null) "添加订阅" else "编辑订阅",
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                source,
                name,
                url,
                userAgent,
                prefix,
                include,
                exclude,
                auto,
                interval.toLongOrNull() ?: 720,
                preResolveNodes,
                dnsConfig,
            )
        },
        saveEnabled = url.isNotBlank() && dnsError == null,
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
                SectionHeader("DNS 解析")
                iOSGroupedCard {
                    ChoiceFormField(
                        label = "解析服务",
                        selectedId = selectedDnsOption,
                        options = dnsOptions,
                        onSelected = { option ->
                            when (option) {
                                "system" -> {
                                    dnsProtocol = ""
                                    dnsServer = ""
                                    dnsConnectionMode = DnsConnectionMode.PRESERVE_DOMAIN.name
                                    allowHostnameMismatch = false
                                }
                                "custom_doh" -> {
                                    if (selectedDnsOption != option) dnsServer = ""
                                    dnsProtocol = DnsProtocol.DOH.name
                                }
                                "custom_dot" -> {
                                    if (selectedDnsOption != option) dnsServer = ""
                                    dnsProtocol = DnsProtocol.DOT.name
                                }
                                else -> PublicDnsPresets.all.firstOrNull { it.id == option }?.let {
                                    dnsProtocol = it.protocol.name
                                    dnsServer = it.server
                                }
                            }
                        },
                    )
                    if (parsedDnsProtocol != null) {
                        if (isCustomDns) {
                            FieldDivider()
                            SmallFormField(
                                label = if (parsedDnsProtocol == DnsProtocol.DOH) "DoH 地址" else "DoT 服务器",
                                value = dnsServer,
                                onValueChange = { dnsServer = it },
                                placeholder = if (parsedDnsProtocol == DnsProtocol.DOH) {
                                    "https://dns.example/dns-query"
                                } else {
                                    "dns.example:853"
                                },
                            )
                        }
                        FieldDivider()
                        ChoiceFormField(
                            label = "连接方式",
                            selectedId = parsedConnectionMode.name,
                            options = listOf(
                                DnsConnectionMode.PRESERVE_DOMAIN.name to "保留域名（推荐）",
                                DnsConnectionMode.IP_URL.name to "直接使用 IP URL",
                            ),
                            onSelected = {
                                dnsConnectionMode = it
                                if (it != DnsConnectionMode.IP_URL.name) {
                                    allowHostnameMismatch = false
                                }
                            },
                        )
                        if (parsedConnectionMode == DnsConnectionMode.IP_URL) {
                            FieldDivider()
                            iOSFormSwitch(
                                label = "忽略证书主机名不匹配",
                                subtitle = "仍校验证书链、颁发机构和有效期",
                                checked = allowHostnameMismatch,
                                onCheckedChange = { allowHostnameMismatch = it },
                            )
                        }
                    }
                    FieldDivider()
                    iOSFormSwitch(
                        label = "预解析节点域名",
                        subtitle = "刷新时解析节点 IP，并按 DNS TTL 自动更新",
                        checked = preResolveNodes,
                        onCheckedChange = { preResolveNodes = it },
                    )
                }
                Text(
                    when {
                        parsedDnsProtocol == null && preResolveNodes ->
                            "订阅更新和节点预解析均使用系统 DNS，节点缓存有效期为 1 小时。"
                        parsedDnsProtocol == null -> "不覆盖解析，订阅更新使用系统 DNS。"
                        parsedConnectionMode == DnsConnectionMode.PRESERVE_DOMAIN ->
                            if (preResolveNodes) {
                                "订阅连接保留域名；节点 server 将使用指定 DNS 预解析的 IP。"
                            } else {
                                "连接指定 DNS 返回的 IP，同时保留原域名、Host 与 SNI。"
                            }
                        else -> "URL 主机将改写为解析 IP，部分 HTTPS 或 CDN 订阅可能无法访问。"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                dnsError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item {
                SectionHeader("节点筛选")
                iOSGroupedCard {
                    SmallFormField("节点前缀", prefix, { prefix = it }, "添加到节点名前，如: [A] ")
                    FieldDivider()
                    SmallFormField("保留正则", include, { include = it }, "如: 香港|台湾")
                    FieldDivider()
                    SmallFormField("排除正则", exclude, { exclude = it }, "如: 实验|过期|流量")
                    if (nodeNames.isNotEmpty()) {
                        RegexPreview(nodeNames, include, exclude)
                    } else {
                        RegexHint()
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
    onConfirm: (TemplateEntity?, String, String, String, Boolean, Boolean) -> Unit,
) {
    var name by rememberSaveable(template?.id) { mutableStateOf(template?.name ?: "YAML 覆写") }
    var remoteUrl by rememberSaveable(template?.id) { mutableStateOf(template?.remoteUrl.orEmpty()) }
    var enabled by rememberSaveable(template?.id) { mutableStateOf(template?.enabled ?: true) }
    var global by rememberSaveable(template?.id) { mutableStateOf(template?.global ?: false) }
    var body by rememberSaveable(template?.id) {
        mutableStateOf(template?.yamlBody ?: DEFAULT_OVERRIDE_YAML.trimIndent())
    }

    EditScreenScaffold(
        title = if (template == null) "添加覆写" else "编辑覆写",
        onDismiss = onDismiss,
        onSave = { onConfirm(template, name, remoteUrl, body, enabled, global) },
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
                    SmallFormField("名称", name, { name = it }, "覆写名称")
                    FieldDivider()
                    SmallFormField("远程 URL", remoteUrl, { remoteUrl = it }, "留空为本地覆写")
                    FieldDivider()
                    iOSFormSwitch("启用覆写", "关闭后不会参与任何输出", enabled, { enabled = it })
                    FieldDivider()
                    iOSFormSwitch("全局覆写", "开启后自动应用到所有输出", global, { global = it })
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
private fun OverrideHelpScreen(
    onDismiss: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("覆写说明", style = MaterialTheme.typography.titleMedium) },
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
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "执行顺序",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "基础配置生成后，先应用全局覆写，再按输出配置里的顺序应用专属覆写。同一个覆写只会执行一次。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionHeader("追加规则到末尾")
                OverrideHelpCodeBlock(
                    """
                    rules+:
                      - DOMAIN-SUFFIX,example.com,DIRECT
                    """.trimIndent(),
                )
            }

            item {
                SectionHeader("插入规则到开头")
                OverrideHelpCodeBlock(
                    """
                    +rules:
                      - DOMAIN,api.example.com,PROXY
                    """.trimIndent(),
                )
            }

            item {
                SectionHeader("整体替换字段")
                OverrideHelpCodeBlock(
                    """
                    proxy-groups!:
                      - name: PROXY
                        type: select
                        proxies: "{{proxy_names}}"
                    """.trimIndent(),
                )
            }

            item {
                SectionHeader("深合并对象")
                OverrideHelpCodeBlock(
                    """
                    dns:
                      enable: true
                      enhanced-mode: fake-ip
                    """.trimIndent(),
                )
            }
        }
    }
}

@Composable
private fun OverrideHelpCodeBlock(text: String) {
    iOSGroupedCard {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputEditScreen(
    profile: OutputProfileEntity?,
    sources: List<SubscriptionSourceEntity>,
    templates: List<TemplateEntity>,
    gistToken: String,
    onDismiss: () -> Unit,
    onConfirm: (OutputProfileEntity?, String, List<Long>, List<Long>, Int, Boolean) -> Unit,
) {
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name ?: "Mihomo Output") }
    var selectedSourceIds by rememberSaveable(profile?.id, sources.size) {
        mutableStateOf(profile?.sourceIds ?: sources.joinToString(",") { it.id.toString() })
    }
    var selectedOverrideIds by rememberSaveable(profile?.id, templates.size) {
        mutableStateOf(profile?.overrideIds.orEmpty())
    }
    var interval by rememberSaveable(profile?.id) { mutableStateOf((profile?.updateIntervalHours ?: 12).toString()) }
    var uploadToGist by rememberSaveable(profile?.id) { mutableStateOf(profile?.uploadToGist ?: false) }

    val selectedSet = remember(selectedSourceIds) {
        selectedSourceIds.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
    }
    val selectedOverrideIdList = remember(selectedOverrideIds) {
        parseIdList(selectedOverrideIds)
    }

    val gistSubtitle: String
    val gistSubtitleColor: Color
    if (gistToken.isBlank()) {
        gistSubtitle = "未配置 Gist Token（去服务页设置）"
        gistSubtitleColor = MaterialTheme.colorScheme.error
    } else {
        gistSubtitle = "刷新订阅后自动上传到 GitHub Gist"
        gistSubtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    }

    EditScreenScaffold(
        title = if (profile == null) "添加输出" else "编辑输出",
        onDismiss = onDismiss,
        onSave = {
            onConfirm(
                profile,
                name,
                selectedSourceIds.split(',').mapNotNull { it.trim().toLongOrNull() },
                selectedOverrideIdList,
                interval.toIntOrNull() ?: 12,
                uploadToGist,
            )
        },
        saveEnabled = name.isNotBlank() && selectedSet.isNotEmpty(),
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
                SectionHeader("专属覆写")
                iOSGroupedCard {
                    OverrideSelectionList(
                        overrides = templates,
                        selectedIds = selectedOverrideIdList,
                        onSelectedIdsChange = { ids ->
                            selectedOverrideIds = ids.joinToString(",")
                        },
                    )
                }
            }

            item {
                iOSGroupedCard {
                    SmallFormField("更新间隔（小时）", interval, { interval = it.filter(Char::isDigit).take(4) }, "12")
                }
            }

            item {
                iOSGroupedCard {
                    iOSFormSwitch(
                        label = "上传到 Gist",
                        subtitle = gistSubtitle,
                        checked = uploadToGist,
                        onCheckedChange = { uploadToGist = it },
                        subtitleColor = gistSubtitleColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverrideSelectionList(
    overrides: List<TemplateEntity>,
    selectedIds: List<Long>,
    onSelectedIdsChange: (List<Long>) -> Unit,
) {
    if (overrides.isEmpty()) {
        Text(
            "请先添加覆写",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val selectedSet = selectedIds.toSet()
    val selectedOverrides = selectedIds.mapNotNull { id -> overrides.firstOrNull { it.id == id } }
    val unselectedOverrides = overrides.filter { it.id !in selectedSet }

    if (selectedOverrides.isEmpty()) {
        Text(
            "未选择专属覆写，将只应用全局覆写",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    selectedOverrides.forEachIndexed { index, overrideItem ->
        OverrideSelectionRow(
            overrideItem = overrideItem,
            selected = true,
            canMoveUp = index > 0,
            canMoveDown = index < selectedOverrides.lastIndex,
            onToggle = { onSelectedIdsChange(selectedIds.filterNot { it == overrideItem.id }) },
            onMoveUp = { onSelectedIdsChange(moveId(selectedIds, overrideItem.id, -1)) },
            onMoveDown = { onSelectedIdsChange(moveId(selectedIds, overrideItem.id, 1)) },
        )
        if (index < selectedOverrides.lastIndex || unselectedOverrides.isNotEmpty()) {
            FieldDivider()
        }
    }

    unselectedOverrides.forEachIndexed { index, overrideItem ->
        OverrideSelectionRow(
            overrideItem = overrideItem,
            selected = false,
            canMoveUp = false,
            canMoveDown = false,
            onToggle = { onSelectedIdsChange(selectedIds + overrideItem.id) },
            onMoveUp = {},
            onMoveDown = {},
        )
        if (index < unselectedOverrides.lastIndex) {
            FieldDivider()
        }
    }
}

@Composable
private fun OverrideSelectionRow(
    overrideItem: TemplateEntity,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                overrideItem.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                overrideStateText(overrideItem),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputNodePreviewScreen(
    profile: OutputProfileEntity,
    sources: List<SubscriptionSourceEntity>,
    onDismiss: () -> Unit,
) {
    val profileSourceIds = remember(profile.sourceIds) {
        profile.sourceIds.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()
    }
    val profileSources = remember(profileSourceIds, sources) {
        profileSourceIds.mapNotNull { id -> sources.find { it.id == id } }
            .filter { it.cachedYaml.isNotBlank() }
    }

    val allNodes = remember(profileSources) {
        profileSources.flatMap { source ->
            val names = extractNodeNames(source.cachedYaml)
            val srcIncludeRe = source.includeRegex.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() }
            val srcExcludeRe = source.excludeRegex.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() }
            names.mapNotNull { name ->
                val included = srcIncludeRe == null || srcIncludeRe.containsMatchIn(name)
                val excluded = srcExcludeRe != null && srcExcludeRe.containsMatchIn(name)
                if (!included || excluded) return@mapNotNull null
                source.prefix + name
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${allNodes.size} 个节点",
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
        if (allNodes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无节点", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("请确保关联订阅已刷新且有匹配节点", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                items(allNodes.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            allNodes[index],
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
private fun ChoiceFormField(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second.orEmpty()
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    selectedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        expanded = false
                        onSelected(id)
                    },
                    trailingIcon = {
                        if (id == selectedId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
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

private const val REGEX_PREVIEW_LIMIT = 20

private data class RegexPreviewItem(
    val name: String,
    val matched: Boolean,
)

private data class RegexPreviewUiState(
    val items: List<RegexPreviewItem> = emptyList(),
    val matchCount: Int = 0,
    val hasRegexError: Boolean = false,
    val isCalculating: Boolean = false,
)

@Composable
private fun RegexPreview(nodeNames: List<String>, includeRegex: String, excludeRegex: String) {
    var preview by remember(nodeNames) {
        mutableStateOf(RegexPreviewUiState(isCalculating = true))
    }

    LaunchedEffect(nodeNames, includeRegex, excludeRegex) {
        preview = preview.copy(isCalculating = true, hasRegexError = false)
        delay(160)
        preview = withContext(Dispatchers.Default) {
            calculateRegexPreview(nodeNames, includeRegex, excludeRegex)
        }
    }

    val summaryText = when {
        preview.isCalculating -> "预览计算中..."
        preview.hasRegexError -> "预览: 正则表达式有误"
        else -> "预览: ${preview.matchCount}/${nodeNames.size} 个节点匹配"
    }
    val summaryColor = if (preview.hasRegexError && !preview.isCalculating) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = summaryColor.copy(alpha = 0.6f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                summaryText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = summaryColor,
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
            if (preview.items.isEmpty()) {
                Text(
                    text = "正在计算预览...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            preview.items.forEach { item ->
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = when {
                        preview.hasRegexError -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        item.matched -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!preview.isCalculating && nodeNames.size > preview.items.size) {
                Text(
                    "... 还有 ${nodeNames.size - preview.items.size} 个节点",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

private fun calculateRegexPreview(
    nodeNames: List<String>,
    includeRegex: String,
    excludeRegex: String,
): RegexPreviewUiState {
    val includeRe = includeRegex.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() }
    val excludeRe = excludeRegex.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() }
    val hasRegexError = (includeRegex.isNotBlank() && includeRe == null) ||
        (excludeRegex.isNotBlank() && excludeRe == null)

    if (hasRegexError) {
        return RegexPreviewUiState(
            items = nodeNames.take(REGEX_PREVIEW_LIMIT).map { RegexPreviewItem(name = it, matched = false) },
            hasRegexError = true,
        )
    }

    val items = ArrayList<RegexPreviewItem>(minOf(nodeNames.size, REGEX_PREVIEW_LIMIT))
    var matchCount = 0
    nodeNames.forEach { name ->
        val included = includeRe == null || includeRe.containsMatchIn(name)
        val excluded = excludeRe != null && excludeRe.containsMatchIn(name)
        val matched = included && !excluded
        if (matched) {
            matchCount += 1
        }
        if (items.size < REGEX_PREVIEW_LIMIT) {
            items += RegexPreviewItem(name = name, matched = matched)
        }
    }

    return RegexPreviewUiState(
        items = items,
        matchCount = matchCount,
    )
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
                    } else {
                        sourceDnsLabel(source)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
                            "上次成功 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))}",
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
    onViewNodes: (OutputProfileEntity) -> Unit,
    onQrShare: (String) -> Unit,
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
                onViewNodes = { onViewNodes(profile) },
                onQrShare = { onQrShare(url) },
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
    onViewNodes: () -> Unit,
    onQrShare: () -> Unit,
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
                        "${sourceNames(profile, sources)} · ${overrideSummary(profile, templates)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.fetchCount > 0) {
                        Text(
                            "已拉取 ${profile.fetchCount} 次",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                iOSIconButton(Icons.Default.Visibility, "预览", onViewNodes)
                iOSIconButton(Icons.Default.QrCode, "二维码", onQrShare)
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
    onMove: (Long, Int) -> Unit,
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
                    title = "还没有覆写",
                    subtitle = "点击右上角 + 创建 YAML 覆写",
                )
            }
        }
        items(templates.size, key = { templates[it].id }) { index ->
            val template = templates[index]
            TemplateCard(
                template = template,
                onRefresh = { onRefresh(template.id) },
                onEdit = { onEdit(template) },
                onDelete = { onDelete(template) },
                onMoveUp = { onMove(template.id, -1) },
                onMoveDown = { onMove(template.id, 1) },
                canMoveUp = index > 0,
                canMoveDown = index < templates.lastIndex,
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
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
                        overrideCardSubtitle(template),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
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
    onAutoStartOnBootChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var port by rememberSaveable(settings.port) { mutableStateOf(settings.port.toString()) }
    var token by rememberSaveable(settings.token) { mutableStateOf(settings.token) }
    var allowLan by rememberSaveable(settings.allowLan) { mutableStateOf(settings.allowLan) }
    var autoStartOnBoot by rememberSaveable(settings.autoStartOnBoot) { mutableStateOf(settings.autoStartOnBoot) }
    var globalUserAgent by rememberSaveable(settings.globalUserAgent) { mutableStateOf(settings.globalUserAgent) }
    var gistToken by rememberSaveable(settings.gistToken) { mutableStateOf(settings.gistToken) }
    val lanAddress = remember(allowLan) { if (allowLan) localLanAddress() else null }
    val allowLanDescription = if (allowLan) {
        "已开启，使用 ${lanAddress ?: "手机局域网 IP"} 分享"
    } else {
        "关闭时仅本机访问，开启后显示局域网地址"
    }
    val previewSettings = settings.copy(
        port = port.toIntOrNull() ?: settings.port,
        token = token,
        allowLan = allowLan,
        autoStartOnBoot = autoStartOnBoot,
        globalUserAgent = globalUserAgent,
        gistToken = gistToken,
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
                            autoStartOnBoot = autoStartOnBoot,
                            allowLan = allowLan,
                            port = port.toIntOrNull() ?: 9876,
                            token = token,
                            globalUserAgent = globalUserAgent,
                            gistToken = gistToken,
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
                    allowLanDescription,
                    allowLan,
                    { allowLan = it },
                )
                FieldDivider()
                iOSFormSwitch(
                    "开机自启动",
                    "设备重启后自动启动本地 HTTP 服务",
                    autoStartOnBoot,
                    {
                        autoStartOnBoot = it
                        onAutoStartOnBootChange(it)
                    },
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
            iOSGroupedCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "GitHub Gist Token",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = gistToken,
                        onValueChange = { gistToken = it },
                        placeholder = {
                            Text(
                                "ghp_xxx（需 gist 权限）",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                        "上传配置到 Gist 用的个人访问令牌，需 gist 权限。留空则不开启上传。",
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
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = subtitleColor,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScanScreen(
    onScanned: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        val permission = android.Manifest.permission.CAMERA
        hasPermission = ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) launcher.launch(permission)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("扫描二维码", style = MaterialTheme.typography.titleMedium) },
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
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("需要相机权限", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("授予权限")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val cameraProviderFuture = remember {
                    androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
                }
                AndroidView(
                    factory = { ctx ->
                        val previewView = androidx.camera.view.PreviewView(ctx)
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build()
                        val selector = androidx.camera.core.CameraSelector.Builder()
                            .requireLensFacing(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                            .build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        val analyzer = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            if (scanned) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                try {
                                    val w = mediaImage.width
                                    val h = mediaImage.height
                                    val yBuf = mediaImage.planes[0].buffer
                                    val uBuf = mediaImage.planes[1].buffer
                                    val vBuf = mediaImage.planes[2].buffer
                                    val ySz = yBuf.remaining()
                                    val uSz = uBuf.remaining()
                                    val vSz = vBuf.remaining()
                                    val nv21 = ByteArray(ySz + uSz + vSz)
                                    yBuf.get(nv21, 0, ySz)
                                    vBuf.get(nv21, ySz, vSz)
                                    uBuf.get(nv21, ySz + vSz, uSz)
                                    val yuv = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, w, h, null)
                                    val out = java.io.ByteArrayOutputStream()
                                    yuv.compressToJpeg(android.graphics.Rect(0, 0, w, h), 90, out)
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                    val rotated = if (rotation != 0 && bmp != null) {
                                        val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                        android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                    } else bmp
                                    if (rotated != null) {
                                        val pixels = IntArray(rotated.width * rotated.height)
                                        rotated.getPixels(pixels, 0, rotated.width, 0, 0, rotated.width, rotated.height)
                                        val source = com.google.zxing.RGBLuminanceSource(rotated.width, rotated.height, pixels)
                                        val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                                        val hints = mapOf(
                                            com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to
                                                listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                                        )
                                        val result = com.google.zxing.MultiFormatReader().apply { setHints(hints) }.decode(binaryBitmap)
                                        val url = result.text
                                        if (url.isNotBlank()) {
                                            scanned = true
                                            onScanned(url)
                                        }
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.activity.ComponentActivity,
                            selector,
                            preview,
                            analyzer,
                        )
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrShareScreen(
    url: String,
    onDismiss: () -> Unit,
) {
    val qrBitmap = remember(url) { generateQrBitmap(url) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("二维码分享", style = MaterialTheme.typography.titleMedium) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.size(260.dp),
            ) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .padding(16.dp)
                        .aspectRatio(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

private fun generateQrBitmap(content: String): ImageBitmap {
    val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
        content, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints,
    )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
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

private fun sourceDnsLabel(source: SubscriptionSourceEntity): String? {
    val protocol = DnsProtocol.fromStorage(source.dnsProtocol)
    val downloadDns = protocol?.let {
        val preset = PublicDnsPresets.all.firstOrNull { preset ->
            preset.protocol == it && preset.server.equals(source.dnsServer.trim(), ignoreCase = true)
        }
        val resolver = preset?.label ?: "自定义 ${it.name}"
        val mode = when (DnsConnectionMode.fromStorage(source.dnsConnectionMode)) {
            DnsConnectionMode.PRESERVE_DOMAIN -> "保留域名"
            DnsConnectionMode.IP_URL -> "IP URL"
        }
        "$resolver · $mode"
    }
    val nodeDns = if (source.preResolveNodes) {
        val total = source.nodeResolveSuccessCount + source.nodeResolveFailureCount
        "节点预解析 ${source.nodeResolveSuccessCount}/$total"
    } else {
        null
    }
    return listOfNotNull(downloadDns, nodeDns).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun overrideCardSubtitle(template: TemplateEntity): String {
    val refreshTime = template.lastRefreshAt?.let {
        "上次成功 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))}"
    } ?: "未成功刷新"
    return listOf(
        overrideStateText(template),
        if (template.remoteUrl.isBlank()) "本地覆写" else "远程覆写 · $refreshTime",
    ).joinToString(" · ")
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
    val ids = parseIdList(profile.sourceIds)
    val names = ids.map { id -> sources.firstOrNull { it.id == id }?.name ?: "#$id" }
    return "订阅源: ${names.joinToString("、")}"
}

private fun overrideSummary(profile: OutputProfileEntity, overrides: List<TemplateEntity>): String {
    val globalCount = overrides.count { it.enabled && it.global }
    val selectedNames = parseIdList(profile.overrideIds)
        .mapNotNull { id -> overrides.firstOrNull { it.id == id }?.name }

    val parts = mutableListOf<String>()
    if (globalCount > 0) {
        parts += "全局覆写 $globalCount 个"
    }
    parts += if (selectedNames.isEmpty()) {
        "专属覆写: 无"
    } else {
        "专属覆写: ${selectedNames.joinToString("、")}"
    }
    return parts.joinToString(" · ")
}

private fun overrideStateText(template: TemplateEntity): String =
    listOfNotNull(
        if (template.enabled) "启用" else "停用",
        if (template.global) "全局" else null,
    ).joinToString(" · ")

private fun parseIdList(rawIds: String): List<Long> =
    rawIds.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()

private fun moveId(ids: List<Long>, id: Long, offset: Int): List<Long> {
    val currentIndex = ids.indexOf(id)
    if (currentIndex < 0) return ids
    val targetIndex = (currentIndex + offset).coerceIn(0, ids.lastIndex)
    if (currentIndex == targetIndex) return ids
    return ids.toMutableList().apply {
        removeAt(currentIndex)
        add(targetIndex, id)
    }
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
