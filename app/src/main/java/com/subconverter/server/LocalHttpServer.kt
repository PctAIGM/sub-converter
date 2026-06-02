package com.subconverter.server

import com.subconverter.data.settings.ServerSettings
import com.subconverter.domain.OutputRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class LocalHttpServer(
    private val outputRepository: OutputRepository,
) {
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var activeSettings: ServerSettings = ServerSettings()
    private val _running = MutableStateFlow(false)

    val running: StateFlow<Boolean> = _running

    fun start(settings: ServerSettings) {
        stop()
        activeSettings = settings
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val bindAddress = InetAddress.getByName(if (settings.allowLan) "0.0.0.0" else "127.0.0.1")
        serverSocket = ServerSocket(settings.port, 50, bindAddress)
        _running.value = true

        scope.launch {
            while (isActive) {
                val socket = runCatching { serverSocket?.accept() }.getOrNull() ?: break
                launch { handle(socket) }
            }
            _running.value = false
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        scope.cancel()
        _running.value = false
    }

    private suspend fun handle(socket: Socket) = withContext(Dispatchers.IO) {
        socket.use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine().orEmpty()
            while (!reader.readLine().isNullOrBlank()) {
                // Drain request headers.
            }

            val parts = requestLine.split(' ')
            if (parts.size < 2) {
                writeResponse(client, 400, "text/plain; charset=utf-8", "Bad Request")
                return@withContext
            }

            val uri = runCatching { URI("http://localhost${parts[1]}") }.getOrNull()
            if (uri == null) {
                writeResponse(client, 400, "text/plain; charset=utf-8", "Bad Request")
                return@withContext
            }

            when {
                uri.path == "/health" -> {
                    writeResponse(
                        client,
                        200,
                        "application/json; charset=utf-8",
                        """{"status":"ok","running":true}""",
                    )
                }

                uri.path.startsWith("/subscriptions/") && uri.path.endsWith(".yaml") -> {
                    if (!isTokenAllowed(uri)) {
                        writeResponse(client, 401, "text/plain; charset=utf-8", "Unauthorized")
                        return@withContext
                    }

                    val id = uri.path
                        .removePrefix("/subscriptions/")
                        .removeSuffix(".yaml")
                        .toLongOrNull()

                    if (id == null) {
                        writeResponse(client, 404, "text/plain; charset=utf-8", "Not Found")
                        return@withContext
                    }

                    val rendered = outputRepository.renderProfile(id)
                    if (rendered == null) {
                        writeResponse(client, 404, "text/plain; charset=utf-8", "Not Found")
                        return@withContext
                    }

                    val headers = buildMap {
                        put("Profile-Update-Interval", rendered.updateIntervalHours.toString())
                        val userInfo = rendered.userInfo?.toHeaderValue().orEmpty()
                        if (userInfo.isNotBlank()) put("Subscription-Userinfo", userInfo)
                        rendered.profileTitle?.let { put("Profile-Title", it) }
                        rendered.profileWebPageUrl?.let { put("Profile-Web-Page-Url", it) }
                        put("Content-Disposition", "attachment; filename=\"${rendered.profileTitle ?: "config"}.yaml\"")
                    }
                    writeResponse(client, 200, "text/yaml; charset=utf-8", rendered.yamlBody, headers)
                }

                else -> writeResponse(client, 404, "text/plain; charset=utf-8", "Not Found")
            }
        }
    }

    private fun isTokenAllowed(uri: URI): Boolean {
        val requiredToken = activeSettings.token
        if (requiredToken.isBlank()) return true
        return parseQuery(uri.rawQuery)["token"] == requiredToken
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&').mapNotNull { pair ->
            val pieces = pair.split('=', limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            decode(pieces[0]) to decode(pieces[1])
        }.toMap()
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun writeResponse(
        socket: Socket,
        status: Int,
        contentType: String,
        body: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val output = socket.getOutputStream()
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n")
            extraHeaders.forEach { (name, value) -> append("$name: $value\r\n") }
            append("\r\n")
        }
        output.write(headers.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }
}
