package com.subconverter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subconverter.data.OutputProfileEntity
import com.subconverter.data.SubscriptionSourceEntity
import com.subconverter.data.TemplateEntity
import com.subconverter.data.TemplateType
import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.DEFAULT_OVERRIDE_JS
import com.subconverter.domain.DEFAULT_OVERRIDE_YAML
import com.subconverter.domain.DnsConnectionMode
import com.subconverter.domain.DnsProtocol
import com.subconverter.domain.PublicDnsPresets
import com.subconverter.domain.SubscriptionDnsConfig
import com.subconverter.i18n.AppI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun String.l10n(): String = AppI18n.text(LocalContext.current, this)

@Composable
private fun localize(text: String): String = AppI18n.text(LocalContext.current, text)

@Composable
private fun l10nf(format: String, vararg args: Any): String =
    AppI18n.format(LocalContext.current, format, *args)

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

private enum class EditScreen { None, Source, Template, Output, Nodes, OutputPreview, OverrideHelp, Scan, QrShare }

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
                        selectedTab.title.l10n(),
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
                                editingSource = null
                                scannedUrl = ""
                                editScreen = EditScreen.Scan
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = localize("扫码添加"))
                            }
                            IconButton(onClick = {
                                state.sources.forEach { viewModel.refreshSource(it.id) }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = localize("全部刷新"))
                            }
                            IconButton(onClick = {
                                editingSource = null
                                scannedUrl = ""
                                editScreen = EditScreen.Source
                            }) {
                                Icon(Icons.Default.Add, contentDescription = localize("添加订阅"))
                            }
                        }
                        MainTab.Outputs -> {
                            IconButton(onClick = {
                                editingProfile = null
                                editScreen = EditScreen.Output
                            }) {
                                Icon(Icons.Default.Add, contentDescription = localize("添加输出"))
                            }
                        }
                        MainTab.Templates -> {
                            IconButton(onClick = {
                                editScreen = EditScreen.OverrideHelp
                            }) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = localize("覆写说明"))
                            }
                            IconButton(onClick = {
                                editingTemplate = null
                                editScreen = EditScreen.Template
                            }) {
                                Icon(Icons.Default.Add, contentDescription = localize("添加覆写"))
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
                    scannedUrl = ""
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
                    viewModel.previewProfile(it.id)
                    editScreen = EditScreen.OutputPreview
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
                onCopied = { viewModel.showMessage("zashboard 地址已复制") },
                onQrShare = {
                    sharingUrl = it
                    editScreen = EditScreen.QrShare
                },
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
            onConfirm = { template, name, remoteUrl, body, enabled, global, type ->
                viewModel.saveTemplate(template, name, remoteUrl, body, enabled, global, type)
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

        EditScreen.OutputPreview -> OutputPreviewScreen(
            previewState = viewModel.previewState.collectAsStateWithLifecycle().value,
            onDismiss = {
                viewModel.clearPreview()
                editScreen = EditScreen.None
            },
        )

        EditScreen.OverrideHelp -> OverrideHelpScreen(
            onDismiss = { editScreen = EditScreen.None },
        )

        EditScreen.Scan -> QrScanScreen(
            onScanned = { url ->
                editingSource = null
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title.l10n(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = saveEnabled,
                    ) {
                        Text("保存".l10n(), fontWeight = FontWeight.SemiBold)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val sourceFormKey = source?.id?.toString() ?: "new:$initialUrl"
    var name by rememberSaveable(sourceFormKey) { mutableStateOf(source?.name.orEmpty()) }
    var url by rememberSaveable(sourceFormKey) { mutableStateOf(source?.url.orEmpty().ifBlank { initialUrl }) }
    var userAgent by rememberSaveable(sourceFormKey) { mutableStateOf(source?.userAgent.orEmpty()) }
    var prefix by rememberSaveable(sourceFormKey) { mutableStateOf(source?.prefix.orEmpty()) }
    var include by rememberSaveable(sourceFormKey) { mutableStateOf(source?.includeRegex.orEmpty()) }
    var exclude by rememberSaveable(sourceFormKey) { mutableStateOf(source?.excludeRegex.orEmpty()) }
    var auto by rememberSaveable(sourceFormKey) { mutableStateOf(source?.autoRefreshEnabled ?: false) }
    var interval by rememberSaveable(sourceFormKey) { mutableStateOf((source?.refreshIntervalMinutes ?: 720).toString()) }
    var dnsProtocol by rememberSaveable(sourceFormKey) { mutableStateOf(source?.dnsProtocol.orEmpty()) }
    var dnsServer by rememberSaveable(sourceFormKey) { mutableStateOf(source?.dnsServer.orEmpty()) }
    var dnsConnectionMode by rememberSaveable(sourceFormKey) {
        mutableStateOf(source?.dnsConnectionMode ?: DnsConnectionMode.PRESERVE_DOMAIN.name)
    }
    var allowHostnameMismatch by rememberSaveable(sourceFormKey) {
        mutableStateOf(source?.allowHostnameMismatch ?: false)
    }
    var preResolveNodes by rememberSaveable(sourceFormKey) {
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
                .imePadding()
                .imeNestedScroll()
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
                            localize("订阅更新和节点预解析均使用系统 DNS，节点缓存有效期为 1 小时。")
                        parsedDnsProtocol == null -> localize("不覆盖解析，订阅更新使用系统 DNS。")
                        parsedConnectionMode == DnsConnectionMode.PRESERVE_DOMAIN ->
                            if (preResolveNodes) {
                                localize("订阅连接保留域名；节点 server 将使用指定 DNS 预解析的 IP。")
                            } else {
                                localize("连接指定 DNS 返回的 IP，同时保留原域名、Host 与 SNI。")
                            }
                        else -> localize("URL 主机将改写为解析 IP，部分 HTTPS 或 CDN 订阅可能无法访问。")
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
                            Text("自动刷新".l10n(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditScreen(
    template: TemplateEntity?,
    onDismiss: () -> Unit,
    onConfirm: (TemplateEntity?, String, String, String, Boolean, Boolean, String) -> Unit,
) {
    val yamlOverrideName = localize("YAML 覆写")
    val jsOverrideName = localize("JavaScript 覆写")
    var name by rememberSaveable(template?.id, yamlOverrideName) { mutableStateOf(template?.name ?: yamlOverrideName) }
    var remoteUrl by rememberSaveable(template?.id) { mutableStateOf(template?.remoteUrl.orEmpty()) }
    var enabled by rememberSaveable(template?.id) { mutableStateOf(template?.enabled ?: true) }
    var global by rememberSaveable(template?.id) { mutableStateOf(template?.global ?: false) }
    var type by rememberSaveable(template?.id) { mutableStateOf(template?.type ?: TemplateType.YAML) }
    var editorFullScreen by rememberSaveable(template?.id) { mutableStateOf(false) }
    var body by rememberSaveable(template?.id) {
        mutableStateOf(
            template?.yamlBody ?: DEFAULT_OVERRIDE_YAML.trimIndent(),
        )
    }

    fun defaultBodyFor(type: String) = when (type) {
        TemplateType.JS -> DEFAULT_OVERRIDE_JS.trimIndent()
        else -> DEFAULT_OVERRIDE_YAML.trimIndent()
    }

    if (editorFullScreen) {
        FullScreenCodeEditor(
            value = body,
            onValueChange = { body = it },
            type = type,
            onClose = { editorFullScreen = false },
        )
        return
    }

    EditScreenScaffold(
        title = if (template == null) "添加覆写" else "编辑覆写",
        onDismiss = onDismiss,
        onSave = { onConfirm(template, name, remoteUrl, body, enabled, global, type) },
        saveEnabled = name.isNotBlank(),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .imeNestedScroll()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                iOSGroupedCard {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("覆写类型".l10n(), style = MaterialTheme.typography.bodySmall)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(TemplateType.YAML to "YAML", TemplateType.JS to "JavaScript")
                            options.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = type == value,
                                    onClick = {
                                        if (type != value) {
                                            if (body.isBlank() || body == defaultBodyFor(type)) {
                                                body = defaultBodyFor(value)
                                            }
                                            if (name == "YAML 覆写" || name == "JavaScript 覆写" ||
                                                name == "YAML Override" || name == "JavaScript Override"
                                            ) {
                                                name = if (value == TemplateType.JS) jsOverrideName else yamlOverrideName
                                            }
                                            type = value
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    FieldDivider()
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
                CodeEditorField(
                    value = body,
                    onValueChange = { body = it },
                    type = type,
                    modifier = Modifier.fillMaxWidth(),
                    onRequestFullScreen = { editorFullScreen = true },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FullScreenCodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    type: String,
    onClose: () -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val title = if (type == TemplateType.JS) "JavaScript" else "YAML"
    val lineCount = remember(value) { value.count { it == '\n' } + 1 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            l10nf("%s · %d 行", title, lineCount),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text("完成".l10n(), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
    ) { padding ->
        CodeEditorField(
            value = value,
            onValueChange = onValueChange,
            type = type,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .imeNestedScroll(),
            fullScreen = true,
            showHeader = false,
        )
    }
}

@Composable
private fun CodeEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    type: String,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    showHeader: Boolean = true,
    onRequestFullScreen: (() -> Unit)? = null,
) {
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    var revealCursorRequest by remember { mutableStateOf(0) }
    LaunchedEffect(value) {
        if (value != editor.text) {
            val cursor = editor.selection.start.coerceIn(0, value.length)
            editor = TextFieldValue(value, TextRange(cursor))
        }
    }

    fun applyEditor(next: TextFieldValue) {
        val textChanged = next.text != editor.text
        editor = next
        onValueChange(next.text)
        if (textChanged) {
            revealCursorRequest += 1
        }
    }

    val density = LocalDensity.current
    val context = LocalContext.current
    var yamlCompletionOffsetY by remember { mutableStateOf(0.dp) }
    var cursorRect by remember { mutableStateOf<Rect?>(null) }
    var editorViewportHeight by remember { mutableStateOf(0) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lineCount = remember(editor.text) { editor.text.count { it == '\n' } + 1 }
    val yamlCompletionState = remember(type, editor.text, editor.selection) {
        yamlCompletionState(type, editor)
    }
    val validation = remember(type, editor.text, context.resources.configuration.locales) {
        editorValidation(context, type, editor.text)
    }
    val syntaxPalette = CodeSyntaxPalette(
        key = MaterialTheme.colorScheme.primary,
        keyword = MaterialTheme.colorScheme.tertiary,
        string = MaterialTheme.colorScheme.secondary,
        number = MaterialTheme.colorScheme.error,
        comment = MaterialTheme.colorScheme.onSurfaceVariant,
        punctuation = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val title = if (type == TemplateType.JS) "JavaScript" else "YAML"
    val lineNumberWidth = when {
        lineCount < 100 -> 30.dp
        lineCount < 1000 -> 36.dp
        lineCount < 10000 -> 42.dp
        else -> 48.dp
    }
    val yamlCompletionHeight = (yamlCompletionState.suggestions.size.coerceAtMost(6) * 32 + 8).dp
    val yamlCompletionY = if (yamlCompletionOffsetY + yamlCompletionHeight > 400.dp) {
        (yamlCompletionOffsetY - yamlCompletionHeight - 20.dp).coerceAtLeast(8.dp)
    } else {
        yamlCompletionOffsetY
    }
    LaunchedEffect(revealCursorRequest) {
        if (revealCursorRequest == 0) return@LaunchedEffect
        delay(40)
        val rect = cursorRect ?: return@LaunchedEffect
        if (editorViewportHeight <= 0) return@LaunchedEffect
        val textTopPadding = with(density) { 12.dp.toPx() }
        val cursorTop = rect.top + textTopPadding
        val cursorBottom = rect.bottom + textTopPadding
        val topPadding = with(density) { 32.dp.toPx() }
        val bottomPadding = with(density) { 96.dp.toPx() }
        val visibleTop = verticalScroll.value.toFloat()
        val visibleBottom = visibleTop + editorViewportHeight
        val target = when {
            cursorBottom + bottomPadding > visibleBottom ->
                cursorBottom + bottomPadding - editorViewportHeight
            cursorTop - topPadding < visibleTop ->
                cursorTop - topPadding
            else -> null
        }
        if (target != null) {
            verticalScroll.scrollTo(target.toInt().coerceIn(0, verticalScroll.maxValue))
        }
    }

    Surface(
        modifier = modifier,
        shape = if (fullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = if (fullScreen) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(if (fullScreen) Modifier.fillMaxSize() else Modifier) {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(start = 10.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            l10nf("%d 行", lineCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (onRequestFullScreen != null) {
                            TextButton(
                                onClick = onRequestFullScreen,
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Text("全屏".l10n(), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            val editorAreaModifier = if (fullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            }
            Box(
                modifier = editorAreaModifier
                    .onSizeChanged { editorViewportHeight = it.height }
                    .verticalScroll(verticalScroll),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .width(lineNumberWidth)
                            .padding(top = 12.dp, end = 5.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        repeat(lineCount) { index ->
                            Text(
                                (index + 1).toString(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 17.sp,
                                ),
                            )
                        }
                    }
                    BasicTextField(
                        value = editor,
                        onValueChange = { next -> applyEditor(autoIndent(editor, next, type)) },
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        visualTransformation = CodeSyntaxVisualTransformation(type, syntaxPalette),
                        onTextLayout = { layoutResult ->
                            val cursor = editor.selection.start.coerceIn(0, editor.text.length)
                            val nextCursorRect = layoutResult.getCursorRect(cursor)
                            if (nextCursorRect != cursorRect) {
                                cursorRect = nextCursorRect
                            }
                            if (type == TemplateType.YAML) {
                                val nextOffset = with(density) { nextCursorRect.bottom.toDp() + 16.dp }
                                if (nextOffset != yamlCompletionOffsetY) {
                                    yamlCompletionOffsetY = nextOffset
                                }
                            }
                        },
                    )
                }
                if (yamlCompletionState.suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .offset(x = lineNumberWidth + 12.dp, y = yamlCompletionY)
                            .widthIn(min = 240.dp, max = 288.dp)
                            .zIndex(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                        ) {
                            yamlCompletionState.suggestions.forEach { completion ->
                                Text(
                                    completion.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            applyEditor(editor.applyYamlCompletion(yamlCompletionState, completion))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            if (type == TemplateType.YAML) {
                val validationErrorPrefix = l10nf("YAML 语法可能有误：%s", "").trim()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (validation.isNotBlank()) {
                        Text(
                            validation,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (validation.startsWith(validationErrorPrefix)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private data class CodeSyntaxPalette(
    val key: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val punctuation: Color,
)

private data class YamlCompletion(
    val label: String,
    val insertText: String = "$label,",
)

private data class YamlCompletionState(
    val tokenStart: Int = 0,
    val tokenEnd: Int = 0,
    val suggestions: List<YamlCompletion> = emptyList(),
)

private val RuleTypeCompletions = listOf(
    "DOMAIN",
    "DOMAIN-SUFFIX",
    "DOMAIN-KEYWORD",
    "DOMAIN-WILDCARD",
    "DOMAIN-REGEX",
    "GEOSITE",
    "GEOIP",
    "IP-CIDR",
    "IP-CIDR6",
    "IP-SUFFIX",
    "IP-ASN",
    "SRC-GEOIP",
    "SRC-IP-ASN",
    "SRC-IP-CIDR",
    "SRC-IP-SUFFIX",
    "DST-PORT",
    "SRC-PORT",
    "IN-PORT",
    "IN-TYPE",
    "IN-USER",
    "IN-NAME",
    "PROCESS-PATH",
    "PROCESS-PATH-WILDCARD",
    "PROCESS-PATH-REGEX",
    "PROCESS-NAME",
    "PROCESS-NAME-WILDCARD",
    "PROCESS-NAME-REGEX",
    "RULE-SET",
    "AND",
    "OR",
    "NOT",
    "SUB-RULE",
    "MATCH",
    "NETWORK",
    "UID",
    "DSCP",
).map { YamlCompletion(it) }

private val YamlBlockFieldCompletions = mapOf(
    "proxies" to listOf(
        "name",
        "type",
        "server",
        "port",
        "ip-version",
        "udp",
        "interface-name",
        "routing-mark",
        "tfo",
        "mptcp",
        "dialer-proxy",
        "servername",
        "alpn",
        "skip-cert-verify",
        "smux",
        "client-fingerprint",
        "network",
        "ws-opts",
        "grpc-opts",
        "reality-opts",
        "packet-addr",
        "udp-over-tcp",
        "headers",
        "cipher",
        "password",
        "uuid",
        "alterId",
        "flow",
        "plugin",
        "plugin-opts",
    ),
    "proxy-groups" to listOf(
        "name",
        "type",
        "proxies",
        "use",
        "url",
        "interval",
        "tolerance",
        "lazy",
        "filter",
        "exclude-filter",
        "include-all",
        "exclude-type",
        "expected-status",
        "disable-udp",
        "hidden",
        "icon",
    ),
    "proxy-providers" to listOf(
        "type",
        "url",
        "path",
        "interval",
        "filter",
        "exclude-filter",
        "health-check",
        "override",
        "header",
    ),
    "rule-providers" to listOf(
        "type",
        "behavior",
        "format",
        "url",
        "path",
        "interval",
        "payload",
    ),
    "dns" to listOf(
        "enable",
        "listen",
        "ipv6",
        "enhanced-mode",
        "fake-ip-range",
        "default-nameserver",
        "nameserver",
        "proxy-server-nameserver",
        "direct-nameserver",
        "fallback",
        "fallback-filter",
        "nameserver-policy",
        "fake-ip-filter",
        "use-hosts",
        "respect-rules",
    ),
    "tun" to listOf(
        "enable",
        "stack",
        "device",
        "auto-route",
        "auto-detect-interface",
        "strict-route",
        "dns-hijack",
        "mtu",
    ),
    "sniffer" to listOf(
        "enable",
        "override-destination",
        "force-dns-mapping",
        "parse-pure-ip",
        "sniff",
        "force-domain",
        "skip-domain",
    ),
).mapValues { (_, fields) -> fields.distinct().map { YamlCompletion(it, "$it: ") } }

private class CodeSyntaxVisualTransformation(
    private val type: String,
    private val palette: CodeSyntaxPalette,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlightCode(text.text, type, palette), OffsetMapping.Identity)
}

private fun highlightCode(text: String, type: String, palette: CodeSyntaxPalette): AnnotatedString =
    buildAnnotatedString {
        text.lineSequence().forEachIndexed { index, line ->
            if (index > 0) append('\n')
            if (type == TemplateType.JS) {
                appendJsLine(line, palette)
            } else {
                appendYamlLine(line, palette)
            }
        }
    }

private fun AnnotatedString.Builder.appendYamlLine(line: String, palette: CodeSyntaxPalette) {
    val commentIndex = line.indexOfCodeComment()
    val code = if (commentIndex >= 0) line.substring(0, commentIndex) else line
    val comment = if (commentIndex >= 0) line.substring(commentIndex) else ""
    val keyMatch = Regex("""^(\s*-\s*|\s*)(\+?[A-Za-z0-9_.-]+[+!]?)(\s*:)""").find(code)
    if (keyMatch != null) {
        append(keyMatch.groupValues[1])
        withStyle(SpanStyle(color = palette.key, fontWeight = FontWeight.SemiBold)) {
            append(keyMatch.groupValues[2])
        }
        withStyle(SpanStyle(color = palette.punctuation)) {
            append(keyMatch.groupValues[3])
        }
        appendValueSegments(code.substring(keyMatch.range.last + 1), palette, jsMode = false)
    } else {
        appendValueSegments(code, palette, jsMode = false)
    }
    if (comment.isNotEmpty()) {
        withStyle(SpanStyle(color = palette.comment)) {
            append(comment)
        }
    }
}

private fun AnnotatedString.Builder.appendJsLine(line: String, palette: CodeSyntaxPalette) {
    val commentIndex = line.indexOfJsComment()
    val code = if (commentIndex >= 0) line.substring(0, commentIndex) else line
    val comment = if (commentIndex >= 0) line.substring(commentIndex) else ""
    appendValueSegments(code, palette, jsMode = true)
    if (comment.isNotEmpty()) {
        withStyle(SpanStyle(color = palette.comment)) {
            append(comment)
        }
    }
}

private fun AnnotatedString.Builder.appendValueSegments(
    code: String,
    palette: CodeSyntaxPalette,
    jsMode: Boolean,
) {
    val regex = if (jsMode) JsSyntaxRegex else YamlValueRegex
    var last = 0
    regex.findAll(code).forEach { match ->
        if (match.range.first > last) append(code.substring(last, match.range.first))
        val token = match.value
        val color = when {
            token.firstOrNull() == '"' || token.firstOrNull() == '\'' || token.firstOrNull() == '`' -> palette.string
            token in setOf("{", "}", "[", "]", "(", ")", ",", ":") -> palette.punctuation
            token.equals("true", true) || token.equals("false", true) || token.equals("null", true) ||
                token.equals("yes", true) || token.equals("no", true) || token.equals("on", true) ||
                token.equals("off", true) || token == "undefined" -> palette.keyword
            token.firstOrNull()?.isDigit() == true || token.startsWith("-") -> palette.number
            else -> palette.keyword
        }
        withStyle(SpanStyle(color = color)) {
            append(token)
        }
        last = match.range.last + 1
    }
    if (last < code.length) append(code.substring(last))
}

private val YamlValueRegex = Regex(
    """"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|\b(?:true|false|null|yes|no|on|off)\b|-?\b\d+(?:\.\d+)?\b|[{}\[\],:]""",
    RegexOption.IGNORE_CASE,
)

private val JsSyntaxRegex = Regex(
    """`(?:\\.|[^`])*`|"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|\b(?:function|return|const|let|var|if|else|for|of|in|while|switch|case|break|continue|true|false|null|undefined|new)\b|-?\b\d+(?:\.\d+)?\b|[{}\[\](),:]""",
)

private fun String.indexOfCodeComment(): Int {
    var quote: Char? = null
    forEachIndexed { index, char ->
        if (quote != null) {
            if (char == quote && getOrNull(index - 1) != '\\') quote = null
            return@forEachIndexed
        }
        if (char == '"' || char == '\'') {
            quote = char
        } else if (char == '#') {
            return index
        }
    }
    return -1
}

private fun String.indexOfJsComment(): Int {
    var quote: Char? = null
    forEachIndexed { index, char ->
        if (quote != null) {
            if (char == quote && getOrNull(index - 1) != '\\') quote = null
            return@forEachIndexed
        }
        if (char == '"' || char == '\'' || char == '`') {
            quote = char
        } else if (char == '/' && getOrNull(index + 1) == '/') {
            return index
        }
    }
    return -1
}

private fun editorValidation(context: android.content.Context, type: String, text: String): String {
    if (text.isBlank()) return ""
    if (type != TemplateType.YAML) return ""
    return runCatching {
        org.yaml.snakeyaml.Yaml().load<Any?>(text)
    }.fold(
        onSuccess = { "" },
        onFailure = {
            AppI18n.format(
                context,
                "YAML 语法可能有误：%s",
                it.message.orEmpty().lineSequence().firstOrNull().orEmpty(),
            )
        },
    )
}

private fun yamlCompletionState(type: String, editor: TextFieldValue): YamlCompletionState {
    if (type != TemplateType.YAML || !editor.selection.collapsed) return YamlCompletionState()
    val cursor = editor.selection.start.coerceIn(0, editor.text.length)
    val lineStart = editor.text.lineStartBefore(cursor)
    val block = editor.text.yamlCompletionBlock(lineStart) ?: return YamlCompletionState()

    val lineToCursor = editor.text.substring(lineStart, cursor)
    if (block == "rules") {
        val match = Regex("""^(\s*-\s*)([A-Za-z-]{1,})$""").find(lineToCursor) ?: return YamlCompletionState()
        val prefix = match.groupValues[2]
        val tokenStart = lineStart + match.groups[2]!!.range.first
        val suggestions = RuleTypeCompletions.filter { it.label.startsWith(prefix, ignoreCase = true) }
        return YamlCompletionState(tokenStart, cursor, suggestions)
    }

    val match = Regex("""^(\s*(?:-\s*)?)([A-Za-z][A-Za-z0-9_.-]*)$""").find(lineToCursor)
        ?: return YamlCompletionState()
    val prefix = match.groupValues[2]
    val tokenStart = lineStart + match.groups[2]!!.range.first
    val suggestions = YamlBlockFieldCompletions[block].orEmpty()
        .filter { it.label.startsWith(prefix, ignoreCase = true) }
    return YamlCompletionState(tokenStart, cursor, suggestions)
}

private fun TextFieldValue.applyYamlCompletion(
    state: YamlCompletionState,
    completion: YamlCompletion,
): TextFieldValue {
    val start = state.tokenStart.coerceIn(0, text.length)
    val end = state.tokenEnd.coerceIn(start, text.length)
    val next = text.replaceRange(start, end, completion.insertText)
    val cursor = start + completion.insertText.length
    return TextFieldValue(next, TextRange(cursor))
}

private fun autoIndent(previous: TextFieldValue, next: TextFieldValue, type: String): TextFieldValue {
    if (!next.selection.collapsed) return next
    val cursor = next.selection.start
    if (next.text.length != previous.text.length + 1 || cursor == 0 || next.text.getOrNull(cursor - 1) != '\n') {
        return next
    }
    val previousCursor = previous.selection.start.coerceIn(0, previous.text.length)
    val lineStart = previous.text.lineStartBefore(previousCursor)
    val previousLine = previous.text.substring(lineStart, previousCursor)
    val baseIndent = previousLine.takeWhile { it == ' ' || it == '\t' }
    val extraIndent = when {
        type == TemplateType.YAML && previousLine.trimEnd().endsWith(":") -> "  "
        type == TemplateType.JS && previousLine.trimEnd().endsWith("{") -> "  "
        else -> ""
    }
    val insert = baseIndent + extraIndent
    val text = next.text.replaceRange(cursor, cursor, insert)
    val selection = TextRange(cursor + insert.length)
    return TextFieldValue(text, selection)
}

private fun String.lineStartBefore(position: Int): Int {
    if (position <= 0) return 0
    val index = lastIndexOf('\n', position - 1)
    return if (index < 0) 0 else index + 1
}

private fun String.yamlCompletionBlock(lineStart: Int): String? {
    val currentLineEnd = indexOf('\n', lineStart).let { if (it < 0) length else it }
    val currentLine = substring(lineStart, currentLineEnd)
    val currentIndent = currentLine.leadingIndentWidth()
    val currentIsListItem = currentLine.trimStart().startsWith("-")
    var namedMappingDepth = 0
    val lines = substring(0, lineStart).lineSequence().toList().asReversed()
    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEach
        val indent = line.leadingIndentWidth()
        val canContainCurrentLine = indent < currentIndent || (currentIsListItem && indent == currentIndent)
        if (!canContainCurrentLine) return@forEach
        val isListItem = trimmed.startsWith("-")
        val key = trimmed
            .removePrefix("-")
            .trim()
            .substringBefore(":", missingDelimiterValue = "")
            .trim()
            .cleanOverrideKey()
        if (key.isBlank() || ":" !in trimmed) return@forEach
        if (key == "rules") return if (namedMappingDepth == 0) key else null
        if (key in YamlBlockFieldCompletions) {
            return when (key) {
                "proxy-providers", "rule-providers" -> if (namedMappingDepth <= 1) key else null
                else -> if (namedMappingDepth == 0) key else null
            }
        }
        if (!isListItem) namedMappingDepth += 1
    }
    return null
}

private fun String.cleanOverrideKey(): String =
    trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trimStart('+')
        .trimEnd('+', '!')

private fun String.leadingIndentWidth(): Int =
    takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') 2 else 1 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverrideHelpScreen(
    onDismiss: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("覆写说明".l10n(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
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
                            "执行顺序".l10n(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "基础配置生成后，先应用全局覆写，再按输出配置里的顺序应用专属覆写。同一个覆写只会执行一次。同一输出中先合并所有 YAML 覆写，再按顺序执行 JavaScript 覆写。".l10n(),
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

            item {
                SectionHeader("JavaScript 覆写")
                iOSGroupedCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "入口为 main(config)，接收解析后的完整配置对象，返回修改后的对象即可。JavaScript 覆写在所有 YAML 覆写之后执行。脚本出错会中断该输出的渲染。".l10n(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OverrideHelpCodeBlock(
                    """
                    function main(config) {
                      // 在 rules 开头插入一条规则
                      config.rules.unshift("DOMAIN,google.com,DIRECT");
                      return config;
                    }
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
                            "请先添加订阅源".l10n(),
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
            "请先添加覆写".l10n(),
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
            "未选择专属覆写，将只应用全局覆写".l10n(),
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
    val context = LocalContext.current
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
                overrideStateText(context, overrideItem),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = localize("上移"), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = localize("下移"), modifier = Modifier.size(18.dp))
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
                            l10nf("%d / %d 个节点", matchCount, allNames.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
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
                Text("暂无节点数据".l10n(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("请先刷新订阅".l10n(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    l10nf("前缀: %s", source.prefix),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (source.includeRegex.isNotBlank()) {
                                Text(
                                    l10nf("保留: %s", source.includeRegex),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (source.excludeRegex.isNotBlank()) {
                                Text(
                                    l10nf("排除: %s", source.excludeRegex),
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

private const val COLLAPSE_THRESHOLD = 20

private data class TreeRow(
    val id: Int,
    val depth: Int,
    val key: String?,
    val value: String?,
    val childCount: Int,
    val isListItem: Boolean,
    val childIds: List<Int>,
) {
    val collapsible get() = childCount > 0
}

private fun parseYamlToTree(yamlBody: String): List<TreeRow> {
    val loaded = runCatching { org.yaml.snakeyaml.Yaml().load<Any?>(yamlBody) }.getOrNull()
        ?: return emptyList()
    val rows = mutableListOf<TreeRow>()
    buildRows(loaded, -1, false, null, rows)
    return rows
}

private fun buildRows(
    value: Any?,
    depth: Int,
    isListItem: Boolean,
    key: String?,
    rows: MutableList<TreeRow>,
) {
    val rowId = rows.size
    when (value) {
        is Map<*, *> -> {
            rows.add(TreeRow(rowId, depth, key, null, value.size, isListItem, emptyList()))
            if (value.isEmpty()) return
            val childIds = mutableListOf<Int>()
            value.forEach { (k, v) ->
                childIds += rows.size
                buildRows(v ?: "", depth + 1, false, k?.toString(), rows)
            }
            rows[rowId] = rows[rowId].copy(childIds = childIds)
        }
        is List<*> -> {
            rows.add(TreeRow(rowId, depth, key, null, value.size, isListItem, emptyList()))
            if (value.isEmpty()) return
            val childIds = mutableListOf<Int>()
            value.forEach { item ->
                childIds += rows.size
                buildRows(item ?: "", depth + 1, true, null, rows)
            }
            rows[rowId] = rows[rowId].copy(childIds = childIds)
        }
        else -> {
            rows.add(TreeRow(rowId, depth, key, value?.toString() ?: "null", 0, isListItem, emptyList()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputPreviewScreen(
    previewState: PreviewState,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }
    val yaml = (previewState as? PreviewState.Success)?.yaml
    val rows = remember(yaml) { yaml?.let(::parseYamlToTree).orEmpty() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val title = (previewState as? PreviewState.Success)?.profileTitle
                            ?: localize("完整配置预览")
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        val success = previewState as? PreviewState.Success
                        if (success != null) {
                            Text(
                                l10nf("%d 字符 · 实时渲染", success.yaml.length),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (rows.isNotEmpty()) {
                        IconButton(onClick = {
                            rows.forEach { if (it.collapsible) expanded[it.id] = true }
                        }) {
                            Icon(Icons.Default.UnfoldMore, contentDescription = localize("全部展开"))
                        }
                        IconButton(onClick = {
                            rows.forEach { if (it.collapsible) expanded[it.id] = false }
                        }) {
                            Icon(Icons.Default.UnfoldLess, contentDescription = localize("全部折叠"))
                        }
                        IconButton(onClick = {
                            yaml?.let { clipboard.setText(AnnotatedString(it)) }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = localize("复制"))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            when (previewState) {
                PreviewState.Idle, PreviewState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("正在渲染配置...".l10n(), style = MaterialTheme.typography.bodySmall)
                    }
                }

                is PreviewState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            "渲染失败".l10n(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            AppI18n.message(LocalContext.current, previewState.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is PreviewState.Success -> {
                    YamlTreeView(
                        rows = rows,
                        expanded = expanded,
                    )
                }
            }
        }
    }
}

@Composable
private fun YamlTreeView(
    rows: List<TreeRow>,
    expanded: SnapshotStateMap<Int, Boolean>,
) {
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("（空配置）".l10n(), style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val visibleRows by remember(rows) {
        derivedStateOf { computeVisibleRows(rows, expanded) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = visibleRows.size,
            key = { visibleRows[it].id },
        ) { index ->
            TreeRowView(visibleRows[index], expanded)
        }
    }
}

private fun computeVisibleRows(
    rows: List<TreeRow>,
    expanded: Map<Int, Boolean>,
): List<TreeRow> {
    if (rows.isEmpty()) return emptyList()
    val result = mutableListOf<TreeRow>()
    fun isExpanded(row: TreeRow): Boolean {
        if (!row.collapsible) return false
        return expanded[row.id] ?: (row.childCount <= COLLAPSE_THRESHOLD)
    }
    fun walk(row: TreeRow) {
        result.add(row)
        if (!isExpanded(row)) return
        row.childIds.forEach { childId -> walk(rows[childId]) }
    }
    val root = rows.first()
    if (root.collapsible) {
        root.childIds.forEach { walk(rows[it]) }
    } else {
        walk(root)
    }
    return result
}

@Composable
private fun TreeRowView(
    row: TreeRow,
    expanded: SnapshotStateMap<Int, Boolean>,
) {
    val isOpen = row.collapsible && (expanded[row.id] ?: (row.childCount <= COLLAPSE_THRESHOLD))
    val keyColor = MaterialTheme.colorScheme.primary
    val valueColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val childCountText = if (row.collapsible) l10nf(" %d 项", row.childCount) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp * (row.depth + 1).coerceAtLeast(0).toFloat())
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                if (row.collapsible) {
                    expanded[row.id] = !isOpen
                }
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.collapsible) {
            Icon(
                if (isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = mutedColor,
            )
        } else {
            Spacer(Modifier.width(14.dp))
        }
        val text = buildAnnotatedString {
            if (row.isListItem) {
                append("- ")
            }
            row.key?.let {
                withStyle(SpanStyle(color = keyColor, fontWeight = FontWeight.SemiBold)) {
                    append(it)
                }
                if (row.value != null) append(": ") else append(":")
            }
            if (row.collapsible) {
                withStyle(SpanStyle(color = mutedColor)) {
                    append(childCountText)
                }
            } else {
                row.value?.let {
                    withStyle(SpanStyle(color = valueColor)) {
                        append(it)
                    }
                }
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                Text(label.l10n(), style = MaterialTheme.typography.labelMedium)
            },
            placeholder = {
                Text(placeholder.l10n(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
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
                    label.l10n(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    selectedLabel.l10n(),
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
                    text = { Text(text.l10n(), style = MaterialTheme.typography.bodySmall) },
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
        text.l10n(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun RegexHint() {
    val firstDescription = localize("包含\"香港\"或\"台湾\"的节点")
    val secondDescription = localize("同时包含\"港\"和\"BGP\"")
    Column(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            "支持正则表达式，匹配节点名称。常见写法:".l10n(),
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
                    append(firstDescription)
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
                    append(secondDescription)
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
        preview.isCalculating -> localize("预览计算中...")
        preview.hasRegexError -> localize("预览: 正则表达式有误")
        else -> l10nf("预览: %d/%d 个节点匹配", preview.matchCount, nodeNames.size)
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
                    text = "正在计算预览...".l10n(),
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
                    l10nf("... 还有 %d 个节点", nodeNames.size - preview.items.size),
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
                        contentDescription = tab.title.l10n(),
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(tab.title.l10n(), style = MaterialTheme.typography.labelSmall)
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
    val context = LocalContext.current
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
                            "正在刷新...".l10n(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        sourceDnsLabel(context, source)?.let {
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
                            l10nf("到期 %s", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } ?: Text(
                        "到期未知".l10n(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    source.lastRefreshAt?.let {
                        Text(
                            l10nf("上次成功 %s", SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))),
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
                        AppI18n.message(context, it),
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
    val context = LocalContext.current
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
                        "${sourceNames(context, profile, sources)} · ${overrideSummary(context, profile, templates)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.fetchCount > 0) {
                        Text(
                            l10nf("已拉取 %d 次", profile.fetchCount),
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
    val context = LocalContext.current
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
                        overrideCardSubtitle(context, template),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = localize("上移"), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = localize("下移"), modifier = Modifier.size(18.dp))
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
    onCopied: () -> Unit,
    onQrShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var port by rememberSaveable(settings.port) { mutableStateOf(settings.port.toString()) }
    var token by rememberSaveable(settings.token) { mutableStateOf(settings.token) }
    var allowLan by rememberSaveable(settings.allowLan) { mutableStateOf(settings.allowLan) }
    var autoStartOnBoot by rememberSaveable(settings.autoStartOnBoot) { mutableStateOf(settings.autoStartOnBoot) }
    var globalUserAgent by rememberSaveable(settings.globalUserAgent) { mutableStateOf(settings.globalUserAgent) }
    var gistToken by rememberSaveable(settings.gistToken) { mutableStateOf(settings.gistToken) }
    val lanAddress = remember(allowLan) { if (allowLan) localLanAddress() else null }
    val allowLanDescription = if (allowLan) {
        l10nf("已开启，使用 %s 分享", lanAddress ?: localize("手机局域网 IP"))
    } else {
        localize("关闭时仅本机访问，开启后显示局域网地址")
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
            val zashboardUrl = zashboardUrl(previewSettings, lanAddress)
            ZashboardCard(
                url = zashboardUrl,
                running = running,
                onOpen = { uriHandler.openUri(zashboardUrl) },
                onCopy = {
                    clipboard.setText(AnnotatedString(zashboardUrl))
                    onCopied()
                },
                onQrShare = { onQrShare(zashboardUrl) },
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
                        "订阅拉取 User-Agent".l10n(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = globalUserAgent,
                        onValueChange = { globalUserAgent = it },
                        placeholder = {
                            Text(
                                "全局请求 UA".l10n(),
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
                        "拉取订阅时使用的默认 User-Agent".l10n(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        item {
            var gistTokenVisible by rememberSaveable { mutableStateOf(true) }
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
                                "ghp_xxx（需 gist 权限）".l10n(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (gistTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { gistTokenVisible = !gistTokenVisible }) {
                                Icon(
                                    imageVector = if (gistTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (gistTokenVisible) localize("隐藏") else localize("显示"),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
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
                        "上传配置到 Gist 用的个人访问令牌，需 gist 权限。留空则不开启上传。".l10n(),
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
                Text("保存配置".l10n(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ZashboardCard(
    url: String,
    running: Boolean,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onQrShare: () -> Unit,
) {
    iOSGroupedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                iOSTintedIcon(Icons.Default.Insights, MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "zashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (running) "静态面板已随 HTTP 服务开放".l10n() else "启动本地 HTTP 服务后可访问".l10n(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpen,
                    enabled = running,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("打开".l10n(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("复制".l10n(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onQrShare,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("二维码".l10n(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
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
                    "本地 HTTP 服务".l10n(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (running) "运行中".l10n() else "已停止".l10n(),
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
                Text(if (running) "停止".l10n() else "启动".l10n(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
            label.l10n(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder.l10n(),
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
            Text(label.l10n(), style = MaterialTheme.typography.bodySmall)
            subtitle?.let {
                Text(
                    it.l10n(),
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
            contentDescription = contentDescription.l10n(),
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
            "${label.l10n()}: ",
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
        Text(title.l10n(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle.l10n(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                title = { Text("扫描二维码".l10n(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
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
                Text("需要相机权限".l10n(), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("授予权限".l10n())
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
                title = { Text("二维码分享".l10n(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = localize("关闭"))
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

private fun trafficText(context: android.content.Context, source: SubscriptionSourceEntity): String {
    val total = source.totalBytes?.let(::formatBytes) ?: AppI18n.text(context, "未知")
    val used = listOfNotNull(source.uploadBytes, source.downloadBytes).takeIf { it.isNotEmpty() }?.sum()?.let(::formatBytes)
        ?: AppI18n.text(context, "未知")
    val remaining = source.totalBytes?.let { totalBytes ->
        val usedBytes = listOfNotNull(source.uploadBytes, source.downloadBytes).sum()
        formatBytes((totalBytes - usedBytes).coerceAtLeast(0))
    } ?: AppI18n.text(context, "未知")
    return AppI18n.format(context, "已用 %s / 剩余 %s / 总量 %s", used, remaining, total)
}

private fun sourceDnsLabel(context: android.content.Context, source: SubscriptionSourceEntity): String? {
    val protocol = DnsProtocol.fromStorage(source.dnsProtocol)
    val downloadDns = protocol?.let {
        val preset = PublicDnsPresets.all.firstOrNull { preset ->
            preset.protocol == it && preset.server.equals(source.dnsServer.trim(), ignoreCase = true)
        }
        val resolver = preset?.label?.let { label -> AppI18n.text(context, label) }
            ?: AppI18n.format(context, "自定义 %s", it.name)
        val mode = when (DnsConnectionMode.fromStorage(source.dnsConnectionMode)) {
            DnsConnectionMode.PRESERVE_DOMAIN -> AppI18n.text(context, "保留域名")
            DnsConnectionMode.IP_URL -> "IP URL"
        }
        "$resolver · $mode"
    }
    val nodeDns = if (source.preResolveNodes) {
        val total = source.nodeResolveSuccessCount + source.nodeResolveFailureCount
        AppI18n.format(context, "节点预解析 %d/%d", source.nodeResolveSuccessCount, total)
    } else {
        null
    }
    return listOfNotNull(downloadDns, nodeDns).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun overrideCardSubtitle(context: android.content.Context, template: TemplateEntity): String {
    val refreshTime = template.lastRefreshAt?.let {
        AppI18n.format(context, "上次成功 %s", SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it)))
    } ?: AppI18n.text(context, "未成功刷新")
    return listOf(
        overrideStateText(context, template),
        if (template.remoteUrl.isBlank()) {
            AppI18n.text(context, "本地覆写")
        } else {
            AppI18n.format(context, "远程覆写 · %s", refreshTime)
        },
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

private fun sourceNames(context: android.content.Context, profile: OutputProfileEntity, sources: List<SubscriptionSourceEntity>): String {
    val ids = parseIdList(profile.sourceIds)
    val names = ids.map { id -> sources.firstOrNull { it.id == id }?.name ?: "#$id" }
    return AppI18n.format(context, "订阅源: %s", names.joinToString("、"))
}

private fun overrideSummary(context: android.content.Context, profile: OutputProfileEntity, overrides: List<TemplateEntity>): String {
    val globalCount = overrides.count { it.enabled && it.global }
    val selectedNames = parseIdList(profile.overrideIds)
        .mapNotNull { id -> overrides.firstOrNull { it.id == id }?.name }

    val parts = mutableListOf<String>()
    if (globalCount > 0) {
        parts += AppI18n.format(context, "全局覆写 %d 个", globalCount)
    }
    parts += if (selectedNames.isEmpty()) {
        AppI18n.text(context, "专属覆写: 无")
    } else {
        AppI18n.format(context, "专属覆写: %s", selectedNames.joinToString("、"))
    }
    return parts.joinToString(" · ")
}

private fun overrideStateText(context: android.content.Context, template: TemplateEntity): String =
    listOfNotNull(
        if (template.type == TemplateType.JS) "JS" else "YAML",
        if (template.enabled) AppI18n.text(context, "启用") else AppI18n.text(context, "停用"),
        if (template.global) AppI18n.text(context, "全局") else null,
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

private fun zashboardUrl(settings: ServerSettings, lanAddress: String?): String {
    val host = if (settings.allowLan) lanAddress ?: "PHONE_IP" else "127.0.0.1"
    return "http://$host:${settings.port}/zashboard/"
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
