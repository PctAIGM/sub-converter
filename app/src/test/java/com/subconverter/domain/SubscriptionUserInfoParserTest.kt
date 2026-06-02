package com.subconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionUserInfoParserTest {
    @Test
    fun parsesStandardHeader() {
        val info = SubscriptionUserInfoParser.parse(
            "upload=100; download=900; total=2000; expire=1893456000",
        )

        assertEquals(100L, info?.uploadBytes)
        assertEquals(900L, info?.downloadBytes)
        assertEquals(2000L, info?.totalBytes)
        assertEquals(1893456000L, info?.expireAtSeconds)
        assertEquals(1000L, info?.usedBytes)
        assertEquals(1000L, info?.remainingBytes)
    }

    @Test
    fun parsesV2boardHeaderReturnedForClashUserAgent() {
        val info = SubscriptionUserInfoParser.parse(
            "upload=832198771; download=13461581558; total=268435456000; expire=1790323555",
        )

        assertEquals(832198771L, info?.uploadBytes)
        assertEquals(13461581558L, info?.downloadBytes)
        assertEquals(268435456000L, info?.totalBytes)
        assertEquals(1790323555L, info?.expireAtSeconds)
        assertEquals(254141675671L, info?.remainingBytes)
    }

    @Test
    fun returnsNullForMissingValues() {
        assertNull(SubscriptionUserInfoParser.parse(null))
        assertNull(SubscriptionUserInfoParser.parse("foo=bar"))
    }
}
