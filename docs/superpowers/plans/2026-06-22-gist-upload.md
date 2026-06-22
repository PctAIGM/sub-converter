# Gist 上传功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增"把输出配置上传到 GitHub Gist"的能力——服务页配置 GitHub PAT，输出配置勾选开关，订阅刷新成功后自动上传，文件名以配置名 + `.yml` 命名，Secret Gist，首次创建后续 PATCH 更新。

**Architecture:** 新建纯领域服务 `GistUploader`（封装 GitHub Gist REST API，POST 创建 / PATCH 更新）；`OutputRepository` 新增 `uploadAffectedProfiles`，在 `SubscriptionRepository.refreshSource` 成功分支里调用；数据层加 Room 8→9 迁移（`output_profiles` 加 `uploadToGist`、`gistId` 两列）；服务设置加全局 `gistToken`；UI 在服务页加 token 输入、在输出编辑页加上传开关。

**Tech Stack:** Kotlin、Jetpack Compose、Room 2.8.4、DataStore Preferences、OkHttp 4.12.0、`org.json.JSONObject`（Android 平台自带，不引新库）、JUnit 4 + OkHttp MockWebServer（测试）。

**Spec:** `docs/superpowers/specs/2026-06-22-gist-upload-design.md`

**Key conventions (from AGENTS.md):**
- 改完代码用 `./gradlew assembleDebug` 验证。
- Room 加列必须建 `Migration(N, N+1)` 并在 `AppContainer` 注册。当前 DB version = 8。
- 无注释（除非要求）；遵循现有命名（`iOSXxx` UI helper、`SmallFormField` 字段）。

---

## File Structure

**新建：**
- `app/src/main/java/com/subconverter/domain/GistUploader.kt` — 纯领域服务，封装 Gist API（POST/PATCH），无 Android 依赖。
- `app/src/test/java/com/subconverter/domain/GistUploaderTest.kt` — MockWebServer 单元测试。

**修改：**
- `app/src/main/java/com/subconverter/data/Entities.kt` — `OutputProfileEntity` 加 `uploadToGist`、`gistId`。
- `app/src/main/java/com/subconverter/data/AppDatabase.kt` — version 9，`Migration8To9`。
- `app/src/main/java/com/subconverter/data/Daos.kt` — `OutputProfileDao` 加 `getAll()`。
- `app/src/main/java/com/subconverter/data/settings/ServerSettingsStore.kt` — `ServerSettings` 加 `gistToken`，store 读写。
- `app/src/main/java/com/subconverter/domain/Models.kt` — 新增 `GistResult`。
- `app/src/main/java/com/subconverter/domain/OutputRepository.kt` — 构造函数加 `settingsStore`、`gistUploader`；新增 `uploadAffectedProfiles`、`sanitizeFilename`。
- `app/src/main/java/com/subconverter/domain/SubscriptionRepository.kt` — 构造函数加 `outputRepository`；`refreshSource` 成功分支调用上传。
- `app/src/main/java/com/subconverter/core/AppContainer.kt` — 装配 `gistUploader`、调整依赖注入。
- `app/src/main/java/com/subconverter/ui/MainViewModel.kt` — `saveProfile` 加 `uploadToGist` 参数。
- `app/src/main/java/com/subconverter/ui/MainScreen.kt` — `OutputEditScreen` 加开关与回调签名；`ServerScreen` 加 token 字段；调用点接线。
- `app/build.gradle.kts` — 测试依赖加 `mockwebserver`。

---

## Task 1: 测试依赖与 GistResult 模型

**Files:**
- Modify: `app/build.gradle.kts:98`
- Modify: `app/src/main/java/com/subconverter/domain/Models.kt:58`

- [ ] **Step 1: 添加 MockWebServer 测试依赖**

修改 `app/build.gradle.kts`，把现有的 `testImplementation("junit:junit:4.13.2")` 那一行替换为两行：

```kotlin
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
```

- [ ] **Step 2: 在 `Models.kt` 末尾（文件最后一行 `}` 之后）追加 `GistResult` 与 `GistUploadSummary`**

在 `app/src/main/java/com/subconverter/domain/Models.kt` 文件末尾（`fun SubscriptionSourceEntity.userInfo()` 函数之后）追加：

```kotlin
data class GistResult(
    val success: Boolean,
    val gistId: String? = null,
    val rawUrl: String? = null,
    val message: String,
)

data class GistUploadSummary(
    val tokenMissing: Boolean,
    val attempted: Int = 0,
    val succeeded: Int = 0,
    val firstError: String? = null,
)
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/subconverter/domain/Models.kt
git commit -m "feat: add mockwebserver test dep and GistResult model"
```

---

## Task 2: GistUploader 领域服务（TDD）

**Files:**
- Create: `app/src/test/java/com/subconverter/domain/GistUploaderTest.kt`
- Create: `app/src/main/java/com/subconverter/domain/GistUploader.kt`

- [ ] **Step 1: 写失败测试**

创建 `app/src/test/java/com/subconverter/domain/GistUploaderTest.kt`：

```kotlin
package com.subconverter.domain

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GistUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var uploader: GistUploader

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        uploader = GistUploader(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createsGistWhenGistIdBlank() {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {"id":"abc123","files":{"My.yml":{"raw_url":"https://raw.example/My.yml"}}}
                    """.trimIndent(),
                ),
        )

        val result = uploader.upload(
            token = "tok",
            gistId = "",
            filename = "My.yml",
            content = "proxies: []",
        ).let { runCatching { it }.getOrElse { throw AssertionError(it) } }

        assertTrue(result.success)
        assertEquals("abc123", result.gistId)
        assertEquals("https://raw.example/My.yml", result.rawUrl)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/gists", recorded.path)
        val auth = recorded.getHeader("Authorization")
        assertEquals("Bearer tok", auth)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"public\":false"))
        assertTrue(body.contains("\"My.yml\""))
        assertTrue(body.contains("\"proxies: []\""))
    }

    @Test
    fun updatesGistWhenGistIdPresent() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"id":"abc123","files":{"My.yml":{"raw_url":"https://raw.example/My.yml"}}}
                    """.trimIndent(),
                ),
        )

        val result = uploader.upload(
            token = "tok",
            gistId = "abc123",
            filename = "My.yml",
            content = "proxies: []",
        )

        assertTrue(result.success)
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/gists/abc123", recorded.path)
    }

    @Test
    fun fallsBackToCreateOnPatch404() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"id":"newid","files":{"My.yml":{"raw_url":"u"}}}"""),
        )

        val result = uploader.upload("tok", "stale", "My.yml", "proxies: []")

        assertTrue(result.success)
        assertEquals("newid", result.gistId)
        assertEquals("PATCH", server.takeRequest().method)
        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun mapsUnauthorizedToMessage() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val result = uploader.upload("bad", "", "My.yml", "proxies: []")

        assertFalse(result.success)
        assertEquals("Gist Token 无效", result.message)
    }

    @Test
    fun mapsRateLimitToMessage() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))

        val result = uploader.upload("bad", "", "My.yml", "proxies: []")

        assertFalse(result.success)
        assertEquals("GitHub 限流，稍后再试", result.message)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.subconverter.domain.GistUploaderTest"`
Expected: FAIL，原因 `GistUploader` 不存在 / 无法解析。

- [ ] **Step 3: 写最小实现**

创建 `app/src/main/java/com/subconverter/domain/GistUploader.kt`：

```kotlin
package com.subconverter.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Duration

class GistUploader(
    private val baseUrl: String = "https://api.github.com/",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) {
    suspend fun upload(
        token: String,
        gistId: String,
        filename: String,
        content: String,
        public: Boolean = false,
    ): GistResult = withContext(Dispatchers.IO) {
        if (gistId.isNotBlank()) {
            val patched = runCatching { patch(token, gistId, filename, content) }.getOrNull()
            if (patched?.success == true) return@withContext patched
            val needFallback = patched == null || patched.message == HTTP_NOT_FOUND_MSG
            if (needFallback) {
                val created = runCatching { post(token, filename, content, public) }.getOrNull()
                return@withContext created ?: GistResult(success = false, message = "Gist 上传失败")
            }
            return@withContext patched ?: GistResult(success = false, message = "Gist 上传失败")
        }
        runCatching { post(token, filename, content, public) }.getOrElse {
            GistResult(success = false, message = it.message ?: "Gist 上传失败")
        }
    }

    private fun post(token: String, filename: String, content: String, public: Boolean): GistResult {
        val body = JSONObject()
            .put("description", "SubConverter")
            .put("public", public)
            .put(
                "files",
                JSONObject().put(filename, JSONObject().put("content", content)),
            ).toString()
        val request = baseRequest(token, "gists")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        return execute(request) { root ->
            GistResult(
                success = true,
                gistId = root.optString("id").takeIf { it.isNotBlank() },
                rawUrl = root.optJSONObject("files")?.optJSONObject(filename)?.optString("raw_url")?.takeIf { it.isNotBlank() },
                message = "Gist 已创建",
            )
        }
    }

    private fun patch(token: String, gistId: String, filename: String, content: String): GistResult {
        val body = JSONObject()
            .put(
                "files",
                JSONObject().put(filename, JSONObject().put("content", content)),
            ).toString()
        val request = baseRequest(token, "gists/$gistId")
            .patch(body.toRequestBody(JSON_MEDIA))
            .build()
        return execute(request) { root ->
            GistResult(
                success = true,
                gistId = root.optString("id").takeIf { it.isNotBlank() } ?: gistId,
                rawUrl = root.optJSONObject("files")?.optJSONObject(filename)?.optString("raw_url")?.takeIf { it.isNotBlank() },
                message = "Gist 已更新",
            )
        }
    }

    private fun baseRequest(token: String, path: String): Request.Builder =
        Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "SubConverter/0.1")

    private inline fun execute(request: Request, onSuccess: (JSONObject) -> GistResult): GistResult {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val root = raw.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()
                return onSuccess(root)
            }
            return GistResult(success = false, message = errorMessage(response.code))
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        401 -> "Gist Token 无效"
        403, 429 -> "GitHub 限流，稍后再试"
        else -> "HTTP $code"
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val HTTP_NOT_FOUND_MSG = "HTTP 404"
    }
}
```

**说明：** `patch` 返回 404 时，`execute` 会返回 `GistResult(message = "HTTP 404")`，`upload` 通过 `needFallback` 判断后回退 POST。其它非 404 的失败（如 401）原样返回。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.subconverter.domain.GistUploaderTest"`
Expected: 5 个测试全 PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subconverter/domain/GistUploader.kt app/src/test/java/com/subconverter/domain/GistUploaderTest.kt
git commit -m "feat: add GistUploader domain service with tests"
```

---

## Task 3: Room 迁移 8→9 与 OutputProfileEntity 新字段

**Files:**
- Modify: `app/src/main/java/com/subconverter/data/Entities.kt:74-87`
- Modify: `app/src/main/java/com/subconverter/data/AppDatabase.kt:8,76`

- [ ] **Step 1: 在 `OutputProfileEntity` 加两个字段**

修改 `app/src/main/java/com/subconverter/data/Entities.kt` 中 `OutputProfileEntity`，在 `fetchCount` 之后追加两行：

```kotlin
@Entity(tableName = "output_profiles")
data class OutputProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceIds: String,
    val templateId: Long,
    val enabled: Boolean = true,
    val prefix: String = "",
    val includeRegex: String = "",
    val excludeRegex: String = "",
    val overrideIds: String = "",
    val updateIntervalHours: Int = 12,
    val fetchCount: Long = 0,
    val uploadToGist: Boolean = false,
    val gistId: String = "",
)
```

- [ ] **Step 2: 在 `AppDatabase.kt` 加 `Migration8To9` 并把 version 改成 9**

修改 `app/src/main/java/com/subconverter/data/AppDatabase.kt`：

- `@Database(... version = 8 ...)` 改为 `version = 9`。
- 在 `Migration7To8` 之后（`companion object` 的 `}` 之前）追加：

```kotlin
        val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE output_profiles ADD COLUMN uploadToGist INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE output_profiles ADD COLUMN gistId TEXT NOT NULL DEFAULT ''")
            }
        }
```

- [ ] **Step 3: 在 `AppContainer` 注册新迁移**

修改 `app/src/main/java/com/subconverter/core/AppContainer.kt`，在 `.addMigrations(AppDatabase.Migration7To8)` 之后追加一行：

```kotlin
        .addMigrations(AppDatabase.Migration8To9)
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subconverter/data/Entities.kt app/src/main/java/com/subconverter/data/AppDatabase.kt app/src/main/java/com/subconverter/core/AppContainer.kt
git commit -m "feat: add uploadToGist and gistId columns (migration 8->9)"
```

---

## Task 4: OutputProfileDao.getAll() 与 ServerSettings.gistToken

**Files:**
- Modify: `app/src/main/java/com/subconverter/data/Daos.kt:76-94`
- Modify: `app/src/main/java/com/subconverter/data/settings/ServerSettingsStore.kt:16-67`

- [ ] **Step 1: 在 `OutputProfileDao` 加 `getAll()`**

修改 `app/src/main/java/com/subconverter/data/Daos.kt`，在 `OutputProfileDao` 接口的 `observeAll()` 之后追加：

```kotlin
    @Query("SELECT * FROM output_profiles ORDER BY id DESC")
    suspend fun getAll(): List<OutputProfileEntity>
```

- [ ] **Step 2: 在 `ServerSettings` 加 `gistToken` 字段**

修改 `app/src/main/java/com/subconverter/data/settings/ServerSettingsStore.kt`，data class 末尾加字段：

```kotlin
data class ServerSettings(
    val enabled: Boolean = false,
    val autoStartOnBoot: Boolean = false,
    val allowLan: Boolean = false,
    val port: Int = 9876,
    val token: String = "",
    val globalUserAgent: String = SubscriptionFetcher.DEFAULT_USER_AGENT,
    val gistToken: String = "",
)
```

- [ ] **Step 3: store 的 settings 读取 + update 写入 + Keys**

在 `settings` 的 map 块末尾（`globalUserAgent = ...` 之后）追加：

```kotlin
            gistToken = preferences[Keys.GistToken].orEmpty(),
```

在 `update` 函数末尾（`preferences[Keys.GlobalUserAgent] = ...` 之后）追加：

```kotlin
            preferences[Keys.GistToken] = settings.gistToken.trim()
```

在 `Keys` object 末尾（`GlobalUserAgent` 之后）追加：

```kotlin
        val GistToken = stringPreferencesKey("gist_token")
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subconverter/data/Daos.kt app/src/main/java/com/subconverter/data/settings/ServerSettingsStore.kt
git commit -m "feat: add OutputProfileDao.getAll and gistToken setting"
```

---

## Task 5: OutputRepository.uploadAffectedProfiles

**Files:**
- Modify: `app/src/main/java/com/subconverter/domain/OutputRepository.kt:12-19,171-172`

- [ ] **Step 1: 构造函数加 `settingsStore`、`gistUploader` 依赖**

修改 `app/src/main/java/com/subconverter/domain/OutputRepository.kt` 的构造函数：

```kotlin
class OutputRepository(
    private val sourceDao: SubscriptionSourceDao,
    private val nodeDnsCacheDao: NodeDnsCacheDao,
    private val templateDao: TemplateDao,
    private val outputDao: OutputProfileDao,
    private val yamlService: MihomoYamlService,
    private val remoteTextFetcher: RemoteTextFetcher,
    private val settingsStore: com.subconverter.data.settings.ServerSettingsStore,
    private val gistUploader: GistUploader,
) {
```

并新增 import：

```kotlin
import com.subconverter.data.settings.ServerSettingsStore
```

（构造函数里用短名 `ServerSettingsStore` 即可，把 `com.subconverter.data.settings.` 前缀去掉。）

- [ ] **Step 2: 把 `parseIds` 改为 internal，新增 `uploadAffectedProfiles` 与 `sanitizeFilename`**

修改 `OutputRepository.kt`：

把现有 `private fun parseIds(rawIds: String): List<Long>` 改为：

```kotlin
    internal fun parseIds(rawIds: String): List<Long> =
        rawIds.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()
```

在 `renderProfile` 函数之后（`aggregateUserInfo` 之前）追加：

```kotlin
    suspend fun uploadAffectedProfiles(sourceId: Long): GistUploadSummary {
        val gistToken = settingsStore.current().gistToken
        if (gistToken.isBlank()) return GistUploadSummary(tokenMissing = true)
        var attempted = 0
        var succeeded = 0
        var firstError: String? = null
        outputDao.getAll()
            .asSequence()
            .filter { it.enabled && it.uploadToGist && sourceId in parseIds(it.sourceIds) }
            .forEach { profile ->
                runCatching {
                    val rendered = renderProfile(profile.id) ?: return@runCatching
                    val filename = "${sanitizeFilename(profile.name)}.yml"
                    val result = gistUploader.upload(
                        token = gistToken,
                        gistId = profile.gistId,
                        filename = filename,
                        content = rendered.yamlBody,
                    )
                    attempted++
                    if (result.success) {
                        succeeded++
                        if (result.gistId != null && result.gistId != profile.gistId) {
                            outputDao.update(profile.copy(gistId = result.gistId))
                        }
                    } else if (firstError == null) {
                        firstError = result.message
                    }
                }
            }
        return GistUploadSummary(
            tokenMissing = false,
            attempted = attempted,
            succeeded = succeeded,
            firstError = firstError,
        )
    }

    private fun sanitizeFilename(name: String): String =
        name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "config" }
```

- [ ] **Step 3: 验证编译（预期会因为构造函数变更导致 AppContainer 报错，下一步修复）**

Run: `./gradlew assembleDebug`
Expected: 编译错误（`AppContainer` 构造 `OutputRepository` 时缺参数）。这是预期的，下一 Task 修复。

- [ ] **Step 4: Commit（允许中间状态，注释说明下一步修复）**

```bash
git add app/src/main/java/com/subconverter/domain/OutputRepository.kt
git commit -m "feat: add uploadAffectedProfiles and sanitizeFilename"
```

---

## Task 6: SubscriptionRepository 串接 + AppContainer 装配

**Files:**
- Modify: `app/src/main/java/com/subconverter/domain/SubscriptionRepository.kt:8-15,116`
- Modify: `app/src/main/java/com/subconverter/core/AppContainer.kt:36,49-58`

- [ ] **Step 1: `SubscriptionRepository` 构造函数加 `outputRepository`**

修改 `app/src/main/java/com/subconverter/domain/SubscriptionRepository.kt` 构造函数：

```kotlin
class SubscriptionRepository(
    private val dao: SubscriptionSourceDao,
    private val nodeDnsCacheDao: NodeDnsCacheDao,
    private val fetcher: SubscriptionFetcher,
    private val yamlService: MihomoYamlService,
    private val nodePreResolver: NodePreResolver,
    private val refreshScheduler: RefreshScheduler,
    private val outputRepository: OutputRepository,
) {
```

- [ ] **Step 2: 在 `refreshSource` 成功分支末尾串接上传，并把结果合并进 message**

在 `refreshSource` 的成功分支里，把现有的：

```kotlin
            val message = nodeResolution?.let {
                if (it.failureCount == 0) {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount}"
                } else {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount + it.failureCount}（${it.failureCount} 失败）"
                }
            } ?: "刷新成功"
            RefreshOutcome(sourceId, success = true, message = message)
        }.getOrElse { throwable ->
```

改为：

```kotlin
            val baseMessage = nodeResolution?.let {
                if (it.failureCount == 0) {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount}"
                } else {
                    "刷新成功，节点解析 ${it.successCount}/${it.successCount + it.failureCount}（${it.failureCount} 失败）"
                }
            } ?: "刷新成功"
            val summary = runCatching { outputRepository.uploadAffectedProfiles(sourceId) }
                .getOrElse { GistUploadSummary(tokenMissing = true) }
            val message = baseMessage + gistSuffix(summary)
            RefreshOutcome(sourceId, success = true, message = message)
        }.getOrElse { throwable ->
```

并在 `SubscriptionRepository` 类末尾（最后一个 `}` 之前）新增私有 helper：

```kotlin
    private fun gistSuffix(summary: GistUploadSummary): String = when {
        summary.tokenMissing -> ""
        summary.attempted == 0 -> ""
        summary.succeeded == summary.attempted -> " · Gist 已更新"
        summary.succeeded == 0 && summary.firstError != null -> " · Gist 上传失败: ${summary.firstError}"
        else -> " · Gist 部分成功 ${summary.succeeded}/${summary.attempted}"
    }
```

新增 import（文件顶部）：

```kotlin
import com.subconverter.domain.GistUploadSummary
```

（注：`GistUploadSummary` 与 `SubscriptionRepository` 同包 `com.subconverter.domain`，所以实际上不需要 import——如果编译器警告未使用则去掉该 import。优先尝试不写 import，编译报错时再加。）

- [ ] **Step 3: `AppContainer` 装配**

修改 `app/src/main/java/com/subconverter/core/AppContainer.kt`：

- 新增 import：`import com.subconverter.domain.GistUploader`
- 在 `val remoteTextFetcher = RemoteTextFetcher()` 之后追加：

```kotlin
    val gistUploader = GistUploader()
```

- 把 `subscriptionRepository` 和 `outputRepository` 改为（注意顺序：output 必须先于 subscription 实例化，因为 subscription 持有 output 引用）：

```kotlin
    val outputRepository = OutputRepository(
        sourceDao = database.subscriptionSourceDao(),
        nodeDnsCacheDao = database.nodeDnsCacheDao(),
        templateDao = database.templateDao(),
        outputDao = database.outputProfileDao(),
        yamlService = yamlService,
        remoteTextFetcher = remoteTextFetcher,
        settingsStore = settingsStore,
        gistUploader = gistUploader,
    )

    val subscriptionRepository = SubscriptionRepository(
        dao = database.subscriptionSourceDao(),
        nodeDnsCacheDao = database.nodeDnsCacheDao(),
        fetcher = subscriptionFetcher,
        yamlService = yamlService,
        nodePreResolver = nodePreResolver,
        refreshScheduler = refreshScheduler,
        outputRepository = outputRepository,
    )
```

（注意把原来先声明 `subscriptionRepository` 后声明 `outputRepository` 的顺序对调。）

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subconverter/domain/SubscriptionRepository.kt app/src/main/java/com/subconverter/core/AppContainer.kt
git commit -m "feat: wire Gist upload into subscription refresh"
```

---

## Task 7: MainViewModel.saveProfile 加 uploadToGist 参数

**Files:**
- Modify: `app/src/main/java/com/subconverter/ui/MainViewModel.kt:259-310`

- [ ] **Step 1: `saveProfile` 加参数并写回 entity**

修改 `app/src/main/java/com/subconverter/ui/MainViewModel.kt` 中的 `saveProfile`：

```kotlin
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
                messages.value = "输出名称和订阅源不能为空"
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
            messages.value = "输出配置已保存"
        }
    }
```

- [ ] **Step 2: `addProfile` 加参数并转发**

把 `MainViewModel.kt` 里的 `addProfile` 改为：

```kotlin
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
```

- [ ] **Step 3: 验证编译（预期会因为 UI 调用点签名不匹配报错，下一 Task 修复）**

Run: `./gradlew assembleDebug`
Expected: 编译错误（`MainScreen` 调用 `saveProfile` 缺 `uploadToGist` 参数）。预期，下一 Task 修复。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/subconverter/ui/MainViewModel.kt
git commit -m "feat: add uploadToGist param to saveProfile"
```

---

## Task 8: UI — OutputEditScreen 加上传开关

**Files:**
- Modify: `app/src/main/java/com/subconverter/ui/MainScreen.kt:320-329,837-872,949-954`

- [ ] **Step 1: 给 `iOSFormSwitch` 加 `subtitleColor` 参数**

修改 `app/src/main/java/com/subconverter/ui/MainScreen.kt` 中 `iOSFormSwitch`（约 2401 行）的签名与 subtitle 渲染：

```kotlin
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
```

- [ ] **Step 2: `OutputEditScreen` 加 `gistToken` 入参、开关状态与 UI**

修改 `OutputEditScreen`（约 837 行）：

签名加 `gistToken: String` 与 `onConfirm` 多一个 `Boolean`：

```kotlin
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
```

（注意：保留 `OutputEditScreen` 中所有原有 import；新增使用 `Color` 与 `MaterialTheme.colorScheme.error`，确认文件顶部已 import `androidx.compose.ui.graphics.Color`——若已有则无需重复。）

- [ ] **Step 3: 更新 `OutputEditScreen` 调用点**

修改 `MainScreen.kt` 约 320 行的调用点：

```kotlin
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
```

- [ ] **Step 4: 确认 `Color` import 存在**

在 `MainScreen.kt` 顶部 import 区确认有 `import androidx.compose.ui.graphics.Color`。如果没有则添加。

- [ ] **Step 5: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/subconverter/ui/MainScreen.kt
git commit -m "feat: add uploadToGist switch to OutputEditScreen"
```

---

## Task 9: UI — ServerScreen 加 Gist Token 字段

**Files:**
- Modify: `app/src/main/java/com/subconverter/ui/MainScreen.kt:2165-2274`

- [ ] **Step 1: 在 `ServerScreen` 加 `gistToken` 本地状态**

修改 `app/src/main/java/com/subconverter/ui/MainScreen.kt` 的 `ServerScreen`（约 2165 行）：

在 `var globalUserAgent by rememberSaveable(settings.globalUserAgent) { mutableStateOf(settings.globalUserAgent) }` 之后追加：

```kotlin
    var gistToken by rememberSaveable(settings.gistToken) { mutableStateOf(settings.gistToken) }
```

把 `previewSettings` 的 `ServerSettings(...)` 构造里追加 `gistToken = gistToken`：

```kotlin
    val previewSettings = settings.copy(
        port = port.toIntOrNull() ?: settings.port,
        token = token,
        allowLan = allowLan,
        autoStartOnBoot = autoStartOnBoot,
        globalUserAgent = globalUserAgent,
        gistToken = gistToken,
    )
```

- [ ] **Step 2: 启停 toggle 里的 `ServerSettings(...)` 构造补字段**

把 `ServerStatusCard` 的 `onToggle` lambda 里的 `ServerSettings(...)` 改为：

```kotlin
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
```

- [ ] **Step 3: 新增 Gist Token 卡片**

在 `ServerScreen` 的 `LazyColumn` 里，"订阅拉取 User-Agent" 卡片 item 之后、"保存" 按钮 item 之前，插入一个新 item：

```kotlin
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
```

- [ ] **Step 4: 确认新 import**

在 `MainScreen.kt` 顶部 import 区确认有以下 import，缺则补：

```kotlin
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
```

（若文件已 import 这些则跳过。）

- [ ] **Step 5: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/subconverter/ui/MainScreen.kt
git commit -m "feat: add Gist Token field to ServerScreen"
```

---

## Task 10: 全量编译 + 单测 + 手动验证清单

**Files:** 无（验证 Task）

- [ ] **Step 1: 全量编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 跑全部单测**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS（含原有测试与 `GistUploaderTest` 的 5 个）。

- [ ] **Step 3: 手动验证清单**

安装 Debug APK 到设备/模拟器，按以下步骤验证：

1. 进入"服务"页，填入一个有 `gist` 权限的 GitHub PAT 到 "GitHub Gist Token" 字段，点保存。确认保存后再进服务页 token 仍在。
2. 进入"输出"，编辑一个输出配置，勾选"上传到 Gist"，保存。
3. 在"订阅源"页点该输出关联的订阅源的刷新按钮。
4. 刷新成功后，登录 GitHub，检查账号下出现一个 Secret Gist，文件名为 `<配置名>.yml`，内容是渲染后的 yaml。
5. 再次刷新订阅，确认是更新同一个 Gist（GitHub Gist 列表里 gist 数量没增加，原 Gist 的 updated 时间变化）。
6. 在 GitHub 手动删除该 Gist，再次刷新订阅，确认自动回退创建新 Gist 并写入新 gistId（再次刷新不会重复创建）。
7. 在服务页清空 Gist Token 保存，回到订阅源刷新，确认刷新正常完成、不报错、不上传。

- [ ] **Step 4: 最终 commit（如手动验证中修了 bug）**

若手动验证中发现并修复了问题，按修复内容提交。否则跳过本步。

---

## 完成标准

- `./gradlew assembleDebug` 通过。
- `./gradlew :app:testDebugUnitTest` 全绿。
- 手动验证清单 1–7 全部通过。
- Spec（`docs/superpowers/specs/2026-06-22-gist-upload-design.md`）中的所有功能点均有对应 Task 实现。
