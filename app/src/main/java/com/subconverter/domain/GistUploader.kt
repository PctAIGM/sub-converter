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
            val needFallback = patched == null || patched.message == NOT_FOUND_MESSAGE
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
        const val NOT_FOUND_MESSAGE = "HTTP 404"
    }
}
