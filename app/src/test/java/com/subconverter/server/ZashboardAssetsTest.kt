package com.subconverter.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZashboardAssetsTest {
    @Test
    fun resolvesRootAndNestedAssets() {
        assertEquals("zashboard/index.html", ZashboardAssets.resolve("/zashboard/"))
        assertEquals(
            "zashboard/assets/index.js",
            ZashboardAssets.resolve("/zashboard/assets/index.js"),
        )
    }

    @Test
    fun rejectsPathsOutsideZashboardRoot() {
        assertNull(ZashboardAssets.resolve("/subscriptions/1.yaml"))
        assertNull(ZashboardAssets.resolve("/zashboard/../secret"))
        assertNull(ZashboardAssets.resolve("/zashboard/assets\\secret"))
    }

    @Test
    fun fallsBackOnlyForFrontendRoutes() {
        assertTrue(ZashboardAssets.shouldFallbackToIndex("zashboard/connections"))
        assertFalse(ZashboardAssets.shouldFallbackToIndex("zashboard/assets/missing.js"))
    }

    @Test
    fun mapsCommonContentTypes() {
        assertEquals("text/html; charset=utf-8", ZashboardAssets.contentType("zashboard/index.html"))
        assertEquals("application/javascript; charset=utf-8", ZashboardAssets.contentType("zashboard/assets/app.js"))
        assertEquals("application/manifest+json; charset=utf-8", ZashboardAssets.contentType("zashboard/manifest.webmanifest"))
        assertEquals("font/woff2", ZashboardAssets.contentType("zashboard/assets/font.woff2"))
    }
}
