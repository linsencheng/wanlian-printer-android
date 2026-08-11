package com.wanlian.printer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonLayoutPersistenceTest {
    @Test
    fun `template person layout snapshot restores every field and stable ids`() {
        val source = FooterLabelSettings(
            persons = listOf(
                FooterPerson(relation = "媳", name = "黄秋", id = "person-1"),
                FooterPerson(relation = "儿", name = "夏凡", id = "person-2"),
            ),
            personLayout = PersonLayout.PARALLEL_COLUMNS,
            personColumnGapMm = 11f,
            personGroupOffsetXMm = -4f,
            personGroupOffsetYMm = 9f,
            personFontId = "imported:family-font",
            personFontSizeDots = 52f,
        )

        val restored = FooterLabelSettings().withPersonLayoutState(source.personLayoutState())

        assertEquals(source.persons, restored.persons)
        assertEquals(source.personLayout, restored.personLayout)
        assertEquals(source.personColumnGapMm, restored.personColumnGapMm, 0.001f)
        assertEquals(source.personGroupOffsetXMm, restored.personGroupOffsetXMm, 0.001f)
        assertEquals(source.personGroupOffsetYMm, restored.personGroupOffsetYMm, 0.001f)
        assertEquals(source.personFontId, restored.personFontId)
        assertEquals(source.personFontSizeDots, restored.personFontSizeDots, 0.001f)
        assertEquals(listOf("person-1", "person-2"), restored.persons.map { it.id })
    }

    @Test
    fun `legacy default remains sequential`() {
        assertEquals(PersonLayout.SEQUENTIAL, FooterLabelSettings().personLayout)
    }
}
