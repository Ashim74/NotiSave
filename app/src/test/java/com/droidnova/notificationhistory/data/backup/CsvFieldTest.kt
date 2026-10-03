package com.droidnova.notificationhistory.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvFieldTest {

    @Test
    fun `plain values stay as they are`() {
        assertEquals("Hello", csvField("Hello"))
        assertEquals("", csvField(""))
    }

    @Test
    fun `commas, quotes and line breaks are quoted with inner quotes doubled`() {
        assertEquals("\"a,b\"", csvField("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", csvField("say \"hi\""))
        assertEquals("\"line1\nline2\"", csvField("line1\nline2"))
    }
}
