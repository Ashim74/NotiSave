package com.droidnova.notificationhistory.data.mapper

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class ReadableTimeFormatterTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `same locale reuses one formatter instance`() {
        Locale.setDefault(Locale.US)
        val first = ReadableTimeFormatter.current()
        val second = ReadableTimeFormatter.current()
        assertSame(first, second)
    }

    @Test
    fun `locale change rebuilds the formatter`() {
        Locale.setDefault(Locale.US)
        val us = ReadableTimeFormatter.current()
        Locale.setDefault(Locale.GERMANY)
        val de = ReadableTimeFormatter.current()
        assertNotSame(us, de)
        assertEquals(Locale.GERMANY, de.locale)
    }

    @Test
    fun `formatter keeps the legacy pattern`() {
        Locale.setDefault(Locale.US)
        // 2024-03-05T14:07:00Z rendered in UTC → "05 Mar 2024, 02:07 PM"
        val formatted = ReadableTimeFormatter.current()
            .withZone(java.time.ZoneOffset.UTC)
            .format(java.time.Instant.parse("2024-03-05T14:07:00Z"))
        assertEquals("05 Mar 2024, 02:07 PM", formatted)
    }
}
