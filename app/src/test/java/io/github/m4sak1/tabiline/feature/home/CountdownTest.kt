package io.github.m4sak1.tabiline.feature.home

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {
    private val now = Instant.parse("2026-10-05T00:00:00Z")
    @Test fun `countdown switches at one hour`() {
        assertEquals("あと 1時間0分", countdown(now.plusSeconds(3600), now))
        assertEquals("あと 59分59秒", countdown(now.plusSeconds(3599), now))
        assertEquals("あと 0分09秒", countdown(now.plusSeconds(9), now))
        assertEquals("あと 0分00秒", countdown(now.minusSeconds(1), now))
        assertEquals("あと 1日0時間", countdown(now.plusSeconds(86400), now))
    }
}
