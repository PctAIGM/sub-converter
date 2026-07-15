# Windows 托盘 + 开机启动 + 视觉美化方案

## 一、托盘最小化（可选）

### 新增文件 `lib/core/tray_service.dart`
封装 tray_manager + window_manager，管理托盘生命周期：
- `init()`：创建托盘图标（复用 `windows/runner/resources/app_icon.ico`），菜单项「显示窗口 / 启动服务 / 停止服务 / 退出」
- `showWindow()` / `hideWindow()`：`windowManager.show()/hide()`
- `onTrayIconClick`：双击/单击托盘图标 → 显示窗口
- `dispose()`：清理

### 改 `lib/main.dart`
- `main()`：`WidgetsFlutterBinding.ensureInitialized()` → `windowManager.ensureInitialized()` → `windowManager.setPreventClose(true)`
- `SubConverterApp` 改 StatefulWidget + `WindowListener`
- `onWindowClose`：读设置 `minimizeToTray`
  - true：`windowManager.hide()`（进托盘）
  - false：`windowManager.destroy()`（真退出）
- 首次关闭时弹一次确认对话框（"将最小化到托盘，不再提示"），用 shared_preferences 记 `tray_prompt_shown`
- `initState` 初始化 tray_service，若命令行带 `--minimized`（开机启动传入）则 `hide()`

### 改 `windows/runner/main.cpp:33`
`SetQuitOnClose(true)` → `false`（配合 setPreventClose）

### 改 `lib/ui/screens/server_screen.dart`
- "开机自启动"开关下方加「关闭时最小化到托盘」开关（仅 Windows 显示）
- 存 ServerSettings 新增字段 `minimizeToTray`

## 二、开机启动配置（启动后默认进托盘）

### 改 `lib/main.dart`
- 启动参数检测：若带 `--minimized` → 启动后 `windowManager.hide()` + 服务自启
- `LaunchAtStartup.setup(appName: 'SubConverter', args: ['--minimized'])` 初始化

### 改 `lib/ui/screens/server_screen.dart:96-103`
"开机自启动"开关 `onChanged`：
- true：`await LaunchAtStartup.enable()` + 保存设置
- false：`await LaunchAtStartup.disable()` + 保存设置
- `initState` 时同步开关状态：`LaunchAtStartup.isEnabled`

## 三、视觉美化（macOS 简洁白 + 系统原生字体）

### 改 `lib/main.dart` ThemeData（核心）
```dart
ThemeData(
  useMaterial3: true,
  // 白底为主
  scaffoldBackgroundColor: Colors.white, // 或 #FAFAFA
  colorScheme: ColorScheme.fromSeed(
    seedColor: Color(0xFF007AFF), // macOS 系统蓝
    brightness: Brightness.light,
  ),
  // 系统字体回退链
  fontFamily: 'Segoe UI', // Windows 原生
  // AppBar：透明/白底、无阴影、居中标题、细底边
  appBarTheme: AppBarTheme(centerTitle: false, elevation: 0, backgroundColor: white,
      surfaceTintColor: Colors.transparent, titleTextStyle: ...),
  // Card：小圆角(10)、微阴影、白底
  cardTheme: CardTheme(elevation: 0.5, shape: RoundedRectangleBorder(borderRadius: 12),
      color: white, surfaceTintColor: Colors.transparent, margin: ...),
  // 分隔线更细更浅
  dividerTheme: DividerThemeData(thickness: 0.5, color: #E5E5EA),
  // 圆角统一
  ...
)
```

### 统一卡片/表单样式
- `form_fields.dart`：GroupedCard 底色用浅灰 #F5F5F7（macOS 分组卡），圆角 10，分隔线 #E5E5EA
- `source_card.dart` / `output_card.dart`：去掉硬编码 `withValues(alpha:0.3)`，改用语义化的浅色容器（如 blueGrey.shade50）
- 抽取 `lib/ui/theme/app_colors.dart` 常量类（如 `groupedBg`、`urlBlockBg`、`dividerColor`）替换散落的魔法数字

### 字体
Windows 自动用 Segoe UI（系统字体），中英文显示正常。无需打包字体。pubspec 不改。

## 四、托盘图标
- 临时方案：复制 `windows/runner/resources/app_icon.ico` 供 tray_manager 用
- 后续可替换为正式 logo

## 改动文件清单
| 文件 | 改动 |
|------|------|
| `lib/main.dart` | 主题重构 + window_manager 初始化 + 托盘接入 + 命令行检测 |
| `lib/core/tray_service.dart` | 新增：托盘管理封装 |
| `lib/ui/screens/server_screen.dart` | 开机启动接入 launch_at_startup + 最小化到托盘开关 |
| `lib/data/settings/server_settings.dart` | 新增 `minimizeToTray` 字段 |
| `lib/ui/theme/app_colors.dart` | 新增：颜色常量 |
| `lib/ui/widgets/form_fields.dart` | GroupedCard/FieldDivider 用新配色 |
| `lib/ui/widgets/source_card.dart` | 去硬编码 alpha，用 app_colors |
| `lib/ui/widgets/output_card.dart` | 去硬编码 alpha，用 app_colors |
| `windows/runner/main.cpp` | `SetQuitOnClose(false)` |

## 验证
- `flutter test` 85 测试仍全过
- `flutter build windows --debug` 构建成功
- 运行验证：关闭→进托盘、开机启动、白底简洁界面