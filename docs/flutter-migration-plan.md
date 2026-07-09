# SubConverter Flutter 迁移执行手册

> 目标：将现有 Android (Kotlin/Compose) 单端应用迁移至 **Flutter**，覆盖 **Android / iOS / Windows** 三平台。
> 现状规模：29 个 Kotlin 文件，8105 行（其中 UI 4113 行）。
> 本手册是迁移期间的执行依据，每个模块都给出"现状 → Flutter 方案 → 对照表 → 风险"。
>
> ⚠️ **本文件是规划文档（说"计划做什么"）。实际进度（说"做了什么、剩什么"）见：
> [`../../sub-converter-flutter/PROGRESS.md`](../../sub-converter-flutter/PROGRESS.md)**
> 迁移已开始，domain/数据层/Windows server/核心 UI 已完成（85 测试过，完成度约 55%）。
> 新会话请**先读 PROGRESS.md，再读本手册**。

---

## 0. 三平台功能矩阵（已确认）

| 能力 | Android | Windows | iOS |
|------|:-------:|:-------:|:---:|
| 订阅源管理/编辑/预览 | ✅ | ✅ | ✅ |
| 订阅转换（解析/YAML/覆写） | ✅ | ✅ | ✅ 前台触发 |
| Gist 上传 | ✅ | ✅ | ✅ **主要输出** |
| 本地 HTTP Server | ✅ 前台 Service | ✅ 进程长驻 | ❌ 不做 |
| 开机自启 | ✅ | ✅ 启动项 | ❌ |
| 定时自动刷新 | ✅ WorkManager | ✅ 进程内定时 | ⚠️ 前台/受限 |
| 扫码 | ✅ | ⚠️ 桌面少用 | ✅ |
| zashboard 静态面板 | ✅ 随 HTTP | ✅ 随 HTTP | ❌ |

**iOS 定位**：转换后上传 Gist，用户把 Gist raw URL 填进 Mihomo 客户端。一次配置长期生效（客户端定期拉），彻底绕开 iOS 后台限制。**raw URL 必须持久化、可查看、可复制**（见 §3.3 `gistRawUrl` 字段 + §15 iOS 验收）。

---

## 1. 技术选型

### 1.1 核心依赖对照

| 现状 (Kotlin) | Flutter 方案 | pub 包 | 成熟度 | 说明 |
|---------------|-------------|--------|:------:|------|
| Jetpack Compose | Flutter Widget | 内置 | ⭐⭐⭐⭐⭐ | 逐屏翻译 |
| StateFlow + collectAsState | Riverpod | `flutter_riverpod` | ⭐⭐⭐⭐⭐ | 状态管理首选 |
| Coroutines + viewModelScope | `async`/`Future` + `Provider` | 内置 + riverpod | ⭐⭐⭐⭐⭐ | |
| Room (SQLite, 10 版) | drift | `drift`, `drift_flutter` | ⭐⭐⭐⭐ | 类型安全，最接近 Room |
| DataStore Preferences | shared_preferences | `shared_preferences` | ⭐⭐⭐⭐⭐ | ServerSettings 这类简单 KV |
| OkHttp | dio | `dio` | ⭐⭐⭐⭐⭐ | 拦截器丰富 |
| SnakeYAML（解析） | yaml | `yaml` | ⭐⭐⭐⭐ | 只解析，**不做输出**（见 §6） |
| SnakeYAML（输出 dump） | yaml_writer | `yaml_writer` | ⭐⭐⭐ | 主方案；POC 验证 Mihomo 可加载 |
| QuickJS (`quickjs-android`) | flutter_js | `flutter_js` | ⭐⭐⭐ | 仍活跃（v0.8.7，~5 月前更新）；iOS=JavaScriptCore, Android=QuickJS，**两引擎 ES6+ 支持有差异（见 R5）** |
| ZXing + CameraX | mobile_scanner | `mobile_scanner` | ⭐⭐⭐⭐ | QR 扫描统一 |
| zxing core (生成二维码) | qr_flutter | `qr_flutter` | ⭐⭐⭐⭐ | 生成二维码 |
| WorkManager | workmanager | `workmanager` | ⭐⭐⭐ | iOS 受限，正好 iOS 不依赖 |
| EncryptedDns (DoH/DoT) | 自实现 + dio | — | ⚠️ | **见 §9：最大技术风险** |
| startForeground Service | 原生 Kotlin (保留) | — | — | Android 不可逃避 |
| BootStartupReceiver | 原生 Kotlin (保留) | — | — | Android 不可逃避 |
| android.util.Locale | flutter_localizations | 内置 | ⭐⭐⭐⭐ | i18n |

### 1.2 项目结构（Flutter 工程骨架）

```
sub_converter/                          # Flutter 工程根（建议新建仓库或新分支）
├── lib/
│   ├── main.dart
│   ├── app.dart                        # MaterialApp / 三平台路由
│   ├── core/
│   │   ├── container.dart              # 对应 AppContainer.kt (DI, Riverpod)
│   │   └── platform.dart               # Platform.isXxx 判断 + 抽象
│   ├── data/
│   │   ├── database.dart               # drift Database
│   │   ├── tables.dart                 # 对应 Entities.kt
│   │   ├── daos.dart                   # 对应 Daos.kt
│   │   └── settings/
│   │       └── server_settings.dart    # 对应 ServerSettingsStore.kt
│   ├── domain/
│   │   ├── models.dart                 # 对应 Models.kt
│   │   ├── share_link_parser.dart      # 对应 ShareLinkParser.kt
│   │   ├── mihomo_yaml_service.dart    # 对应 MihomoYamlService.kt
│   │   ├── js_override_service.dart    # 对应 JsOverrideService.kt
│   │   ├── subscription_fetcher.dart   # 对应 SubscriptionFetcher.kt
│   │   ├── subscription_dns.dart       # 对应 SubscriptionDns.kt
│   │   ├── encrypted_dns.dart          # 对应 EncryptedDns.kt  ⚠️ §9
│   │   ├── node_pre_resolver.dart      # 对应 NodePreResolver.kt
│   │   ├── gist_uploader.dart          # 对应 GistUploader.kt
│   │   ├── remote_text_fetcher.dart    # 对应 RemoteTextFetcher.kt
│   │   ├── subscription_repository.dart# 对应 SubscriptionRepository.kt
│   │   ├── output_repository.dart      # 对应 OutputRepository.kt
│   │   ├── default_templates.dart      # 对应 DefaultTemplates.kt
│   │   └── refresh_worker.dart         # 对应 RefreshWorker.kt + RefreshScheduler
│   ├── server/
│   │   ├── local_http_server.dart      # Dart HttpServer (Windows 主力)
│   │   └── zashboard_assets.dart       # 对应 ZashboardAssets
│   ├── i18n/
│   │   ├── app_i18n.dart               # 对应 AppI18n.kt
│   │   └── strings.dart                # 从 AppI18n.english map 迁移
│   └── ui/
│       ├── main_screen.dart            # 对应 MainScreen.kt 主体
│       ├── screens/
│       │   ├── sources_screen.dart     # SourcesScreen + SourceCard
│       │   ├── outputs_screen.dart     # OutputsScreen + OutputCard
│       │   ├── templates_screen.dart   # TemplatesScreen + TemplateCard
│       │   ├── server_screen.dart      # ServerScreen (Android/Windows)
│       │   ├── source_edit_screen.dart
│       │   ├── template_edit_screen.dart
│       │   ├── output_edit_screen.dart
│       │   ├── code_editor.dart        # FullScreenCodeEditor + 语法高亮
│       │   ├── node_preview_screen.dart
│       │   ├── output_preview_screen.dart  # YAML 树形预览
│       │   ├── override_help_screen.dart
│       │   ├── qr_scan_screen.dart     # mobile_scanner
│       │   └── qr_share_screen.dart    # qr_flutter
│       ├── widgets/
│       │   ├── ios_form_field.dart     # iOSXxx 系列 helper
│       │   ├── ios_grouped_card.dart
│       │   ├── navigation_bar.dart
│       │   └── ...
│       └── theme/
│           └── theme.dart              # 对应 Theme.kt
├── android/                            # 含保留的原生 Service/Receiver
│   └── app/src/main/kotlin/.../server/
│       ├── LocalHttpServerService.kt   # 保留（前台服务通知/START_STICKY，业务转交 Dart engine）
│       ├── BootStartupReceiver.kt      # 保留（开机自启 + 原生侧设置迁移 §4.1）
│       └── LegacySettingsMigrator.kt   # 新增（原生侧读旧 DataStore，§4.1）
│   # 注：Plan A 不保留 LocalHttpServer.kt（原始 socket），HTTP 业务由 Dart engine 执行。
│   # 仅 Plan B（§17）才保留 LocalHttpServer.kt。
├── ios/                                # Flutter 标准
├── windows/                            # Flutter 标准
├── assets/zashboard/                   # zashboard 静态资源
└── test/
    └── domain/                         # 解析逻辑回归测试（重要）
```

---

## 2. 分阶段执行计划

### 阶段 0：准备 + Android 常驻 Server POC（4-6 天）+ 决策窗口（2-3 天）
- [ ] 新建 Flutter 工程（`flutter create --platforms=android,ios,windows`）
- [ ] 在新分支 `flutter-rewrite` 上进行，旧 Kotlin 代码作为参照常开
- [ ] 配置 Riverpod、drift、dio 等核心依赖
- [ ] 把 `assets/zashboard/` 从旧工程复制过来
- [ ] **Android 常驻 Server POC**（见 §7.2 验证清单）：
  - Kotlin 前台服务启动 headless FlutterEngine，开机/无 UI 可响应 `/health`
  - 实测冷启动耗时（<8 秒为通过）
  - 双 isolate drift WAL 并发 + `fetchCount` 跨连接可见性
- [ ] **决策窗口（单列，不并入后续阶段）**：POC 跑完后留 2-3 天做 go/no-go 决策——通过则进阶段 1；不通过则评估 Plan B（§17）或 KMP，**这段时间不计入开发工期，但会阻塞所有后续阶段**。

### 阶段 1：数据层（3-4 天）
- [ ] drift tables（对应 Entities.kt）—— 见 §3
- [ ] drift DAO（对应 Daos.kt）—— 见 §3
- [ ] ServerSettingsStore（shared_preferences）—— 见 §4
- [ ] **列名对齐硬验收（§3.2 注②）**：跑通 `ATTACH 旧库 + INSERT INTO ... SELECT`，校验所有列值正确；全局 camelCase 不生效则降级逐列 `.named(...)`
- [ ] **回归测试**：用一份真实 `sub_converter.db` 导入 drift，验证数据可读

### 阶段 2：Domain 层（1 周）⚠️ 关键
按依赖顺序逐文件翻译，每个文件配单元测试：
1. `models.dart`（最简单，先做）
2. `share_link_parser.dart` + **回归测试**（见 §5）
3. `default_templates.dart`
4. `js_override_service.dart`（flutter_js）+ **三平台 JS 一致性验证**：翻译完立即在 Android/iOS/Windows 各跑一遍现有 JS 覆写用例（不要推到联调）。重点：现有覆写脚本是否用了 JSCore 支持滞后的 ES6+ 特性（`BigInt`、`Array.prototype.at`、`Object.hasOwn`、顶层 `await` 等），若有则在两引擎行为不一致处加 polyfill 或限制特性集。
5. `mihomo_yaml_service.dart` + **YAML round-trip 测试**（见 §6）
6. `subscription_dns.dart`
7. `encrypted_dns.dart` ⚠️ §9
8. `node_pre_resolver.dart`
9. `remote_text_fetcher.dart` + `subscription_fetcher.dart`（dio）
10. `gist_uploader.dart`（dio + GitHub API）
11. `subscription_repository.dart` + `output_repository.dart`

### 阶段 3：HTTP Server（Android/Windows 双路径，5-7 天）
- [ ] `local_http_server.dart`：Dart `HttpServer` 实现，对应 `LocalHttpServer.kt` 路由
- [ ] Windows 端先跑通（纯 Dart，无原生）
- [ ] Android 端：原生 `LocalHttpServerService` 负责前台服务/开机自启，服务内启动 headless FlutterEngine 执行 Dart HTTP Server（见 §7.2）
- [ ] Android 端：验证锁屏、后台、开机后无 UI 的情况下 `/health` 和 `/subscriptions/{id}.yaml` 可访问

### 阶段 4：UI 层（1.5-2 周）⭐ 最大块
- [ ] i18n 迁移（见 §8）
- [ ] theme 迁移
- [ ] 公共 widget（iOSXxx 系列 helper）
- [ ] 按屏幕逐个翻译（见 §10 屏幕-函数对照表）
- [ ] **Windows 桌面端先行验证**（最快，无原生包袱）

### 阶段 5：平台特性（1 周）
- [ ] Android：前台服务控制、headless FlutterEngine 生命周期、开机自启、权限
- [ ] Windows：开机自启（注册表/启动文件夹）
- [ ] iOS：扫码、前台转换、Gist 上传（无后台）
- [ ] WorkManager 定时刷新（Android）

### 阶段 6：联调与打包（1 周）
- [ ] 三平台冒烟测试
- [ ] iOS App Store / Google Play / Windows 打包配置
- [ ] 旧 Room 数据迁移工具（Android 端：读旧 db → 写 drift）
- [ ] 旧 DataStore 设置迁移（见 §4.1）

**总计：约 7.5-9 周（1 人有经验）**，构成：
- 阶段 0 POC + 决策窗口：4-6 天 POC + 2-3 天决策
- 阶段 1-2 数据+domain：1.5-2 周（含 YAML 输出选型 POC、DNS POC）
- 阶段 3 HTTP Server：5-7 天（Android headless engine 是大头）
- 阶段 4 UI：1.5-2 周
- 阶段 5-6 平台特性 + 打包：2 周

> 相比 v3（6.5-7.5 周）上调原因：YAML 输出从"用 yaml 包 dump"改为 yaml_writer POC + 可能自写 emitter；drift 列名/时间戳对齐新增工作量；DataStore 设置迁移新增；POC 决策窗口单列。若 POC 失败转 Plan B，工期再 +2-3 周。
>
> ⚠️ **7.5-9 周是"POC 全过、无返工"的理想路径**。建议预留 **30-50% buffer**，对外承诺按 **10-13 周** 报。理由（见 R9）：三个 POC（R3/R1/R2）可能需迭代而非一次通过；R4 代码编辑器若降级到纯 TextField 会牺牲功能、若自写高亮会显著加期；Android 低端机双 engine 内存问题需真机调试暴露。

---

## 3. 数据库迁移（Room → drift）

### 3.1 表对照

Room 版本已到 **10**。drift 重建时**直接以 v10 schema 为目标**（Flutter 是新 app，新装机直接建 v10；旧 Android 用户走迁移工具，见 §3.4）。

| Room Entity | drift Table |
|-------------|-------------|
| `SubscriptionSourceEntity` | `SubscriptionSources` |
| `NodeDnsCacheEntity` (复合主键) | `NodeDnsCaches` |
| `TemplateEntity` | `Templates` |
| `OutputProfileEntity` | `OutputProfiles` |

### 3.2 SubscriptionSources 完整字段（来自 Entities.kt:8-36）

drift 定义示例（对应 `subscription_sources`）：

```dart
class SubscriptionSources extends Table {
  IntColumn get id => integer().autoIncrement()();   // 对应 Room autoGenerate
  TextColumn get name => text()();
  TextColumn get url => text()();
  TextColumn get website => text().withDefault(const Constant(''))();
  TextColumn get userAgent => text().withDefault(const Constant('ClashforWindows/0.20.39'))();
  BoolColumn get enabled => boolean().withDefault(const Constant(true))();
  BoolColumn get autoRefreshEnabled => boolean().withDefault(const Constant(false))();
  IntColumn get refreshIntervalMinutes => integer().withDefault(const Constant(720))();
  TextColumn get prefix => text().withDefault(const Constant(''))();
  TextColumn get includeRegex => text().withDefault(const Constant(''))();
  TextColumn get excludeRegex => text().withDefault(const Constant(''))();
  TextColumn get cachedYaml => text().withDefault(const Constant(''))();
  IntColumn get lastRefreshAt => integer().nullable()();   // epoch millis，见下注①
  IntColumn get lastStatusCode => integer().nullable()();
  TextColumn get lastError => text().withDefault(const Constant(''))();
  IntColumn get uploadBytes => integer().nullable()();
  IntColumn get downloadBytes => integer().nullable()();
  IntColumn get totalBytes => integer().nullable()();
  IntColumn get expireAtSeconds => integer().nullable()();
  TextColumn get dnsProtocol => text().withDefault(const Constant(''))();
  TextColumn get dnsServer => text().withDefault(const Constant(''))();
  TextColumn get dnsConnectionMode => text().withDefault(const Constant('PRESERVE_DOMAIN'))();
  BoolColumn get allowHostnameMismatch => boolean().withDefault(const Constant(false))();
  BoolColumn get preResolveNodes => boolean().withDefault(const Constant(false))();
  IntColumn get nodeResolveSuccessCount => integer().withDefault(const Constant(0))();
  IntColumn get nodeResolveFailureCount => integer().withDefault(const Constant(0))();

  // 注意：不要 override primaryKey。autoIncrement() 已自动把 id 设为单列主键，
  // 再写 @override Set<Column> get primaryKey => {id} 会冲突。
  // 复合主键（如 NodeDnsCaches）才需要 override primaryKey。
}
```

> 注① **时间戳必须用 `IntColumn` 存 epoch 毫秒，不能用 `DateTimeColumn`**。drift 的 `DateTimeColumn` 默认存 Unix **秒**（或开 `store_date_time_values_as_text` 存 ISO-8601 文本），两种都和 Room 现状的 epoch **毫秒**对不上。时间戳列（`lastRefreshAt`、`lastStatusCode` 无关、Templates 的 `updatedAt`/`lastRefreshAt`）直接用 `IntColumn` 存 `DateTime.now().millisecondsSinceEpoch`，读取时 `DateTime.fromMillisecondsSinceEpoch(v)` 转回。这样迁移时旧库数据无需换算。`lastError` 是文本字段，不在此列。
>
> 注② **列名必须与 Room 的 camelCase 对齐**。drift **默认把 Dart getter 名转成 snake_case**（如 `userAgent` → `user_agent`），而 Room 保留 camelCase（`userAgent`）。直接 `SELECT *` 迁移会导致列名对不上。两种对齐方式，**二选一**：
> - **全局策略（推荐，改动最小）**：在 `build.yaml` 设 `case_from_dart_to_sql: camelCase`，所有表列名自动保留 camelCase，与 Room 完全一致。
>   ```yaml
>   targets:
>     $default:
>       builders:
>         drift_dev:
>           options:
>             case_from_dart_to_sql: camelCase
>   ```
>   （需 `drift_dev ≥ 2.14` 支持该选项）
> - **逐列显式**：`TextColumn get userAgent => text().named('userAgent')();`，每个 getter 都加 `.named(...)`。仅在全局策略验证失败时降级使用。
>
> ⚠️ **这是阶段 1 硬验收项，不是"POC 时确认"**：旧库迁移（§3.4）强依赖列名完全一致。阶段 1 完成时必须用一份真实 `sub_converter.db` 跑通 `ATTACH + INSERT INTO ... SELECT` 且校验所有列值正确。若全局 camelCase 策略在该 drift_dev 版本上不生效，立即降级为逐列 `.named(...)`，不要拖到迁移阶段才发现。

### 3.3 其余三表

- **NodeDnsCaches**：复合主键 `(sourceId, hostname)`，外键 `sourceId → subscription_sources.id ON DELETE CASCADE`，索引 `sourceId`。drift 用 `@override Set<Column> get primaryKey => {sourceId, hostname};`，并在 database `onCreate` 写外键 + index。
- **Templates**：含 `type TEXT DEFAULT 'YAML'`、`sortOrder`、`global`、`updatedAt`（IntColumn millis，见 §3.2 注①）、`lastRefreshAt`（同）。
- **OutputProfiles**：含 `uploadToGist`、`gistId`、`fetchCount`、`overrideIds`、`sourceIds`(逗号分隔字符串)。**新增列 `gistRawUrl`**（相对 Room v10 的新字段，drift 直接建）：Gist 上传成功后由 `GistUploader.upload` 返回的 `rawUrl`（见 GistResult）持久化，供 iOS 端展示与复制。每次上传 PATCH 后若 rawUrl 变化则更新。

### 3.4 旧 Android 用户数据迁移

**问题**：现有 Android 用户有 v10 的 `sub_converter.db`，Flutter 重写后 drift 数据库文件路径可能不同。

**关键约束：drift schema ≠ Room v10 schema**。drift 表比 Room 多了 `gistRawUrl`（§3.3 新增列），其他三表字段一致。因此**不能再用"字段一一对应"的 `INSERT INTO ... SELECT *`**，必须：
- 对字段一致的 3 张表（`subscription_sources` / `node_dns_cache` / `templates`）可用显式列清单的 SELECT。
- 对 `output_profiles` 必须写**显式列清单**，并给 `gistRawUrl` 填默认值 `''`。

**方案**：在 Android 端 Flutter 启动时检测旧 `sub_converter.db`（`/data/data/<pkg>/databases/sub_converter.db`），若存在且 drift 库为空：
1. 用 `sqlite3` 直接 attach 旧库（`ATTACH '/data/.../sub_converter.db' AS old;`）
2. 字段一致的 3 张表，显式列出 Room v10 拥有的全部列名后拷贝：
   ```sql
   INSERT INTO subscription_sources (id, name, url, website, userAgent, enabled, ...)
   SELECT id, name, url, website, userAgent, enabled, ... FROM old.subscription_sources;
   ```
3. `output_profiles` 用显式列清单 + `gistRawUrl` 填默认空串：
   ```sql
   INSERT INTO output_profiles
     (id, name, sourceIds, templateId, enabled, prefix, includeRegex, excludeRegex,
      overrideIds, updateIntervalHours, fetchCount, uploadToGist, gistId, gistRawUrl)
   SELECT id, name, sourceIds, templateId, enabled, prefix, includeRegex, excludeRegex,
          overrideIds, updateIntervalHours, fetchCount, uploadToGist, gistId, ''
   FROM old.output_profiles;
   ```
4. 复制 4 张表
5. 重命名旧库为 `.bak`

> ⚠️ **列名必须 camelCase 对齐**（§3.2 注②）——旧 Room 库的列名是 camelCase，drift 端若没全局开 `case_from_dart_to_sql: camelCase`，上面 SQL 的列名就对不上，迁移直接失败。这也是把列名对齐定为阶段 1 硬验收的原因。

**这是迁移最容易出错的地方，务必写迁移测试**：用一份真实 v10 `sub_converter.db` 跑通上述 4 条 INSERT，逐表逐字段校验行数和值。

### 3.5 drift 多 isolate 并发（UI + headless engine）

本方案 Android 端存在两个独立 Dart isolate：UI isolate（Flutter UI）和 headless engine isolate（HTTP Server）。**drift 默认不允许跨 isolate 共享一个连接**，二者必须各开连接。

**采用方案：WAL 多连接**（已确认）：
1. 数据库初始化时统一执行 `PRAGMA journal_mode=WAL;`（drift 可通过 `beforeOpen` 或 `customStatement` 设置），开启 SQLite Write-Ahead Logging，允许多读 + 单写并发。
2. UI isolate 和 headless engine isolate 各自用 drift 打开**同一个 db 文件**（路径通过 `path_provider` 的 `getApplicationDocumentsDirectory()` 统一）。
3. 写操作靠 SQLite 自身的 WAL 锁串行化；跨 isolate 的数据可见性靠 drift 的 `Stream`（基于 SQLite 的 `UPDATE` 触发，但**跨连接不会自动推送**）。

**关键坑：跨连接的 stream 不会自动更新**。`fetchCount` 在 headless engine 更新后，UI 端 drift `Stream` 不一定能收到通知（drift 的表更新通知基于同连接的事务钩子）。两种解决：
- **方案 a（推荐）**：headless engine 写完 `fetchCount` 后，通过 platform channel / `IsolateNameServer` 端口主动通知 UI isolate 刷新对应查询。
- 方案 b：UI 端每次回到前台时强制重查（牺牲实时性）。

> ⚠️ 这条是 drift 多连接最大的隐藏成本，POC 必须验证「server 更新 → UI 可见」的实时性。drift 官方的 isolate 连接共享（单连接 isolate）是另一条路，本方案不采用，见附录 C 对比。

**验收**：
- [ ] 两 isolate 并发读写不抛 `SQLITE_BUSY`
- [ ] headless engine 写入后，UI 端 `fetchCount` 在 2 秒内可见（方案 a）

---

## 4. ServerSettingsStore（DataStore → shared_preferences）

`ServerSettingsStore.kt` 是纯 KV，直接映射：

| ServerSettings 字段 | 类型 | 默认值 | 存储 |
|---------------------|------|--------|------|
| enabled | bool | false | shared_preferences |
| autoStartOnBoot | bool | false | |
| allowLan | bool | false | |
| port | int | 9876 | (coerce 1024..65535) |
| token | String | '' | |
| globalUserAgent | String | 'SubConverter/1.0' | |
| gistToken | String | '' | |

iOS 端 `enabled`/`autoStartOnBoot`/`allowLan` 无意义，UI 上隐藏。`gistToken` 是 iOS 的核心配置。

> 考虑用 `flutter_secure_storage` 存 `gistToken`（敏感凭证），其余用 `shared_preferences`。

### 4.1 旧 Android DataStore → Flutter 迁移

现有设置存在 Android Jetpack DataStore Preferences，文件位于
`/data/data/<pkg>/files/datastore/server_settings.preferences_pb`（pb 格式，见 ServerSettingsStore.kt:14 `preferencesDataStore("server_settings")`）。

**Flutter 的 `shared_preferences` 读不了这个 pb 文件**，必须显式迁移，否则旧用户的 `enabled`/`autoStartOnBoot` 丢失——直接导致开机自启失效（§7.2 要求 BootStartupReceiver 在无 UI 时读取最小配置）。

**迁移方案（Android 首次启动时）**：

⚠️ **关键：迁移不能只依赖 `MainActivity.configureFlutterEngine`**。如果用户升级后**没打开 App 就重启设备**，BootStartupReceiver 此时既没有 Flutter/UI、原生 SP 也没被迁移写入——开机自启会读到空值而不启动服务。因此迁移逻辑必须有一条**原生侧可独立执行**的路径。

1. **原生侧迁移（BootStartupReceiver / LocalHttpServerService 启动时）**：写一个 Kotlin 工具类（如 `LegacySettingsMigrator`），能用 `androidx.datastore.preferences` 直接读旧 `server_settings.preferences_pb`（DataStore 是标准格式，Kotlin 可读），把 `enabled`/`autoStartOnBoot`（开机自启所需的最小集）写入原生 SharedPreferences（`commit()` 同步落盘）。Receiver/Service 在读原生 SP 前先调用此工具，原生 SP 为空则尝试从旧 DataStore 迁移。
2. **Flutter 侧完整迁移（用户打开 App 时）**：用 `MainActivity.configureFlutterEngine` 注册的 MethodChannel 读取旧 DataStore pb 的**全部字段**，转成 Map 返回给 Dart，写入 `shared_preferences`（普通字段）和 `flutter_secure_storage`（`gistToken`）。Flutter 写完后也同步回原生 SP（见 §4.2）。
3. 标记迁移完成（写入原生 SP 的 `settings_migrated=true`），后续 Receiver/Flutter 都不再读旧 pb。

> 两条路径分工：原生侧只保证开机自启所需的最小集（`enabled`/`autoStartOnBoot`，这两个决定服务起不起）；Flutter 侧负责完整设置（含 `port`/`token`/`gistToken` 等，这些在服务起来后由 Dart engine 从 shared_preferences 读）。

**字段映射**（对照 ServerSettingsStore.kt:63-71 的 Keys）：

| DataStore key | 原生 SP（Receiver 真源） | Flutter shared_preferences |
|---------------|:---:|:---:|
| `enabled` | ✅ | ✅ |
| `auto_start_on_boot` | ✅ | ✅ |
| `allow_lan` | — | ✅ |
| `port` | — | ✅ |
| `token` | — | ✅ |
| `global_user_agent` | — | ✅ |
| `gist_token` | — | **flutter_secure_storage**（敏感，从明文 KV 升级） |

**验收**：
- [ ] 旧用户升级后**直接重启设备（不打开 App）**，若旧设置 `autoStartOnBoot=true`，服务能按设置自启（原生侧迁移路径生效）
- [ ] 旧用户升级后打开 App，全部设置可见且 `gistToken` 在 secure storage
- [ ] `gistToken` 迁到 secure storage 后，原 shared_preferences 不残留明文

> BootStartupReceiver 在无 UI 自启时如何拿到 `enabled`/`autoStartOnBoot`：见 §7.2 + 本节步骤 1 的原生侧迁移。原生 SharedPreferences（android.content.SharedPreferences）是 Receiver 唯一真源，不依赖 Flutter 启动。

### 4.2 双写一致性问题（必须解决）

上一节让 `enabled`/`autoStartOnBoot` 落到原生 SharedPreferences，但**没有定义何时写入**——用户在 Flutter UI 改了开关后，若不主动同步到原生 SP，开机自启会读到旧值。这是真实的双写一致性陷阱。

**明确写入时机**：
1. **用户在 Flutter UI 改设置时**：`updateServerSettings()` 执行后，**同步**（非异步）通过 MethodChannel 写入原生 SP。写原生 SP 失败应回滚 UI 状态并提示，不能「写了 Flutter 忘了原生」。
2. **DataStore 迁移完成后**（§4.1）：Flutter 完整迁移写入 shared_preferences 的同时，也同步写入原生 SP（至少 `enabled`/`autoStartOnBoot`）。
3. **原生 SP 为开机自启的唯一真源**：BootStartupReceiver 只读原生 SP，不读 Flutter。Flutter shared_preferences 是 UI 层视图，原生 SP 是 Receiver 真源，两者由写入逻辑保证一致。

**一致性保证**：
- 原生 SP 写入用 `commit()`（同步落盘）而非 `apply()`（异步），避免进程被杀前未落盘。
- 设置变更的 UI 操作要 await MethodChannel 返回成功后再更新 UI 状态，避免「乐观更新后原生写入失败」。
- 增加 P2 验收：在 Flutter UI 切换 `autoStartOnBoot` 后立即杀进程，重启设备验证读到的是新值。

> ⚠️ 这条不解决，开机自启会出现「UI 显示开、实际不开」或反之的幽灵状态，用户极难排查。

---

## 5. ShareLinkParser（最该写测试的纯逻辑）

`ShareLinkParser.kt`（256 行）解析 6 种协议：`vless / vmess / trojan / ss / hy2(hysteria2)`，输出 Mihomo proxy map。

### 翻译要点
- Kotlin `URI(link)` → Dart `Uri.parse(link)`，注意 `uri.host`/`uri.userInfo`/`uri.queryParameters` 字段对应
- `Base64.getDecoder().decode` → Dart `base64Url`/`base64` + padding 补齐逻辑要照搬（见 `decodeBase64Text`，先试原符号再试 urlsafe 互换）
- `LinkedHashMap` 保序 → Dart `LinkedHashMap`（Dart Map 默认保序 ✅）
- `JSONObject` (vmess) → `dart:convert` `jsonDecode`

### 回归测试（必须）
从真实订阅收集每种协议至少 3 个样本链接，断言解析出的 proxy map 与 Kotlin 版本逐字段一致。这是迁移质量的保险绳。

```
test/resources/links/
  vless_reality.txt
  vless_ws_tls.txt
  vmess_ws.txt
  vmess_tcp.json       # vmess base64 后是 json
  trojan_tls.txt
  ss_plain.txt
  ss_base64.txt
  hy2.txt
```

---

## 6. ⚠️ 最大风险：YAML 处理（解析 + 输出分离选型）

`MihomoYamlService.kt` 是核心渲染器。Kotlin 用 SnakeYAML 一个库同时做 load + dump，**Dart 没有这种一体库**，必须把"解析"和"输出"拆开选型。

### 6.1 关键事实（已核实）

- **Dart `yaml` 包只解析、不序列化**——没有 `dump`/`toYamlString` API。任何"用 yaml 包 dump"的描述都是错的。
- 输出端需另选：`yaml_writer`（标准方案，见下）、或自写 emitter、或放弃 YAML 库走字符串模板。

### 6.2 选型

| 方向 | 库 | 职责 | 评价 |
|------|----|----|------|
| 解析（load） | `yaml` | `loadYaml` → `YamlMap`（保序） | ✅ 主方案 |
| 输出（dump） | `yaml_writer` | `YamlWriter().write(map)` → YAML 字符串 | ✅ **主方案** |
| 输出（备选） | 自写 emitter | 手写缩进/引号控制 | 仅当 yaml_writer 输出不符 Mihomo 要求 |
| 输出（备选） | 字符串模板 | 放弃 YAML 库，只做占位符替换 + 规则拼接 | 规则复杂时维护成本高 |

### 6.3 能力对照

| SnakeYAML 特性 | Dart 方案 | 满足 | 风险 |
|----------------|----------|:----:|------|
| `load()` → 保序嵌套 Map | `yaml.loadYaml` → `YamlMap` | ✅ | 低 |
| `dump()` BLOCK 缩进 2 | `yaml_writer` 默认输出 | ⚠️ 需验证 | **中** |
| 保留注释 | — | ❌ | 低（见 6.4，非回归） |
| 保留键顺序 | Dart `Map`/`YamlMap` 保序 | ✅ | 低 |
| 数字/布尔类型 | yaml 解析成对应类型，输出时 yaml_writer 自动处理 | ⚠️ | 中 |

### 6.4 关键决策点

`MihomoYamlService.renderTemplate` 的流程是 **load → 改 → dump**（见 MihomoYamlService.kt:62-94）。

**已查证**：`DEFAULT_MIHOMO_TEMPLATE`（见 DefaultTemplates.kt）本身**不含注释**，SnakeYAML 旧版 dump 出来也不保留注释，因此「注释丢失」在本项目中**不是回归**，yaml_writer 无注释能力可以接受。`{{proxy_names}}` 占位符在 load 阶段是普通字符串、dump 前已被 `replacePlaceholders` 替换为真实值，不依赖注释能力。

### 6.5 行动项
- [ ] POC：`yaml_writer` 输出一份真实渲染结果，喂给 Mihomo 内核验证可加载
- [ ] **重点验证易翻车场景**（Mihomo 配置高频出现）：
  - 多行字符串 / literal block（`|`、`>`）—— 规则脚本、JS 覆写体常含换行
  - 含特殊字符的标量值：`:`、`#`、`{`、`}`、`[`、`]`、`,`、`&`、`*`、`!`、首尾空格——决定 yaml_writer 是否正确加引号
  - 数字 vs 字符串：端口号、密码串若纯数字需保持字符串（yaml_writer 别误判成 int）
  - 中文节点名（含 unicode）的转义/引用
- [ ] 若 yaml_writer 输出不符，备选顺序：自写 emitter（控制缩进/引号）→ 字符串模板
- [ ] 关注 `yaml` 包解析出的 `YamlNode` 子类型（`YamlMap`/`YamlList`/`YamlScalar`），转成普通 `Map` 再喂 `yaml_writer` 时类型需正确转换

### 6.6 `deepMerge` / `replacePlaceholders` / `makeNamesUnique`
这些是纯 Map 操作（MihomoYamlService.kt:144-256），与 YAML 库无关，直接翻译。注意：
- `+key` 前插、`key+` 后追加、`key!` 强制覆盖、`<key>` 去 wrap —— 全部照搬逻辑
- Dart 没有 `copyValue` 这种递归深拷贝，需自己写

---

## 7. HTTP Server（三平台策略）

### 7.1 Windows：纯 Dart（首选验证平台）

`LocalHttpServer.kt`（284 行）是原始 `ServerSocket` 实现，路由如下：

| 路由 | 方法 | 行为 | Flutter 对应 |
|------|------|------|-------------|
| `GET /health` | — | `{"status":"ok","running":true}` | HttpServer |
| `GET /zashboard` | — | 302 → `/zashboard/` | HttpServer |
| `GET /zashboard/*` | — | 静态资源（含 SPA fallback 到 index.html） | HttpServer + assets |
| `GET /subscriptions/{id}.yaml` | token 校验 | 渲染输出 + 自定义 headers | HttpServer |

**关键 headers**（必须保留，LocalHttpServer.kt:143-150）：
- `Profile-Update-Interval`
- `Subscription-Userinfo`（来自 `SubscriptionUserInfo.toHeaderValue()`）
- `Profile-Title`
- `Profile-Web-Page-Url`
- `Content-Disposition: attachment; filename="xxx.yaml"`

Dart `HttpServer` 实现示例骨架：
```dart
final server = await HttpServer.bind(
  settings.allowLan ? InternetAddress.anyIPv4 : InternetAddress.loopbackIPv4,
  settings.port,
);
await for (final request in server) {
  // 路由分发，注意 zashboard SPA fallback、token 校验
}
```

zashboard 静态资源：三平台统一由 Dart 从 `assets/zashboard/` 读（桌面端通过 `rootBundle`，Android headless engine 通过 engine 的 `assets` 目录）。**不保留 Kotlin `AssetManager` 路径**，否则破坏「drift/domain 唯一数据源」原则，Android 端会重新依赖 Kotlin 维护资源映射。

### 7.2 Android：前台服务 + headless FlutterEngine（必须支持）

**不可逃避的原生代码**（`LocalHttpServerService.kt` 169 行 + `BootStartupReceiver.kt`）：
- Android 14+ 前台服务类型 `connectedDevice`、常驻通知、`START_STICKY`
- 开机自启 `RECEIVE_BOOT_COMPLETED`

**硬性要求**：Android 迁移后必须继续支持「前台服务常驻 HTTP Server + 开机自启」。因此 Android 不能只依赖 Flutter UI isolate，也不能把 HTTP Server 当成前台页面能力；开机后用户不打开 App 时，服务也必须能独立启动并响应请求。

**推荐架构**：Kotlin Service 负责 Android 系统能力，Dart 负责业务能力。

| 层 | 职责 |
|----|------|
| `LocalHttpServerService` (Kotlin) | 前台通知、`START_STICKY`、停止按钮、端口启动失败后的通知/状态更新 |
| `BootStartupReceiver` (Kotlin) | 开机后读取原生可访问的最小配置，按需启动前台服务 |
| headless FlutterEngine | 在无 UI 时启动 Dart entrypoint，注册必要插件，持有 server isolate |
| Dart `local_http_server.dart` | `HttpServer` 路由、token 校验、zashboard 静态资源、订阅渲染、fetchCount 更新 |
| drift / domain | 唯一业务数据源和渲染逻辑，避免 Android 保留第二套 Room/domain |

**不采用**：
- Kotlin `LocalHttpServer.kt` 原样保留并继续读 Room：会让 Android 长期保留第二套数据库、YAML 渲染、JS 覆写和 repository 逻辑，三平台结果难以一致。
- Service 通过 MethodChannel 调 UI isolate 渲染：开机自启和用户未打开 App 时没有 UI isolate，不满足硬性要求。

**为何手写 headless engine 而非用 `flutter_background_service` 包**：
- `flutter_background_service`（业界主流方案）开箱即用、自动托管 isolate，但有已知生命周期 bug（foreground service `stopSelf` 不可靠，见 upstream Issue #386），且开机自启支持有限、对 SDK 34+ 前台服务类型的声明灵活度不足。
- 本项目对「开机即用 + stop 可靠」要求高，故采用手写 headless engine 换取完全控制。代价是：需自己处理 isolate 通信、插件注册、生命周期。

**隐藏成本（务必在 POC 量化）**：
- **冷启动耗时**：开机后冷启动一个 FlutterEngine 需初始化 Dart VM、加载 flutter_js、注册插件，实测常达 2-5 秒。Kotlin 现状几十毫秒起来。HTTP Server 的核心价值之一是"开机即用"，若 Mihomo 客户端在系统启动后立刻拉订阅而 server 还没起来，体验劣化。需评估能否接受这个窗口。
- **插件兼容性**：headless engine 不带 UI，`mobile_scanner`/`qr_flutter` 等 UI 插件不应注册到 server engine；需用 `getPlugins()` 精确白名单注册，否则启动失败或体积膨胀。
- **双 engine 内存占用**：UI + headless engine 各持一份 Dart heap，内存翻倍，低端 Android 设备需测。

**Android 验证清单**：
- [ ] App 前台启动服务后，锁屏 30 分钟仍可访问 `/health`
- [ ] 杀掉 Activity 但保留前台服务后，订阅 URL 仍可渲染
- [ ] 设备重启后，若 `enabled=true && autoStartOnBoot=true`，服务自动启动并可访问 `/health`
- [ ] **冷启动耗时**：设备重启到首次 `/health` 200 响应 < 8 秒（优于 Kotlin 现状则更好；若 >15 秒需重新评估路线）
- [ ] 服务 stop action 能关闭 Dart HTTP Server、释放端口并更新持久化状态
- [ ] UI isolate 与 headless engine 同时存在时，drift 读写不抛 `SQLITE_BUSY`，`fetchCount` 更新 2 秒内反映到 UI（见 §3.5）
- [ ] headless engine 的插件白名单不含 UI 插件，启动不报错

> ⚠️ 这是 Flutter 迁移能否满足 Android 现有能力的门槛，应在阶段 0 做 POC。POC 未通过前，不进入正式 UI 迁移。

### 7.3 iOS：无 HTTP Server，Gist URL 是主输出

iOS 端 `server_screen` 隐藏 HTTP 相关 UI，只保留 Gist Token 配置 + 「转换并上传」按钮。转换逻辑复用 `outputRepository.renderProfile` + `gistUploader.upload`。

**Gist URL 回显与触达（iOS 主路径，必须闭环）**：
1. 上传成功后，`GistUploader.upload` 返回的 `rawUrl`（见 GistResult）写入 `OutputProfiles.gistRawUrl`（§3.3）。
2. UI 回显：`OutputCard`（outputs_screen）在 `gistRawUrl` 非空时显示该 URL（截断显示 + 复制按钮 + 「打开」跳转 Safari）。这是 iOS 用户拿到订阅地址的唯一入口，等同 Android 的 subscription URL。
3. 触达：复制按钮用 `Clipboard.setData`；「打开」用 `url_launcher`。下次打开 app 该 URL 仍从 drift 读取，不依赖重新上传。
4. 更新：每次 PATCH 上传后若 `rawUrl` 变化，刷新 drift 记录与 UI。

详见 §10.2 OutputCard 的 iOS 分支说明。

---

## 8. i18n 迁移

`AppI18n.kt`（231 行）的机制：
- 中文字符串为 key，英文为 value 的 map
- `message()` 支持正则模板（如 `已添加订阅 #(\d+)` → `Subscription #${1} added`）
- 根据 `context.resources.configuration.locales[0]` 判断中/英

### Flutter 方案
- 用 `flutter_localizations` + `Locale` 判断
- 把 `AppI18n.english` map（**189 条**键值对）迁到 `strings.dart`，按同样 key→value 结构
- 正则模板迁到 `strings.dart` 的 `regexMessages` 列表（**5 条**，如「已添加订阅 #(\d+)」→「Subscription #${1} added」）
- `l10n()` helper 改为 Riverpod 提供的 `ref.watch(localeProvider)`

### 注意
当前 i18n 用「中文做 key」的方式不优雅，迁移时**可以考虑重构为标准的 ARB 文件 + gen-l10n**，但要付出一次性重构成本。**建议先原样照搬，保证功能一致，后续再优化。**

---

## 9. ⚠️ 第二大风险：EncryptedDns（DoH/DoT）

`EncryptedDns.kt`（299 行）+ `SubscriptionDns.kt`（139 行）实现了：
- DoH（DNS over HTTPS）查询
- DoT（DNS over TLS）查询
- 自定义 DNS 服务器（腾讯 DNSPod / 阿里 AliDNS 预设）
- 两种连接模式：`PRESERVE_DOMAIN` / `IP_URL`

### 风险
Dart 标准库**没有 DoH/DoT 客户端**。需要：
- DoH：用 dio 发 JSON/二进制 DNS query 到 `https://dns.alidns.com/dns-query`，解析 DNS wire format（需 `dnsclient` 或自实现 RFC 1035）
- DoT：需要 TLS socket（`dart:io` `SecureSocket`）+ DNS wire format

**pub 候选**：`dns_client`、`dnsninja`——但生态不如 JVM 的 dnsjava 成熟，需要实测。

### 行动项
- [ ] 阶段 2 中期做技术验证：用 `dio` + 一个轻量 DNS 消息编解码库，对 DoH 跑通 A 记录查询
- [ ] 若 DoT 实现成本高，可考虑**初版只支持 DoH 和系统 DNS，DoT 延期**（DoH 覆盖了腾讯/阿里预设）
- [ ] `IP_URL` 连接模式（替换 URL 主机为 IP + Host header）在 dio 里要禁用重定向、手动处理 Location、手动设置 Host header 和 hostname verifier——较复杂，需仔细翻译 `fetchWithIpUrl`

---

## 10. UI 屏幕-函数对照表

`MainScreen.kt`（4113 行）拆分。左边 Kotlin 函数 → 右边目标 Dart 文件/Widget。

### 10.1 顶层与导航
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `MainScreen` (190) | `ui/main_screen.dart` → `MainScreen` ConsumerWidget |
| `iOSStyleNavigationBar` (2687) | `ui/widgets/navigation_bar.dart` |
| `iOSGroupedCard` (2728) | `ui/widgets/ios_grouped_card.dart` |
| `iOSTintedIcon` (2744) | 同上 |

### 10.2 四个主 Tab
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `SourcesScreen` (2766) + `SourceCard` (2805) | `ui/screens/sources_screen.dart` |
| `OutputsScreen` (2967) + `OutputCard` (3014) | `ui/screens/outputs_screen.dart`（**iOS 专属分支**：`OutputCard` 在 `gistRawUrl` 非空时显示 Gist URL 行 + 复制/打开按钮，见 §7.3） |
| `TemplatesScreen` (3097) + `TemplateCard` (3138) | `ui/screens/templates_screen.dart` |
| `ServerScreen` (3241) + `ZashboardCard` (3450) + `ServerStatusCard` (3544) | `ui/screens/server_screen.dart`（iOS 隐藏） |

### 10.3 编辑屏（全屏 Scaffold）
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `EditScreenScaffold` (432) | `ui/widgets/edit_scaffold.dart` |
| `SourceEditScreen` (469) | `source_edit_screen.dart` |
| `TemplateEditScreen` (730) | `template_edit_screen.dart` |
| `OutputEditScreen` (1700) | `output_edit_screen.dart` |
| `OverrideSelectionList` (1847) + `OverrideSelectionRow` (1907) | output_edit 内 |
| `OverrideHelpScreen` (1570) + `OverrideHelpCodeBlock` (1683) | `override_help_screen.dart` |

### 10.4 代码编辑器（高复杂度）
| Kotlin (行) | 说明 |
|-------------|------|
| `FullScreenCodeEditor` (834) + `CodeEditorField` (894) | 代码编辑器 |
| `highlightCode` (1330) + `appendYamlLine`/`appendJsLine` (1342/1366) | **语法高亮**：Compose 用 `AnnotatedString`，Flutter 用 `TextSpan`/`RichText` |
| `autoIndent` (1498) + `yamlCompletionBlock` (1525) | 自动缩进 + 关键字补全（最近 commit 加的） |
| `editorValidation` (1446) | 实时校验 |

> ⚠️ 这块在 Flutter 里是最费时的 UI 子模块。考虑用 `code_text_field` 或 `re_editor` 包，但需要适配 YAML/JS 高亮和自动补全。**可能需要自己写 `RichText` + `TextField` 的混合实现。**

### 10.5 预览屏
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `NodePreviewScreen` (1959) | `node_preview_screen.dart` |
| `OutputPreviewScreen` (2161) + `YamlTreeView` (2271) + `TreeRowView` (2326) | `output_preview_screen.dart`（YAML 树形折叠预览） |
| `parseYamlToTree` (2116) + `buildRows` (2124) | 树形解析逻辑（纯函数，直接翻译） |

### 10.6 公共表单组件
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `SmallFormField` (2397) | `ui/widgets/ios_form_field.dart` |
| `ChoiceFormField` (2428) | 同上 |
| `iOSFormTextField` (3607) / `iOSFormSwitch` (3650) | 同上 |
| `iOSIconButton` (3686) / `IOSInfoRow` (3706) / `iOSEmptyState` (3731) | 同上 |
| `RegexHint` (2508) + `RegexPreview` (2562) + `calculateRegexPreview` (2649) | 正则预览（纯逻辑） |

### 10.7 扫码与二维码
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `QrScanScreen` (3764) | `qr_scan_screen.dart`（`mobile_scanner`） |
| `QrShareScreen` (3906) + `generateQrBitmap` (3962) | `qr_share_screen.dart`（`qr_flutter`） |

### 10.8 工具函数（纯逻辑，直接翻译）
| Kotlin (行) | Dart 目标 |
|-------------|-----------|
| `extractNodeNames` (3980) | `domain/...` |
| `trafficText` (3991) / `formatBytes` (4039) | `ui/widgets/...` |
| `sourceDnsLabel` (4002) / `overrideCardSubtitle` (4025) | |
| `sourceNames` (4050) / `overrideSummary` (4056) | |
| `subscriptionUrl` (4094) / `zashboardUrl` (4100) / `localLanAddress` (4105) | `server/...`（LAN IP 获取平台相关） |

---

## 11. ViewModel → Riverpod

`MainViewModel.kt`（410 行）用 `StateFlow` 合并 5 个数据源（sources/templates/profiles/settings/running）+ messages + refreshingIds。

### Riverpod 对应
```dart
// ui/main_screen.dart
final mainUiStateProvider = StreamProvider<MainUiState>((ref) async* {
  // 合并多个 stream：sourcesDao.observeAll(), templatesDao.observeAll(), ...
  // 用 RxJS 风格的 combineLatest，或 riverpod 的 ref.watch + AsyncValue
});

// 操作方法（对应 MainViewModel 的 saveSource/saveTemplate/saveProfile...）
// 拆成多个 NotifierProvider，而非一个巨型 ViewModel
final sourceActionsProvider = NotifierProvider<SourceActions, void>(SourceActions.new);
```

建议**拆分** `MainViewModel`（410 行）成多个职责清晰的 Notifier，而不是原样照搬。Kotlin 版已经偏大。

> **重要前置：domain Repository 一律按 abstract interface 写**（`SubscriptionRepository`/`OutputRepository` 是抽象接口，drift 是其实现之一）。这样 Plan A（drift 实现）和 Plan B（Android 走 platform channel+Room 实现，见 §17）都能复用同一 domain 抽象，中途切方案不必改 domain 调用方。阶段 2 翻译 domain 时就按此约束。

---

## 12. 平台差异处理清单

| 能力 | Android | iOS | Windows | 实现 |
|------|---------|-----|---------|------|
| HTTP Server UI | ✅ 显示 | ❌ 隐藏 | ✅ 显示 | `if (!Platform.isIOS)` |
| 开机自启开关 | ✅ | ❌ | ✅ | 同上 + 注册表插件 |
| 定时刷新 | ✅ workmanager | ⚠️ 前台 | ✅ Timer | 条件编译 |
| LAN IP 获取 | NetworkInterface | NetworkInterface | NetworkInterface | `NetworkInterface.list()` |
| zashboard 入口 | ✅ | ❌ | ✅ | 随 HTTP |
| 扫码 | mobile_scanner | mobile_scanner | ⚠️ 可选 | |

用 `Platform.isAndroid / isIOS / isWindows` 做条件渲染和条件逻辑。

---

## 13. 依赖最终清单（pubspec.yaml）

> 版本号仅作参考，正式添加时以 `flutter pub add <pkg>` 取到的最新稳定版为准。

```yaml
dependencies:
  flutter_riverpod: ^2.5.1      # 状态管理
  drift: ^2.16.0                # 数据库
  drift_flutter: ^2.0.0         # drift + sqlite3_flutter
  sqlite3_flutter_libs: ^0.5.0  # SQLite 引擎
  shared_preferences: ^2.2.0    # ServerSettings KV
  flutter_secure_storage: ^9.0.0 # gistToken 等敏感凭证
  dio: ^5.4.0                   # HTTP 客户端
  yaml: ^3.1.0                  # YAML 解析（仅 load，⚠️ §6）
  yaml_writer: ^2.0.1           # YAML 输出（dump），⚠️ §6 POC 验证
  flutter_js: ^0.8.7            # JavaScript 覆写引擎（仍活跃，⚠️ R5 三平台引擎差异）
  mobile_scanner: ^5.1.0        # 二维码扫描
  qr_flutter: ^4.1.0            # 二维码生成
  workmanager: ^0.5.2           # 后台定时任务 (Android)
  flutter_localizations:        # i18n
    sdk: flutter
  intl: any
  path_provider: ^2.1.0         # 数据库文件路径
  path: ^1.9.0
  url_launcher: ^6.2.0          # iOS Gist URL 打开（§7.3）

dev_dependencies:
  drift_dev: ^2.16.0            # drift 代码生成
  build_runner: ^2.4.0
  flutter_lints: ^3.0.0
```

**Android headless engine 相关**（POC 通过后正式加入，本方案手写 headless engine，不用 `flutter_background_service`；后者列为备选，若 POC 暴露不可控问题再切换）。

> DoH/DoT 的 DNS 库待技术验证后确定（§9）。

---

## 14. 风险登记册

| # | 风险 | 影响 | 概率 | 缓解 |
|---|------|------|------|------|
| R1 | yaml_writer 输出格式不被 Mihomo 接受 | 高 | 中 | 阶段 2 早期做 round-trip 测试（喂真实渲染结果给 Mihomo 内核） |
| R2 | DoH/DoT 在 Dart 无成熟库 | 高 | 中 | 先只做 DoH，DoT 延期 |
| R3 | Android headless FlutterEngine 常驻 HTTP Server | 高 | 高 | 阶段 0 POC：前台服务、开机自启、无 UI 渲染、drift 并发 |
| R4 | 代码编辑器语法高亮/补全在 Flutter 难实现 | 中 | 高 | 评估 re_editor 包，必要时降级为纯 TextField |
| R5 | flutter_js 跨平台引擎差异（Android=QuickJS, iOS=JavaScriptCore）导致 JS 覆写静默出错 | 高 | 中 | 阶段 2 翻译 js_override_service 时**立即**跑三平台 JS 用例（见 §5.x）；排查现有覆写脚本的 ES6+ 用法（BigInt/`Array.at`/`Object.hasOwn` 等 JSCore 支持滞后的特性） |
| R6 | 旧 Android 用户 Room v10 数据迁移 | 中 | 中 | 写迁移测试，用真实 db 验证 |
| R7 | iOS 前台转换体验差（用户预期后台） | 低 | 中 | 产品说明 + Gist 工作流文档 |
| R8 | Windows 端打包/签名/分发 | 低 | 低 | msix 或 portable zip |
| R9 | 总工期超出 50%（实际 11-13 周） | 高 | 高 | 三个 POC 可能需迭代而非一次通过；R4 编辑器降级或自写都加期；预留 30-50% buffer，对外按 10-13 周承诺；设中止线（§18）避免沉没成本 |
| R10 | Android 低端机双 engine（UI + headless）内存压力 | 中 | 中 | 真机测试覆盖低端设备（≤4GB RAM）；必要时合并 engine 或延迟加载 server engine 插件 |

---

## 15. 验收标准（Definition of Done）

每个平台需满足：

- [ ] **Android**：现有 Kotlin 版本所有功能在 Flutter 版本可用，旧用户 db 可迁移，前台服务 + 开机自启正常
- [ ] **Windows**：HTTP Server 本机/局域网可访问，订阅转换正确，可打包为 exe/msix
- [ ] **iOS**：订阅管理 + 转换 + Gist 上传全流程通，扫码可用，无后台依赖
- [ ] **iOS Gist 流闭环**：上传成功后 `gistRawUrl` 持久化，UI 展示该 URL 且可一键复制；下次打开 app 仍可见；PATCH 更新后 URL 同步刷新

跨平台一致性：
- [ ] 同一组订阅源/输出/覆写在三平台渲染结果一致（用回归测试保证）
- [ ] ShareLinkParser 全协议样本通过
- [ ] YAML 渲染输出与 Kotlin 版本 diff 为零（或仅有可接受的格式差异）

---

## 16. 推荐落地顺序（降低风险）

不要按章节顺序做。按"风险高/阻塞多"的优先：

1. **先验证 R3（Android 常驻 Server）**：Kotlin 前台服务启动 headless FlutterEngine，开机后无 UI 可响应 `/health`（1-2 天 spike）
2. **再验证 R1（YAML 输出）**：用 `yaml` 解析 + `yaml_writer` 输出，跑通 `mihomo_yaml_service.dart` 的 round-trip，并喂给 Mihomo 内核验证可加载（1-2 天 spike）
3. **再验证 R2（DNS）**：DoH 跑通 A 记录查询（1 天 spike）
4. 确认 R3/R1/R2 可行后，再正式进入阶段 1（数据层）
5. Windows 端作为第一个完整跑通的端（无原生包袱，快速验证 domain + UI）
6. Android 端在阶段 3 接入并持续回归（不能最后才接，因为它是硬性能力）
7. iOS 任何时候都可以做（依赖最少）

**如果 R3、R1 或 R2 任一 spike 失败，需要重新评估是否继续 Flutter 路线（回到 KMP 选项，或下文 §17 Plan B）。**

---

## 17. Plan B：Android 保留 Kotlin Server（仅当阶段 0 POC 失败时启动）

> ⚠️ **Plan B 与正文 Plan A（headless FlutterEngine + drift 唯一数据源）互斥**，二者不能混用。Plan B 是一套**独立的平行架构**，下面列出它额外引入的全部代价——它不是"一句话回退"，启动它会显著扩大工作量。

### 触发条件
阶段 0 POC 任一不通过：headless engine 冷启动 >15 秒、或 drift WAL 跨 isolate 不可靠、或 POC 无法在 6 天内跑通。

### 架构
- Android 端 HTTP Server 继续用 Kotlin 原生 `ServerSocket` + Room（保留 `LocalHttpServer.kt` / `LocalHttpServerService.kt` / `BootStartupReceiver.kt`）。
- iOS / Windows 端走 Flutter（drift + Dart HttpServer / Gist）。
- Flutter UI 层三平台共享。

### Android UI 数据边界（必须明确，否则 Plan B 不可执行）

Android UI 是 Flutter 共享代码，但数据源是 Room——两者不能直接对接，必须明确边界。Plan B 有两个子选项，**二选一**：

| 子选项 | 做法 | 优劣 |
|--------|------|------|
| **B1：Android UI 走 platform channel 读 Room（推荐）** | Flutter UI 不直接用 drift；所有数据访问通过 MethodChannel 由 Kotlin 端读写 Room。定义统一的 Repository channel（`sourceDao.observeAll` → channel stream）。iOS/Windows 端同一个 Repository 接口由 drift 实现，Android 端由 Room+channel 实现。 | 单一数据源（Android=Room），无同步问题；但 channel 数据序列化开销大，且 stream 跨 channel 较繁琐。**Repository 接口抽象是关键**：domain 层定义抽象 `SubscriptionRepository`，各平台实现注入。 |
| B2：Android 同时跑 drift + Room，双向同步 | Android UI 用 drift，Server 用 Room，两者靠同步。 | ❌ 双写同步极其复杂，一致性几乎无法保证，不推荐。 |

**采用 B1**：domain 层（`SubscriptionRepository`/`OutputRepository`）定义为抽象接口，三平台各自实现：
- iOS / Windows：drift 实现
- Android：Room + platform channel 实现（Kotlin 侧封装 DAO 调用，channel 返回 JSON/List）

这意味着 **Plan B 的 domain 层不能是具体 class，必须是 abstract interface**——这是 Plan A（drift 直接实现）和 Plan B（channel 实现）都能兼容的唯一设计。**建议 Plan A 阶段 2 翻译 domain 时就按 interface 写**，这样即使中途切 Plan B，domain 抽象不变。

### Plan B 额外代价（必须全部接受）
| 项 | 代价 |
|----|------|
| 双数据库 | Android=Room，iOS/Windows=drift。schema 演进要两边同步改，drift migration 和 Room Migration 各写一遍 |
| 双 domain | YAML 渲染、JS 覆写、ShareLinkParser、订阅解析在 Kotlin 和 Dart 各维护一份 |
| **Android Repository 双实现** | 同一 `SubscriptionRepository` 接口在 Android 走 channel+Room、iOS/Windows 走 drift，两套实现都要写+测 |
| 跨平台一致性测试矩阵 | 同一输入要在 Kotlin domain 和 Dart domain 都跑回归，diff 必须为零；新增一条规则改两边 |
| Android 旧用户数据迁移 | Room→Room 无需迁移（✅，这是 Plan B 唯一省的地方），但 Android 端 Settings 迁移仍要做（见 §4 迁移） |
| 发布分支 | 至少两个构建产物（Android 含 Kotlin server，iOS/Windows 纯 Flutter），CI 流水线分叉 |

### 决策建议
- 若 POC 失败仅因冷启动慢，优先尝试优化（延迟加载插件、缩小 engine）再决定，不要轻易进 Plan B。
- 若 POC 失败因 drift 跨 isolate 不可靠，优先切 drift isolate 连接共享（附录 C），仍属 Plan A。
- 只有 Plan A 的核心架构被证伪时，才启动 Plan B，并应重新评估 KMP 路线（KMP 的 Kotlin domain 可被 Android 直接复用，避免双 domain）。

---

## 18. 中止条件与部分完成的处置（止损策略）

迁移可能做了一半需要止损。明确各阶段中止时的状态与旧版本处置。

### 18.1 止止触发线
任一即触发决策会：
- 阶段 0 POC 失败且 Plan B/KMP 评估后仍不可接受
- 累计实际工期超过承诺工期（含 buffer，即 ~13 周）的 80%，且关键路径未到阶段 4
- 连续两个 POC 迭代仍未通过

### 18.2 部分完成状态的资产价值
| 完成到 | 可保留资产 | 丢弃/搁置 |
|--------|-----------|----------|
| 阶段 0（POC 失败） | 工程骨架、POC 经验文档 | POC 代码 |
| 阶段 1-2（数据+domain） | **drift schema、domain 纯逻辑+测试**有长期价值，可复用到任何后续方案（含 KMP 的 Dart 侧、或未来重试） | — |
| 阶段 3（server 完成、UI 未做） | Windows 端可作为一个**可用的 CLI/桌面工具**先发布（无 UI 也能本地转换） | Android/iOS UI |
| 阶段 4 进行中 | 已完成的屏幕可作为渐进发布的一部分 | — |

### 18.3 旧 Kotlin 版本的维护策略（关键）
- **迁移期间旧 Kotlin 版本必须保持在 main 分支可发布状态**，`flutter-rewrite` 在独立分支。不要在 main 上边迁移边改。
- 止损后：旧 Kotlin 版本回到唯一维护线，`flutter-rewrite` 分支保留不删（资产见 18.2）。
- 若已完成阶段 1-2，旧版本的 bugfix 应评估是否同步到 Dart domain（双写成本），否则两边逻辑会进一步分化。

### 18.4 渐进发布选项（降低全有全无风险）
不必等三平台全做完才发布：
- Windows 端先发（最快，无原生包袱）
- iOS 端次发（依赖最少）
- Android 端最后发（最复杂，旧 Kotlin 版本继续服务到 Flutter Android 验证充分）

> 核心原则：**任何止损点都要有可交付物或明确的资产沉淀，避免"做了几个月全丢弃"**。

---

## 附录 A：文件-行数速查

| 文件 | 行数 | 迁移难度 | 备注 |
|------|------|:------:|------|
| ui/MainScreen.kt | 4113 | ⭐⭐⭐ | 拆成 ~15 个 dart 文件 |
| ui/MainViewModel.kt | 410 | ⭐⭐ | 拆成多个 Riverpod Notifier |
| domain/EncryptedDns.kt | 299 | ⭐⭐⭐⭐⭐ | §9 最大风险 |
| server/LocalHttpServer.kt | 284 | ⭐⭐⭐⭐ | Dart HttpServer 重写 + Android headless engine + 多 isolate（§7.2/§3.5） |
| domain/MihomoYamlService.kt | 257 | ⭐⭐⭐⭐⭐ | §6 解析(yaml)+输出(yaml_writer)分离，输出选型是关键风险 |
| domain/ShareLinkParser.kt | 256 | ⭐⭐ | 配回归测试 |
| domain/OutputRepository.kt | 237 | ⭐⭐ | |
| i18n/AppI18n.kt | 231 | ⭐⭐ | |
| ui/theme/Theme.kt | 219 | ⭐⭐ | |
| domain/SubscriptionFetcher.kt | 195 | ⭐⭐⭐ | dio + §9 |
| server/LocalHttpServerService.kt | 169 | — | Android 保留原生 |
| domain/SubscriptionDns.kt | 139 | ⭐⭐⭐ | §9 |
| domain/NodePreResolver.kt | 138 | ⭐⭐⭐ | §9 |
| data/AppDatabase.kt | 119 | ⭐⭐⭐ | drift 重建 |
| domain/JsOverrideService.kt | 115 | ⭐⭐⭐ | flutter_js |
| domain/GistUploader.kt | 110 | ⭐⭐ | |
| data/Daos.kt | 97 | ⭐⭐ | drift DAO |
| data/Entities.kt | 95 | ⭐⭐ | drift tables |
| domain/Models.kt | 90 | ⭐ | |
| core/AppContainer.kt | 78 | ⭐⭐ | Riverpod providers |
| data/settings/ServerSettingsStore.kt | 72 | ⭐ | |
| domain/RefreshWorker.kt | 62 | ⭐⭐ | workmanager |
| domain/RemoteTextFetcher.kt | 57 | ⭐ | dio |
| server/BootStartupReceiver.kt | 29 | — | Android 保留原生 |
| domain/SubscriptionUserInfoParser.kt | 29 | ⭐ | |
| domain/DefaultTemplates.kt | 27 | ⭐ | |
| MainActivity.kt | 26 | ⭐ | |
| SubConverterApp.kt | 14 | ⭐ | |
| **合计** | **8105** | | |

---

## 附录 B：版本对照与修订记录

| 版本 | 修订 |
|------|------|
| v1 (初版) | ZCode 初稿，Android 原方案为「Kotlin 保留独立 Server+Room」 |
| v2 (评审修订) | 引入 headless FlutterEngine + 唯一数据源原则；Android 常驻 Server 提升为阶段 0 POC |
| v3 (二次 review) | 修正 drift `autoIncrement` 写法；补 drift 多 isolate 并发方案（WAL）；量化 headless engine 冷启动耗时与隐藏成本；关闭 YAML 注释 open question；统一 zashboard 资源读取路径 |
| v4 (三次 review) | YAML 输出方案重构（yaml 只解析 + yaml_writer 输出）；drift 时间戳改 IntColumn(millis) + 列名 camelCase 对齐；Plan B 独立成章（与 Plan A 互斥）；补 DataStore 旧设置迁移；闭合 iOS Gist raw URL 模型与验收；工期上调至 7.5-9 周 |
| v5 (四次 review) | 修正 AGENTS.md Room 版本（8→10，以代码为准）；R5 flutter_js 引擎差异风险上调 + 阶段 2 提前三平台 JS 验证；yaml_writer POC 验证项细化（多行串/特殊字符）；§4.2 双写一致性（Flutter→原生 SP）；§7.3 iOS Gist URL 回显触达闭环 + §10 OutputCard iOS 分支；工期加 30-50% buffer（对外 10-13 周）+ R9/R10；§18 中止条件与渐进发布；i18n 条目数修正为 189+5；flutter_js 核实仍活跃不换 webf |
| v6 (五次 review) | gistRawUrl 破坏旧库迁移→§3.4 改显式列清单+默认值；§4.1 加原生侧迁移路径（升级后未开 app 重启可读）；§17 Plan B 明确 Android UI 数据边界（B1 channel+Room，domain 按 interface 写）；drift 列名 camelCase 升级为阶段 1 硬验收；删除多余 primaryKey override（autoIncrement 已含）；修正项目结构 LocalHttpServer.kt 描述与 lastError 误入时间戳说明 |

---

## 附录 C：drift 多 isolate 方案对比（§3.5 决策依据）

| 维度 | WAL 多连接（**采用**） | drift isolate 连接共享（不采用） |
|------|----------------------|------------------------------|
| 架构 | 每个 isolate 各开 db 连接，靠 SQLite WAL 并发 | DB 连接收进一个独立 isolate，其他 isolate 作客户端代理 |
| 实现复杂度 | 低（drift 默认 + 开 WAL） | 中（需 `DriftIsolate`、`_connect`、`ConnectionCoordinator`） |
| 跨连接实时通知 | ❌ 需自己用 platform channel/端口推送 | ✅ 天然单连接，stream 自动更新 |
| 性能 | 高（直接 SQL） | 多一层消息序列化开销 |
| 风险 | `SQLITE_BUSY`（WAL 下极低）、`fetchCount` 可见性 | headless engine 与 DB isolate 通信链路需稳定 |

**选 WAL 的理由**：实现简单、性能直接；`fetchCount` 跨连接可见性用一条 platform channel 通知即可解决（§3.5 方案 a）。若 POC 发现 `SQLITE_BUSY` 或通知链路不稳定，再切 isolate 连接共享。
