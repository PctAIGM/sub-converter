package com.subconverter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.subconverter.domain.MihomoRuleTypes
import com.subconverter.domain.RuleLine
import com.subconverter.domain.RuleOverrideService
import com.subconverter.i18n.AppI18n

@Composable
fun RulesEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val service = remember { RuleOverrideService() }
    val rules = remember(value) { service.parseBody(value).ifEmpty { listOf(RuleLine()) } }

    fun emit(next: List<RuleLine>) = onValueChange(service.serializeBody(next))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        AppI18n.text(context, "规则列表"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        AppI18n.text(context, "按当前顺序插入到最终 rules 列表开头"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(
                    onClick = { emit(rules + RuleLine()) },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(AppI18n.text(context, "添加规则"))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RuleHeader(AppI18n.text(context, "类型"), Modifier.weight(5f))
                RuleHeader(AppI18n.text(context, "内容"), Modifier.weight(6f))
                RuleHeader(AppI18n.text(context, "目标"), Modifier.weight(4f))
                Box(Modifier.size(44.dp))
            }

            rules.forEachIndexed { index, rule ->
                RuleEditorRow(
                    rule = rule,
                    canMoveUp = index > 0,
                    canMoveDown = index < rules.lastIndex,
                    onChange = { next -> emit(rules.toMutableList().apply { this[index] = next }) },
                    onMove = { offset ->
                        val target = index + offset
                        if (target in rules.indices) {
                            emit(rules.toMutableList().apply { add(target, removeAt(index)) })
                        }
                    },
                    onDelete = {
                        emit(
                            if (rules.size == 1) listOf(RuleLine())
                            else rules.toMutableList().apply { removeAt(index) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RuleEditorRow(
    rule: RuleLine,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (RuleLine) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var actionsExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    var showExtra by remember { mutableStateOf(rule.extra.isNotBlank()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RuleTypeField(
                value = rule.type,
                modifier = Modifier.weight(5f),
                onValueChange = { type ->
                    onChange(
                        if (MihomoRuleTypes.isMatchOnly(type)) rule.copy(type = type, payload = "")
                        else rule.copy(type = type),
                    )
                },
            )
            CompactRuleTextField(
                value = rule.payload,
                onValueChange = { onChange(rule.copy(payload = it)) },
                modifier = Modifier.weight(6f),
                contentDescription = AppI18n.text(context, "规则内容"),
                placeholder = if (MihomoRuleTypes.isMatchOnly(rule.type)) "—" else "google.com",
                enabled = !MihomoRuleTypes.isMatchOnly(rule.type),
            )
            TargetField(
                value = rule.target,
                modifier = Modifier.weight(4f),
                onValueChange = { onChange(rule.copy(target = it)) },
            )
            Box {
                IconButton(
                    onClick = { actionsExpanded = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Default.MoreVert, AppI18n.text(context, "更多操作"))
                }
                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "选择规则类型")) },
                        onClick = { actionsExpanded = false; typeExpanded = true },
                    )
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "选择规则目标")) },
                        onClick = { actionsExpanded = false; targetExpanded = true },
                    )
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "上移")) },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) },
                        enabled = canMoveUp,
                        onClick = { actionsExpanded = false; onMove(-1) },
                    )
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "下移")) },
                        leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                        enabled = canMoveDown,
                        onClick = { actionsExpanded = false; onMove(1) },
                    )
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "附加参数")) },
                        onClick = { actionsExpanded = false; showExtra = !showExtra },
                    )
                    DropdownMenuItem(
                        text = { Text(AppI18n.text(context, "删除"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { actionsExpanded = false; onDelete() },
                    )
                }
                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
                ) {
                    MihomoRuleTypes.all.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                typeExpanded = false
                                onChange(
                                    if (MihomoRuleTypes.isMatchOnly(type)) rule.copy(type = type, payload = "")
                                    else rule.copy(type = type),
                                )
                            },
                        )
                    }
                }
                DropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false },
                ) {
                    MihomoRuleTypes.commonTargets.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(target, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                targetExpanded = false
                                onChange(rule.copy(target = target))
                            },
                        )
                    }
                }
            }
        }
        if (showExtra || rule.extra.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    AppI18n.text(context, "附加参数"),
                    modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CompactRuleTextField(
                    value = rule.extra,
                    onValueChange = { onChange(rule.copy(extra = it)) },
                    modifier = Modifier.weight(1f),
                    contentDescription = AppI18n.text(context, "附加参数（可选）"),
                    placeholder = "no-resolve",
                )
            }
        }
    }
}

@Composable
private fun RuleTypeField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    var suggestionsEnabled by remember { mutableStateOf(false) }
    val suggestions = remember(value) { MihomoRuleTypes.filter(value) }
    Box(modifier) {
        CompactRuleTextField(
            value = value,
            onValueChange = {
                suggestionsEnabled = true
                onValueChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = AppI18n.text(context, "规则类型"),
            placeholder = "DOMAIN-SUFFIX",
            onFocusChange = { focused = it },
        )
        DropdownMenu(
            expanded = focused && suggestionsEnabled && value.isNotBlank() && suggestions.isNotEmpty(),
            onDismissRequest = { suggestionsEnabled = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
            properties = PopupProperties(focusable = false),
        ) {
            suggestions.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type, fontFamily = FontFamily.Monospace) },
                    onClick = {
                        suggestionsEnabled = false
                        onValueChange(type)
                    },
                )
            }
        }
    }
}

@Composable
private fun TargetField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    var suggestionsEnabled by remember { mutableStateOf(false) }
    val suggestions = remember(value) {
        MihomoRuleTypes.commonTargets.filter { value.isBlank() || it.contains(value, ignoreCase = true) }
    }
    Box(modifier) {
        CompactRuleTextField(
            value = value,
            onValueChange = {
                suggestionsEnabled = true
                onValueChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = AppI18n.text(context, "规则目标"),
            placeholder = "DIRECT",
            onFocusChange = { focused = it },
        )
        DropdownMenu(
            expanded = focused && suggestionsEnabled && value.isNotBlank() && suggestions.isNotEmpty(),
            onDismissRequest = { suggestionsEnabled = false },
            properties = PopupProperties(focusable = false),
        ) {
            suggestions.forEach { target ->
                DropdownMenuItem(
                    text = { Text(target, fontFamily = FontFamily.Monospace) },
                    onClick = {
                        suggestionsEnabled = false
                        onValueChange(target)
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactRuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    contentDescription: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    var editor by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (value != editor.text) {
            val cursor = editor.selection.end.coerceIn(0, value.length)
            editor = TextFieldValue(value, selection = TextRange(cursor))
        }
    }
    Surface(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 1.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = editor,
                onValueChange = { next ->
                    val textChanged = next.text != editor.text
                    editor = next
                    if (textChanged) onValueChange(next.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChange?.invoke(it.isFocused) }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown ||
                            event.isShiftPressed || event.isCtrlPressed || event.isAltPressed
                        ) {
                            return@onPreviewKeyEvent false
                        }
                        val selection = editor.selection
                        val cursor = when (event.key) {
                            Key.DirectionRight -> if (selection.collapsed) {
                                (selection.end + 1).coerceAtMost(editor.text.length)
                            } else {
                                maxOf(selection.start, selection.end)
                            }
                            Key.DirectionLeft -> if (selection.collapsed) {
                                (selection.start - 1).coerceAtLeast(0)
                            } else {
                                minOf(selection.start, selection.end)
                            }
                            else -> return@onPreviewKeyEvent false
                        }
                        editor = editor.copy(selection = TextRange(cursor))
                        true
                    }
                    .padding(horizontal = 8.dp),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (editor.text.isEmpty()) {
                            Text(
                                placeholder,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun RuleHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
    )
}
