package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTransferRulesTest {
    @Test
    fun `keeps original name when it is not occupied`() {
        assertEquals(
            "双联挽联",
            TemplateTransferRules.uniqueImportedName("双联挽联", listOf("其他模板")),
        )
    }

    @Test
    fun `adds numbered import suffix without overwriting existing templates`() {
        assertEquals(
            "双联挽联（导入 2）",
            TemplateTransferRules.uniqueImportedName(
                "双联挽联",
                listOf("双联挽联", "双联挽联（导入）"),
            ),
        )
    }

    @Test
    fun `imported name always respects the template length limit`() {
        val longName = "挽".repeat(TemplateNameRules.MAX_CODE_POINTS)
        val result = TemplateTransferRules.uniqueImportedName(longName, listOf(longName))

        assertTrue(result.endsWith("（导入）"))
        assertTrue(result.codePointCount(0, result.length) <= TemplateNameRules.MAX_CODE_POINTS)
    }

    @Test
    fun `safe file stem removes characters forbidden by Android file systems`() {
        val result = TemplateTransferRules.safeFileStem("父亲/母亲:*?\"<>|")

        assertFalse(result.contains('/'))
        assertFalse(result.contains(':'))
        assertEquals("父亲_母亲_______", result)
    }
}
