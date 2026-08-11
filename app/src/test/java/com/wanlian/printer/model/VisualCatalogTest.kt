package com.wanlian.printer.model

import com.wanlian.printer.printing.FontRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualCatalogTest {
    @Test
    fun `border catalog includes twenty new printable templates`() {
        val printableBorders = BorderTemplate.entries.filterNot { it == BorderTemplate.NONE }
        assertTrue(printableBorders.size >= 49)
        assertEquals(printableBorders.size, printableBorders.distinctBy(BorderTemplate::name).size)
    }

    @Test
    fun `font catalog includes thirty new unique presets`() {
        assertTrue(FontRepository.fonts.size >= 42)
        assertEquals(FontRepository.fonts.size, FontRepository.fonts.distinctBy { it.id }.size)
    }
}
