package com.subconverter.domain

import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.IDN
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal class EncryptedDns(
    private val config: SubscriptionDnsConfig,
    private val bootstrapClient: OkHttpClient,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        config.validate()?.let { throw UnknownHostException(it) }
        val protocol = config.protocol ?: return Dns.SYSTEM.lookup(hostname)
        val ipv4 = runCatching { query(hostname, TYPE_A, protocol) }
        val ipv6 = runCatching { query(hostname, TYPE_AAAA, protocol) }
        val addresses = ipv4.getOrDefault(emptyList()) + ipv6.getOrDefault(emptyList())
        if (addresses.isNotEmpty()) return addresses

        val cause = ipv4.exceptionOrNull() ?: ipv6.exceptionOrNull()
        throw UnknownHostException(
            cause?.message ?: "指定 DNS 未返回 $hostname 的 IP 地址",
        ).apply {
            cause?.let(::initCause)
        }
    }

    private fun query(hostname: String, type: Int, protocol: DnsProtocol): List<InetAddress> {
        val id = NEXT_ID.getAndIncrement() and 0xffff
        val query = DnsMessageCodec.createQuery(hostname, type, id)
        val response = when (protocol) {
            DnsProtocol.DOH -> queryDoh(query)
            DnsProtocol.DOT -> queryDot(query)
        }
        return DnsMessageCodec.parseResponse(response, id, type)
    }

    private fun queryDoh(query: ByteArray): ByteArray {
        val endpoint = requireNotNull(config.server.trim().toHttpUrlOrNull()) {
            "DoH 地址格式无效"
        }
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", DNS_MESSAGE_MEDIA_TYPE.toString())
            .post(query.toRequestBody(DNS_MESSAGE_MEDIA_TYPE))
            .build()

        bootstrapClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UnknownHostException("DoH 请求失败: HTTP ${response.code}")
            }
            return response.body?.bytes()
                ?: throw UnknownHostException("DoH 响应为空")
        }
    }

    private fun queryDot(query: ByteArray): ByteArray {
        val endpoint = DotEndpoint.parse(config.server)
        val addresses = runCatching { Dns.SYSTEM.lookup(endpoint.host) }
            .getOrElse {
                throw UnknownHostException("无法解析 DoT 服务器 ${endpoint.host}: ${it.message}").apply {
                    initCause(it)
                }
            }
            .sortedBy { if (it is Inet4Address) 0 else 1 }

        var lastError: Throwable? = null
        for (address in addresses) {
            try {
                return queryDotAddress(endpoint, address, query)
            } catch (throwable: Exception) {
                lastError = throwable
            }
        }
        throw UnknownHostException("DoT 请求失败: ${lastError?.message ?: endpoint.host}").apply {
            lastError?.let(::initCause)
        }
    }

    private fun queryDotAddress(
        endpoint: DotEndpoint,
        address: InetAddress,
        query: ByteArray,
    ): ByteArray {
        val socket = Socket()
        socket.connect(InetSocketAddress(address, endpoint.port), CONNECT_TIMEOUT_MILLIS)
        val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val tlsSocket = sslSocketFactory
            .createSocket(socket, endpoint.host, endpoint.port, true) as SSLSocket
        tlsSocket.use {
            it.soTimeout = READ_TIMEOUT_MILLIS
            it.sslParameters = it.sslParameters.apply {
                endpointIdentificationAlgorithm = "HTTPS"
            }
            it.startHandshake()
            DataOutputStream(it.outputStream).use { output ->
                output.writeShort(query.size)
                output.write(query)
                output.flush()
                val input = DataInputStream(it.inputStream)
                val length = input.readUnsignedShort()
                require(length in 12..MAX_DNS_MESSAGE_SIZE) { "DoT 响应长度无效" }
                return ByteArray(length).also(input::readFully)
            }
        }
    }

    companion object {
        private val DNS_MESSAGE_MEDIA_TYPE = "application/dns-message".toMediaType()
        private val NEXT_ID = AtomicInteger((System.nanoTime() and 0xffff).toInt())
        private const val TYPE_A = 1
        private const val TYPE_AAAA = 28
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        private const val MAX_DNS_MESSAGE_SIZE = 65_535
    }
}

internal object DnsMessageCodec {
    fun createQuery(hostname: String, type: Int, id: Int): ByteArray {
        require(type == 1 || type == 28) { "不支持的 DNS 查询类型" }
        val asciiName = IDN.toASCII(hostname.trim().removeSuffix("."))
        require(asciiName.isNotEmpty()) { "域名不能为空" }
        val labels = asciiName.split('.')
        require(labels.all { it.isNotEmpty() && it.toByteArray(Charsets.US_ASCII).size <= 63 }) {
            "域名格式无效"
        }

        val nameSize = labels.sumOf { 1 + it.toByteArray(Charsets.US_ASCII).size } + 1
        val message = ByteArray(12 + nameSize + 4)
        writeU16(message, 0, id)
        writeU16(message, 2, 0x0100)
        writeU16(message, 4, 1)
        var offset = 12
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            message[offset++] = bytes.size.toByte()
            bytes.copyInto(message, offset)
            offset += bytes.size
        }
        message[offset++] = 0
        writeU16(message, offset, type)
        writeU16(message, offset + 2, 1)
        return message
    }

    fun parseResponse(message: ByteArray, expectedId: Int, expectedType: Int): List<InetAddress> {
        require(message.size >= 12) { "DNS 响应过短" }
        require(readU16(message, 0) == expectedId) { "DNS 响应 ID 不匹配" }
        val flags = readU16(message, 2)
        require(flags and 0x8000 != 0) { "DNS 响应标记无效" }
        require(flags and 0x0200 == 0) { "DNS 响应被截断" }
        val responseCode = flags and 0x000f
        require(responseCode == 0) { "DNS 解析失败: ${responseCodeText(responseCode)}" }

        val questionCount = readU16(message, 4)
        val answerCount = readU16(message, 6)
        var offset = 12
        repeat(questionCount) {
            offset = skipName(message, offset)
            require(offset + 4 <= message.size) { "DNS 问题区损坏" }
            offset += 4
        }

        val addresses = mutableListOf<InetAddress>()
        repeat(answerCount) {
            offset = skipName(message, offset)
            require(offset + 10 <= message.size) { "DNS 回答区损坏" }
            val type = readU16(message, offset)
            val dnsClass = readU16(message, offset + 2)
            val dataLength = readU16(message, offset + 8)
            offset += 10
            require(offset + dataLength <= message.size) { "DNS 记录数据损坏" }
            if (dnsClass == 1 && type == expectedType &&
                ((type == 1 && dataLength == 4) || (type == 28 && dataLength == 16))
            ) {
                addresses += InetAddress.getByAddress(message.copyOfRange(offset, offset + dataLength))
            }
            offset += dataLength
        }
        return addresses
    }

    private fun skipName(message: ByteArray, start: Int): Int {
        var offset = start
        var labels = 0
        while (true) {
            require(offset < message.size) { "DNS 名称越界" }
            val length = message[offset].toInt() and 0xff
            when {
                length == 0 -> return offset + 1
                length and 0xc0 == 0xc0 -> {
                    require(offset + 1 < message.size) { "DNS 压缩指针损坏" }
                    return offset + 2
                }
                length > 63 -> error("DNS 标签长度无效")
                else -> {
                    offset += length + 1
                    require(offset <= message.size) { "DNS 标签越界" }
                    labels++
                    require(labels <= 127) { "DNS 名称过长" }
                }
            }
        }
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int {
        require(offset + 2 <= bytes.size) { "DNS 数据越界" }
        return ((bytes[offset].toInt() and 0xff) shl 8) or
            (bytes[offset + 1].toInt() and 0xff)
    }

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    private fun responseCodeText(code: Int): String = when (code) {
        1 -> "格式错误"
        2 -> "服务器失败"
        3 -> "域名不存在"
        4 -> "不支持的查询"
        5 -> "查询被拒绝"
        else -> "响应码 $code"
    }
}
