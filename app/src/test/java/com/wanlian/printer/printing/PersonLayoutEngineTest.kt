package com.wanlian.printer.printing

import com.wanlian.printer.model.FooterPerson
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.PersonLayoutRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonLayoutEngineTest {
    @Test
    fun `two people use separate vertical columns with a shared top`() {
        val result = parallel(
            FooterPerson(relation = "媳", name = "黄秋", id = "wife"),
            FooterPerson(relation = "儿", name = "夏凡", id = "son"),
        )

        assertEquals(2, result.columns.size)
        assertEquals(result.columns[0].startY, result.columns[1].startY, 0.001f)
        assertNotEquals(result.columns[0].centerX, result.columns[1].centerX, 0.001f)
        assertEquals(listOf("媳", "黄", "秋"), result.glyphs.filter { it.personId == "wife" }.map { it.text })
        assertEquals(listOf("儿", "夏", "凡"), result.glyphs.filter { it.personId == "son" }.map { it.text })
    }

    @Test
    fun `three parallel people all share the same start y`() {
        val result = parallel(
            FooterPerson(relation = "长子", name = "张三", id = "first"),
            FooterPerson(relation = "次子", name = "李四", id = "second"),
            FooterPerson(relation = "女", name = "王五", id = "third"),
        )

        assertEquals(3, result.columns.size)
        assertTrue(result.columns.all { it.startY == result.columns.first().startY })
        assertEquals(3, result.columns.map { it.centerX }.distinct().size)
    }

    @Test
    fun `sequential mode preserves the original single vertical chain`() {
        val persons = listOf(
            FooterPerson(relation = "媳", name = "黄秋", id = "wife"),
            FooterPerson(relation = "儿", name = "夏凡", id = "son"),
        )
        val result = PersonLayoutEngine.layout(
            persons = persons,
            layout = PersonLayout.SEQUENTIAL,
            areaStartX = 0f,
            areaWidthDots = 800f,
            groupTop = 120f,
            glyphAdvanceDots = 48f,
            glyphHeightDots = 40f,
            baselineOffsetDots = 36f,
            sequentialColumnWidthDots = 40f,
            parallelColumns = null,
            groupOffsetXDots = 0f,
        )

        assertEquals(result.columns[0].centerX, result.columns[1].centerX, 0.001f)
        assertTrue(result.columns[1].startY > result.columns[0].startY)
        assertEquals(listOf("媳", "黄", "秋", "儿", "夏", "凡"), result.glyphs.map { it.text })
    }

    @Test
    fun `preview and print consumers receive identical coordinates`() {
        val persons = arrayOf(
            FooterPerson(relation = "媳", name = "黄秋", id = "wife"),
            FooterPerson(relation = "儿", name = "夏凡", id = "son"),
        )

        val previewCoordinates = parallel(*persons)
        val printMaskCoordinates = parallel(*persons)

        assertEquals(previewCoordinates, printMaskCoordinates)
    }

    private fun parallel(vararg persons: FooterPerson): PersonCoordinateLayout {
        val resolved = PersonLayoutRules.resolveParallelColumns(
            personCount = persons.size,
            requestedFontSizeDots = 40f,
            requestedColumnGapMm = 6f,
            availableWidthDots = 800f,
            fontWidthScale = 1f,
        )
        return PersonLayoutEngine.layout(
            persons = persons.toList(),
            layout = PersonLayout.PARALLEL_COLUMNS,
            areaStartX = 0f,
            areaWidthDots = 800f,
            groupTop = 120f,
            glyphAdvanceDots = 48f,
            glyphHeightDots = 40f,
            baselineOffsetDots = 36f,
            sequentialColumnWidthDots = 40f,
            parallelColumns = resolved,
            groupOffsetXDots = 0f,
        )
    }
}
