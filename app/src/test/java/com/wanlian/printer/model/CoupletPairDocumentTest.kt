package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupletPairDocumentTest {
    @Test
    fun `single to pair starts with equal paper lengths and aligned text tops`() {
        val pair = CoupletPairDocument.fromSingle(PrintSettings(paperLengthMm = 991f, topMarginMm = 12f))
        assertEquals(pair.left.paperLengthMm, pair.right.paperLengthMm, 0.001f)
        assertTrue(PairLayoutRules.hasAlignedTextStart(pair))
    }

    @Test
    fun `700 and 850 millimeter requirements resolve to 991`() {
        assertEquals(991f, PairLayoutRules.resolveLengthMm(700f, 850f), 0.001f)
    }

    @Test
    fun `pair grows together when one side exceeds preferred length`() {
        val lengths = PairLayoutRules.resolveSideLengthsMm(1050f, 800f)
        assertTrue(lengths.leftMm >= 1050f)
        assertEquals(lengths.leftMm, lengths.rightMm, 0.001f)
    }

    @Test
    fun `shared border update applies to both sides`() {
        val pair = CoupletPairDocument.fromSingle(PrintSettings())
        val updated = pair.replaceSelected(
            pair.left.copy(border = pair.left.border.copy(widthMm = 12f)),
        )
        assertEquals(updated.left.border, updated.right.border)
    }

    @Test
    fun `shared footer layout update applies cut guide and flower to both sides`() {
        val pair = CoupletPairDocument.fromSingle(PrintSettings())
        val selected = pair.left.copy(
            cutGuide = pair.left.cutGuide.copy(enabled = true, notchDepthMm = 31f),
            footerLabel = pair.left.footerLabel.copy(
                flower = pair.left.footerLabel.flower.copy(sizeMm = 24f, offsetXmm = 3f),
            ),
        )
        val updated = pair.replaceSelected(selected)
        assertEquals(updated.left.cutGuide, updated.right.cutGuide)
        assertEquals(updated.left.footerLabel.flower, updated.right.footerLabel.flower)
    }

    @Test
    fun `shared person layout keeps independent people on both sides`() {
        val leftPeople = listOf(FooterPerson("媳", "黄秋"), FooterPerson("儿", "夏凡"))
        val rightPeople = listOf(FooterPerson("女", "夏丽君"))
        val original = CoupletPairDocument.fromSingle(
            PrintSettings(footerLabel = FooterLabelSettings(persons = leftPeople)),
        ).copy(
            right = PrintSettings(footerLabel = FooterLabelSettings(persons = rightPeople)),
        )
        val updated = original.replaceSelected(
            original.left.copy(
                footerLabel = original.left.footerLabel.copy(
                    personLayout = PersonLayout.PARALLEL_COLUMNS,
                    personColumnGapMm = 9f,
                    personGroupOffsetXMm = -3f,
                    personGroupOffsetYMm = 7f,
                    personFontId = "hei",
                    personFontSizeDots = 42f,
                ),
            ),
        )

        assertEquals(PersonLayout.PARALLEL_COLUMNS, updated.right.footerLabel.personLayout)
        assertEquals(9f, updated.right.footerLabel.personColumnGapMm, 0.001f)
        assertEquals(-3f, updated.right.footerLabel.personGroupOffsetXMm, 0.001f)
        assertEquals(7f, updated.right.footerLabel.personGroupOffsetYMm, 0.001f)
        assertEquals("hei", updated.right.footerLabel.personFontId)
        assertEquals(42f, updated.right.footerLabel.personFontSizeDots, 0.001f)
        assertEquals(leftPeople, updated.left.footerLabel.persons)
        assertEquals(rightPeople, updated.right.footerLabel.persons)
    }

    @Test
    fun `left text update never changes right text`() {
        val original = CoupletPairDocument.fromSingle(PrintSettings(text = "左联原文"))
        val updated = original.replaceSelected(original.left.copy(text = "左联新文"))
        assertEquals("左联新文", updated.left.text)
        assertEquals(original.right.text, updated.right.text)
        assertNotEquals(updated.left.text, updated.right.text)
    }

    @Test
    fun `right text update never changes left text`() {
        val original = CoupletPairDocument.fromSingle(PrintSettings(text = "左联原文"))
            .select(CoupletSide.RIGHT)
        val updated = original.replaceSelected(original.right.copy(text = "右联新文"))
        assertEquals("右联新文", updated.right.text)
        assertEquals(original.left.text, updated.left.text)
    }

    @Test
    fun `pair print plan always orders left before right`() {
        val jobs = PairPrintPlan.jobs(CoupletPairDocument.fromSingle(PrintSettings()))
        assertEquals(listOf(CoupletSide.LEFT, CoupletSide.RIGHT), jobs.map { it.side })
    }

    @Test
    fun `footer vertical and horizontal orientations create different text runs`() {
        val vertical = FooterLabelSettings(
            text = "阿里巴巴集团\n敬挽",
            orientation = FooterTextOrientation.VERTICAL,
        )
        val horizontal = vertical.copy(orientation = FooterTextOrientation.HORIZONTAL)
        assertEquals(listOf("阿", "里", "巴", "巴", "集", "团"), FooterTextLayoutRules.textRuns(vertical).first())
        assertEquals(listOf("阿里巴巴集团"), FooterTextLayoutRules.textRuns(horizontal).first())
        assertEquals(2, FooterTextLayoutRules.textRuns(horizontal).size)
    }
}
