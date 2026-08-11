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
    fun `footer flower and cut guide remain independent even with legacy shared flag`() {
        val pair = CoupletPairDocument.fromSingle(PrintSettings())
        val selected = pair.left.copy(
            cutGuide = pair.left.cutGuide.copy(enabled = true, notchDepthMm = 31f),
            footerLabel = pair.left.footerLabel.copy(
                flower = pair.left.footerLabel.flower.copy(sizeMm = 24f, offsetXmm = 3f),
            ),
        )
        val updated = pair.replaceSelected(selected)
        assertNotEquals(updated.left.cutGuide, updated.right.cutGuide)
        assertNotEquals(updated.left.footerLabel.flower, updated.right.footerLabel.flower)
    }

    @Test
    fun `shared person layout keeps independent people on both sides`() {
        val leftPeople = listOf(FooterPerson("媳", "黄秋"), FooterPerson("儿", "夏凡"))
        val rightPeople = listOf(FooterPerson("女", "夏丽君"))
        val original = CoupletPairDocument.fromSingle(
            PrintSettings(
                personBlock = PersonBlockSettings(
                    persons = leftPeople,
                    personInsertIndex = 4,
                    offsetXMm = -3f,
                ),
            ),
        ).copy(
            right = PrintSettings(
                personBlock = PersonBlockSettings(
                    persons = rightPeople,
                    personInsertIndex = 7,
                    offsetXMm = 8f,
                ),
            ),
        )
        val updated = original.replaceSelected(
            original.left.copy(
                personBlock = original.left.personBlock.copy(
                    layout = PersonLayout.PARALLEL_COLUMNS,
                    placementMode = PersonPlacementMode.INLINE_INSERT,
                    columnGapMm = 9f,
                    fontId = "hei",
                    fontSizeDots = 42f,
                ),
            ),
        )

        assertEquals(PersonLayout.PARALLEL_COLUMNS, updated.right.personBlock.layout)
        assertEquals(PersonPlacementMode.INLINE_INSERT, updated.right.personBlock.placementMode)
        assertEquals(9f, updated.right.personBlock.columnGapMm, 0.001f)
        assertEquals("hei", updated.right.personBlock.fontId)
        assertEquals(42f, updated.right.personBlock.fontSizeDots, 0.001f)
        assertEquals(leftPeople, updated.left.personBlock.persons)
        assertEquals(rightPeople, updated.right.personBlock.persons)
        assertEquals(4, updated.left.personBlock.personInsertIndex)
        assertEquals(7, updated.right.personBlock.personInsertIndex)
        assertEquals(-3f, updated.left.personBlock.offsetXMm, 0.001f)
        assertEquals(8f, updated.right.personBlock.offsetXMm, 0.001f)
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
    fun `selected side updates and prints left plus eight and right minus four independently`() {
        var document = CoupletPairDocument(
            left = PrintSettings(
                text = "沉痛悼念外祖母  千古",
            ),
            right = PrintSettings(
                text = "愚侄夫妇率全家  叩挽",
            ),
        )
        document = document.replaceSelected(
            document.selectedSettings.copy(
                closingTextBlock = document.selectedSettings.closingTextBlock.copy(offsetYMm = 8f),
            ),
        )
        document = document.select(CoupletSide.RIGHT)
        assertEquals(0f, document.selectedSettings.closingTextBlock.offsetYMm, 0.001f)
        document = document.replaceSelected(
            document.selectedSettings.copy(
                closingTextBlock = document.selectedSettings.closingTextBlock.copy(offsetYMm = -4f),
            ),
        )
        val printJobs = PairPrintPlan.jobs(document)

        assertEquals(8f, document.left.closingTextBlock.offsetYMm, 0.001f)
        assertEquals(-4f, document.right.closingTextBlock.offsetYMm, 0.001f)
        assertEquals(-4f, document.selectedSettings.closingTextBlock.offsetYMm, 0.001f)
        assertEquals(8f, printJobs[0].settings.closingTextBlock.offsetYMm, 0.001f)
        assertEquals(-4f, printJobs[1].settings.closingTextBlock.offsetYMm, 0.001f)
    }

    @Test
    fun `selected side keeps every left and right footer label setting independent`() {
        var document = CoupletPairDocument.fromSingle(PrintSettings()).copy(
            right = PrintSettings(
                bottomMarginMm = 18f,
                footerLabel = FooterLabelSettings(text = "右联落款"),
            ),
        )
        document = document.replaceSelected(
            document.selectedSettings.copy(
                bottomMarginMm = 26f,
                cutGuide = document.selectedSettings.cutGuide.copy(enabled = true),
                footerLabel = document.selectedSettings.footerLabel.copy(
                    text = "左联落款",
                    fontId = "song",
                    fontSizeDots = 72f,
                    spacingDots = 18f,
                    distanceFromMainMm = 28f,
                    bottomMarginMm = 34f,
                    offsetYMm = 35f,
                    position = FooterLabelPosition.LEFT,
                    flower = document.selectedSettings.footerLabel.flower.copy(sizeMm = 22f),
                ),
            ),
        )
        document = document.select(CoupletSide.RIGHT)
        assertEquals(0f, document.selectedSettings.footerLabel.offsetYMm, 0.001f)
        document = document.replaceSelected(
            document.selectedSettings.copy(
                bottomMarginMm = 44f,
                cutGuide = document.selectedSettings.cutGuide.copy(
                    enabled = false,
                    bottomOffsetMm = 19f,
                ),
                footerLabel = document.selectedSettings.footerLabel.copy(
                    text = "右联新落款",
                    fontId = "hei",
                    fontSizeDots = 96f,
                    spacingDots = 32f,
                    distanceFromMainMm = 46f,
                    bottomMarginMm = 52f,
                    offsetYMm = -42f,
                    position = FooterLabelPosition.RIGHT,
                    flower = document.selectedSettings.footerLabel.flower.copy(sizeMm = 31f),
                ),
            ),
        )
        val printJobs = PairPrintPlan.jobs(document)

        assertEquals("左联落款", document.left.footerLabel.text)
        assertEquals("右联新落款", document.right.footerLabel.text)
        assertEquals(35f, document.left.footerLabel.offsetYMm, 0.001f)
        assertEquals(-42f, document.right.footerLabel.offsetYMm, 0.001f)
        assertEquals(72f, document.left.footerLabel.fontSizeDots, 0.001f)
        assertEquals(96f, document.right.footerLabel.fontSizeDots, 0.001f)
        assertEquals(22f, document.left.footerLabel.flower.sizeMm, 0.001f)
        assertEquals(31f, document.right.footerLabel.flower.sizeMm, 0.001f)
        assertEquals(26f, document.left.bottomMarginMm, 0.001f)
        assertEquals(44f, document.right.bottomMarginMm, 0.001f)
        assertTrue(document.left.cutGuide.enabled)
        assertTrue(!document.right.cutGuide.enabled)
        assertEquals(35f, printJobs[0].settings.footerLabel.offsetYMm, 0.001f)
        assertEquals(-42f, printJobs[1].settings.footerLabel.offsetYMm, 0.001f)
        assertEquals(26f, printJobs[0].settings.bottomMarginMm, 0.001f)
        assertEquals(44f, printJobs[1].settings.bottomMarginMm, 0.001f)
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
