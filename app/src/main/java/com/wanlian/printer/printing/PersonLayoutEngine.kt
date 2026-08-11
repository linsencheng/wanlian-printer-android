package com.wanlian.printer.printing

import com.wanlian.printer.model.FooterPerson
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.ResolvedPersonColumns

data class PersonColumnCoordinate(
    val personId: String,
    val centerX: Float,
    val startY: Float,
)

data class PersonGlyphCoordinate(
    val personId: String,
    val text: String,
    val centerX: Float,
    val baselineY: Float,
)

data class PersonCoordinateLayout(
    val columns: List<PersonColumnCoordinate>,
    val glyphs: List<PersonGlyphCoordinate>,
    val groupLeft: Float,
    val groupWidth: Float,
    val groupTop: Float,
    val groupBottom: Float,
)

/**
 * The single source of truth for footer-person coordinates. Both preview and print-mask canvases
 * consume these exact placements through [FooterLabelRenderer].
 */
object PersonLayoutEngine {
    fun layout(
        persons: List<FooterPerson>,
        layout: PersonLayout,
        areaStartX: Float,
        areaWidthDots: Float,
        groupTop: Float,
        glyphAdvanceDots: Float,
        glyphHeightDots: Float,
        baselineOffsetDots: Float,
        sequentialColumnWidthDots: Float,
        parallelColumns: ResolvedPersonColumns?,
        groupOffsetXDots: Float,
    ): PersonCoordinateLayout {
        val visiblePersons = persons.filter { it.verticalText.isNotBlank() }
        if (visiblePersons.isEmpty()) {
            return PersonCoordinateLayout(emptyList(), emptyList(), areaStartX, 0f, groupTop, groupTop)
        }

        val groupWidth = when (layout) {
            PersonLayout.SEQUENTIAL -> sequentialColumnWidthDots
            PersonLayout.PARALLEL_COLUMNS -> requireNotNull(parallelColumns).groupWidthDots
        }.coerceAtLeast(1f)
        val centeredLeft = areaStartX + (areaWidthDots - groupWidth) / 2f + groupOffsetXDots
        val minimumLeft = areaStartX
        val maximumLeft = areaStartX + (areaWidthDots - groupWidth).coerceAtLeast(0f)
        val groupLeft = if (groupWidth <= areaWidthDots) {
            centeredLeft.coerceIn(minimumLeft, maximumLeft)
        } else {
            centeredLeft
        }

        val columns = mutableListOf<PersonColumnCoordinate>()
        val glyphs = mutableListOf<PersonGlyphCoordinate>()
        var maximumBottom = groupTop
        when (layout) {
            PersonLayout.SEQUENTIAL -> {
                val centerX = groupLeft + groupWidth / 2f
                var nextStartY = groupTop
                visiblePersons.forEach { person ->
                    columns += PersonColumnCoordinate(person.id, centerX, nextStartY)
                    val characters = codePoints(person.verticalText)
                    characters.forEachIndexed { index, character ->
                        glyphs += PersonGlyphCoordinate(
                            personId = person.id,
                            text = character,
                            centerX = centerX,
                            baselineY = nextStartY + index * glyphAdvanceDots + baselineOffsetDots,
                        )
                    }
                    nextStartY += characters.size * glyphAdvanceDots
                    maximumBottom = maxOf(
                        maximumBottom,
                        nextStartY - glyphAdvanceDots + glyphHeightDots,
                    )
                }
            }

            PersonLayout.PARALLEL_COLUMNS -> {
                val resolved = requireNotNull(parallelColumns)
                visiblePersons.forEachIndexed { index, person ->
                    val centerX = groupLeft + resolved.centersFromGroupStart[index]
                    columns += PersonColumnCoordinate(person.id, centerX, groupTop)
                    val characters = codePoints(person.verticalText)
                    characters.forEachIndexed { characterIndex, character ->
                        glyphs += PersonGlyphCoordinate(
                            personId = person.id,
                            text = character,
                            centerX = centerX,
                            baselineY = groupTop + characterIndex * glyphAdvanceDots + baselineOffsetDots,
                        )
                    }
                    maximumBottom = maxOf(
                        maximumBottom,
                        groupTop + (characters.size - 1) * glyphAdvanceDots + glyphHeightDots,
                    )
                }
            }
        }
        return PersonCoordinateLayout(
            columns = columns,
            glyphs = glyphs,
            groupLeft = groupLeft,
            groupWidth = groupWidth,
            groupTop = groupTop,
            groupBottom = maximumBottom,
        )
    }

    private fun codePoints(text: String): List<String> = buildList {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            add(String(Character.toChars(codePoint)))
            offset += Character.charCount(codePoint)
        }
    }
}
