# Gist 上传功能设计

日期：2026-06-22
状态：待实现

## 目标

在 sub-converter 中新增"把输出配置上传到 GitHub Gist"的能力。用户在服务页配置 GitHub PAT，在每个输出配置里勾选"上传到 Gist"，订阅源刷新成功后自动把渲染好的 yaml 上传到 Gist，文件名以配置名 + `.yml` 命名。

## 范围与非目标

- **范围内**：Gist token 全局存储；输出配置级开关；刷新订阅后自动上传；首次创建、后续更新同一个 Gist；Secret Gist。
- **非目标**：不上传到其它代码托管平台；不做 Gist 列表管理 / 删除；不做 token 加密存储（沿用项目现有明文 DataStore 约定）；不提供独立的"立即上传"按钮；不在输出列表项展示上传状态标记。

## 关键决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 上传时机 | 每次刷新订阅成功后自动上传 | 用户明确要求 |
| Token 存放 | 服务设置页（全局唯一） | 多输出共用同一账号 |
| 开关粒度 | 每个输出配置一个 `uploadToGist` 开关 | 用户明确要求 |
| Gist 可见性 | Secret | 含代理节点信息，不公开 |
| Gist 复用 | 首次创建，后续 PATCH 更新 | URL 稳定、不耗配额 |
| 实现层 | 新建 `GistUploader` 领域服务 + Repository 串接 | 符合现有 MVVM 分层，可单测 |

## 架构与数据流

```
[订阅源刷新成功]
      │
      ▼
SubscriptionRepository.refreshSource()
      │ (成功分支)
      ▼
OutputRepository.uploadAffectedProfiles(sourceId)   ← 新增
      │
      ├─ 查询 enabled && uploadToGist && sourceIds 包含 sourceId 的 profiles
      ▼ 对每个 profile:
      ├─ renderProfile(profile.id)   ← 复用现有渲染
      ▼
      ├─ GistUploader.upload(token, profile.gistId, "${name}.yml", yaml, public=false)
      ▼
      └─ 首次返回 gistId → 回填 profile.gistId；后续 PATCH 更新同一 Gist
```

要点：
- `GistUploader` 是纯领域服务，不依赖 Android Context。
- `uploadAffectedProfiles` 在刷新成功后调用；上传失败不阻断刷新主流程（catch 后忽略，仅 WorkManager 后台场景记日志）。
- 上传结果通过 ViewModel 的 `messages` 通道反馈给用户；不上传状态到独立 DB 列。

## 数据模型变更

### Room 迁移 8 → 9

`output_profiles` 表新增两列：

| 列名 | 类型 | 默认 | 用途 |
|---|---|---|---|
| `uploadToGist` | INTEGER (Boolean) | 0 | 是否启用上传 |
| `gistId` | TEXT | `''` | 首次上传成功后回填的 Gist ID |

```kotlin
val Migration8To9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE output_profiles ADD COLUMN uploadToGist INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE output_profiles ADD COLUMN gistId TEXT NOT NULL DEFAULT ''")
    }
}
```

`AppDatabase.version = 9`；`AppContainer` 注册 `Migration8To9`。

### `OutputProfileEntity` 新增字段

```kotlin
val uploadToGist: Boolean = false,
val gistId: String = "",
```

### `ServerSettings` 新增字段

```kotlin
val gistToken: String = "",
```

### `ServerSettingsStore`

新增 DataStore key `gist_token`，读写仿照现有 `token`（trim 保存，空串默认）。沿用项目现有明文 DataStore 约定（不引入 EncryptedSharedPreferences）。

### 新增模型 `GistResult`

```kotlin
data class GistResult(
    val success: Boolean,
    val gistId: String? = null,   // 首次创建时返回
    val rawUrl: String? = null,   // raw URL
    val message: String,
)
```

## 组件设计

### `GistUploader`（新文件 `domain/GistUploader.kt`）

```kotlin
class GistUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) {
    suspend fun upload(
        token: String,
        gistId: String,      // 空=创建，非空=更新
        filename: String,    // e.g. "MyOutput.yml"
        content: String,
        public: Boolean = false,
    ): GistResult
}
```

**API 调用**：

| 操作 | URL | 方法 | 请求体关键字段 |
|---|---|---|---|
| 创建 | `https://api.github.com/gists` | POST | `{"description":"SubConverter","public":false,"files":{"<filename>":{"content":"<yaml>"}}}` |
| 更新 | `https://api.github.com/gists/{gistId}` | PATCH | `{"files":{"<filename>":{"content":"<yaml>"}}}` |

- 请求头：`Authorization: Bearer <token>`、`Accept: application/vnd.github+json`、`X-GitHub-Api-Version: 2022-11-28`、`User-Agent: SubConverter/0.1`。
- 响应解析：用 `org.json.JSONObject` 拿 `id` 和 `files[filename].raw_url`（不引新 JSON 库）。
- 错误处理：
  - 401 → "Gist Token 无效"
  - 403/429 → "GitHub 限流，稍后再试"
  - 404（PATCH 时）→ Gist 已被删除，回退为 POST 创建
  - 其它非 2xx → "HTTP {code}"
  - 网络异常 → 异常 message
  - 统一映射成 `GistResult(success=false, message=...)`
- 走 `withContext(Dispatchers.IO)`，风格与 `RemoteTextFetcher` 一致。

### `OutputRepository` 新增方法

```kotlin
suspend fun uploadAffectedProfiles(sourceId: Long) {
    val gistToken = settingsStore.current().gistToken
    if (gistToken.isBlank()) return  // token 缺失，静默跳过
    outputDao.getAll()
        .filter { it.enabled && it.uploadToGist && sourceId in parseIds(it.sourceIds) }
        .forEach { profile ->
            val rendered = renderProfile(profile.id) ?: return@forEach
            val filename = "${sanitize(profile.name)}.yml"
            val result = gistUploader.upload(gistToken, profile.gistId, filename, rendered.yamlBody)
            if (result.success && result.gistId != null && result.gistId != profile.gistId) {
                outputDao.update(profile.copy(gistId = result.gistId))
            }
        }
}
```

- `OutputRepository` 构造函数新增两个依赖：`settingsStore: ServerSettingsStore`、`gistUploader: GistUploader`。
- `sanitize(name)`：把 `/\:*?"<>|` 等非法文件名字符替换为 `_`，避免 GitHub 拒绝。
- 单个 profile 上传失败不影响其它 profile（每个 forEach 项独立 try/catch）。

### `SubscriptionRepository` 串接上传

`SubscriptionRepository` 构造函数新增 `outputRepository: OutputRepository` 依赖（检查后无循环依赖）。在 `refreshSource()` 成功分支末尾调用：

```kotlin
if (outcome.success) {
    runCatching { outputRepository.uploadAffectedProfiles(sourceId) }
        .onFailure { /* 记录但不阻断刷新结果 */ }
}
return outcome
```

这样 WorkManager 后台定时刷新（`RefreshSubscriptionWorker`）和 UI 手动刷新都会自然触发上传。

### `AppContainer` 装配

- 新增 `val gistUploader = GistUploader()`。
- `outputRepository` 构造时注入 `settingsStore` 与 `gistUploader`。
- `subscriptionRepository` 构造时注入 `outputRepository`。

## UI 变更

### 服务页（`ServerScreen`）加 Gist Token 字段

在现有"端口 / 访问 Token"卡片组下方新增一张卡片，仿照现有 `globalUserAgent` 的 `OutlinedTextField` 风格：

```
┌─────────────────────────────────────┐
│  GitHub Gist Token                  │
│  [________________________________] │  ← OutlinedTextField，单行
│  上传配置到 Gist 用的个人访问令牌，   │
│  需 gist 权限。留空则不开启上传。     │
└─────────────────────────────────────┘
```

变更点：
- `ServerScreen` 新增 `var gistToken by rememberSaveable(settings.gistToken) { mutableStateOf(settings.gistToken) }`。
- `previewSettings` 与所有 `ServerSettings(...)` 构造点（启停 toggle、保存按钮）都补上 `gistToken = gistToken`，避免保存时丢字段。

### 输出编辑页（`OutputEditScreen`）加上传开关

在现有"更新间隔"卡片下方新增一张卡片，用项目现有 `iOSFormSwitch`（与"允许局域网访问""开机自启动"同款）：

```
┌─────────────────────────────────────┐
│  上传到 Gist                        │
│  刷新订阅后自动上传到 GitHub Gist   │  ← iOSFormSwitch
│                              [○─]   │
└─────────────────────────────────────┘
```

变更点：
- `onConfirm` 回调签名扩展一个 `Boolean`：
  ```kotlin
  onConfirm: (OutputProfileEntity?, String, List<Long>, List<Long>, Int, Boolean) -> Unit
  ```
- 新增 `var uploadToGist by rememberSaveable(profile?.id) { mutableStateOf(profile?.uploadToGist ?: false) }`。
- 卡片内加 `iOSFormSwitch("上传到 Gist", "刷新订阅后自动上传到 GitHub Gist", uploadToGist, { uploadToGist = it })`。
- 当 token 未配置时，开关副标题改为红色提示 `"未配置 Gist Token（去服务页设置）"`，但允许勾选（保存不报错，运行时静默跳过）。

### `MainViewModel.saveProfile` 扩展

新增 `uploadToGist: Boolean` 参数，写回 entity。**`gistId` 不通过 UI 编辑**——由 `OutputRepository.uploadAffectedProfiles` 在首次上传成功后自动回填；编辑保存时保留原值（`existing?.gistId ?: ""`）。

### 输出列表项

不变更（不加任何 Gist 状态标记）。

## 错误处理

| 场景 | 行为 |
|---|---|
| Gist token 未配置 | `uploadAffectedProfiles` 直接 return；UI 手动刷新照常返回订阅刷新结果，不打扰 |
| 单个 profile 上传失败 | 该 profile 跳过（gistId 不变），不影响其它 profile 与刷新主流程 |
| PATCH 返回 404 | 回退为 POST 创建新 Gist，回填新 gistId |
| WorkManager 后台刷新触发上传失败 | 静默忽略（不写 message，不打扰用户） |
| UI 手动刷新触发上传 | 通过 ViewModel `messages` 通道合并反馈：`"订阅刷新成功 · Gist 已更新"` 或 `"订阅刷新成功 · Gist 上传失败: <error>"` |

## 测试

### 单元测试（`GistUploaderTest`）

用 OkHttp 的 `MockWebServer` 验证：
- 创建成功：返回 200 + JSON → `GistResult(success=true, gistId=..., rawUrl=...)`，请求体含正确 `files` 结构和 `public=false`。
- 更新成功：传入非空 gistId，验证发的是 PATCH 到 `/gists/{id}`。
- 401 token 无效 → `success=false, message="Gist Token 无效"`。
- PATCH 返回 404 → 自动回退 POST 创建。
- 网络异常（MockWebServer 关闭）→ `success=false`。

### 手动验证

1. 服务页填一个有 `gist` 权限的 PAT。
2. 新建/编辑一个输出配置，勾选"上传到 Gist"。
3. 触发订阅刷新（手动或等待定时）。
4. 检查 GitHub 账号下出现一个 Secret Gist，文件名为 `<配置名>.yml`，内容是渲染后的 yaml。
5. 再次刷新订阅，确认 Gist 被更新（同一个 gistId，内容刷新，没有新 Gist）。
6. 删除该 Gist 后再刷新，确认自动回退创建新 Gist 并回填 gistId。

## 实现顺序（供 writing-plans 参考）

1. 数据层：`Migration8To9`、`OutputProfileEntity` 字段、`ServerSettings`/`Store` 的 `gistToken`、`GistResult` 模型。
2. `GistUploader` 领域服务 + 单元测试。
3. `OutputRepository.uploadAffectedProfiles`、构造函数注入、`SubscriptionRepository` 串接、`AppContainer` 装配。
4. UI：`ServerScreen` token 字段、`OutputEditScreen` 开关、`MainViewModel.saveProfile` 签名扩展、`onConfirm` 回调接线。
5. 手动验证 + `./gradlew assembleDebug` 通过。
