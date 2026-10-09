package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateNameRulesTest {
    @Test
    fun `rename changes only display name and preserves stable identity and content`() {
        val settings = PrintSettings(text = "沉痛悼念\n王先生千古", paperWidthMm = 88f)
        val pair = CoupletPairDocument.fromSingle(settings)
        val original = CoupletTemplate(
            id = "stable-template-id",
            name = "旧名称",
            settings = settings,
            updatedAt = 123456789L,
            documentMode = DocumentMode.PAIR,
            pairDocument = pair,
        )

        val renamed = requireNotNull(original.renamedTo("  公司花圈双联  "))

        assertEquals("stable-template-id", renamed.id)
        assertEquals("公司花圈双联", renamed.name)
        assertEquals(original.settings, renamed.settings)
        assertSame(original.settings, renamed.settings)
        assertEquals(original.updatedAt, renamed.updatedAt)
        assertEquals(original.documentMode, renamed.documentMode)
        assertEquals(original.pairDocument, renamed.pairDocument)
    }

    @Test
    fun `blank names are rejected and names are limited to thirty code points`() {
        val template = CoupletTemplate("id", "原名", PrintSettings(), 1L)
        assertNull(template.renamedTo("   "))

        val longName = "挽".repeat(35)
        val normalized = TemplateNameRules.normalize(longName)
        assertEquals(30, normalized.codePointCount(0, normalized.length))
        assertTrue(TemplateNameRules.isValid(normalized))
    }

    @Test
    fun `duplicate names do not overwrite templates because ids remain distinct`() {
        val first = CoupletTemplate("template-a", "甲", PrintSettings(text = "甲联"), 1L)
        val second = CoupletTemplate("template-b", "通用模板", PrintSettings(text = "乙联"), 2L)
        val renamedFirst = requireNotNull(first.renamedTo("通用模板"))

        assertEquals(second.name, renamedFirst.name)
        assertNotEquals(second.id, renamedFirst.id)
        assertNotEquals(second.settings, renamedFirst.settings)
    }

    @Test
    fun `single and pair templates use the same rename rule`() {
        val single = CoupletTemplate("single", "单联", PrintSettings(), 1L)
        val pair = CoupletTemplate(
            "pair",
            "双联",
            PrintSettings(),
            2L,
            DocumentMode.PAIR,
            CoupletPairDocument.fromSingle(PrintSettings()),
        )

        assertEquals("新单联", requireNotNull(single.renamedTo("新单联")).name)
        assertEquals("新双联", requireNotNull(pair.renamedTo("新双联")).name)
    }
}
