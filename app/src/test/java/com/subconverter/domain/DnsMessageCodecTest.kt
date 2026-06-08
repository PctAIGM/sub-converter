package com.subconverter.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress

class DnsMessageCodecTest {
    @Test
    fun parsesIpv4AnswerWithCompressedName() {
        val response = response(
            hostname = "example.com",
            type = 1,
            id = 0x1234,
            address = byteArrayOf(1, 2, 3, 4),
        )

        val addresses = DnsMessageCodec.parseResponse(response, 0x1234, 1)

        assertEquals(listOf(InetAddress.getByName("1.2.3.4")), addresses)
    }

    @Test
    fun parsesIpv6Answer() {
        val address = InetAddress.getByName("2001:db8::1").address
        val response = response(
            hostname = "example.com",
            type = 28,
            id = 42,
            address = address,
        )

        val addresses = DnsMessageCodec.parseResponse(response, 42, 28)

        assertArrayEquals(address, addresses.single().address)
    }

    @Test
    fun rejectsMismatchedTransactionId() {
        val response = response(
            hostname = "example.com",
            type = 1,
            id = 1,
            address = byteArrayOf(1, 1, 1, 1),
        )

        assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.parseResponse(response, 2, 1)
        }
    }

    @Test
    fun reportsDnsErrorResponse() {
        val response = DnsMessageCodec.createQuery("missing.example", 1, 7)
        response[2] = 0x81.toByte()
        response[3] = 0x83.toByte()

        val error = assertThrows(IllegalArgumentException::class.java) {
            DnsMessageCodec.parseResponse(response, 7, 1)
        }

        assertEquals("DNS 解析失败: 域名不存在", error.message)
    }

    private fun response(
        hostname: String,
        type: Int,
        id: Int,
        address: ByteArray,
    ): ByteArray {
        val query = DnsMessageCodec.createQuery(hostname, type, id)
        query[2] = 0x81.toByte()
        query[3] = 0x80.toByte()
        query[6] = 0
        query[7] = 1
        return ByteArrayOutputStream().apply {
            write(query)
            write(byteArrayOf(0xc0.toByte(), 0x0c))
            writeU16(type)
            writeU16(1)
            write(byteArrayOf(0, 0, 0, 60))
            writeU16(address.size)
            write(address)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeU16(value: Int) {
        write((value ushr 8) and 0xff)
        write(value and 0xff)
    }
}
