package com.wanlian.printer.model

data class FooterLabelSettings(
    val enabled: Boolean = false,
    val text: String = "XXX敬挽",
    val secondaryText: String = "",
    val fontId: String = "elegant-song",
    val fontSizeDots: Float = 48f,
    val spacingDots: Float = 8f,
    val distanceFromMainMm: Float = 12f,
    val bottomMarginMm: Float = 12f,
    val offsetYMm: Float = 0f,
    val position: FooterLabelPosition = FooterLabelPosition.CENTER,
    val orientation: FooterTextOrientation = FooterTextOrientation.VERTICAL,
    val flower: FlowerSettings = FlowerSettings(),
)

object FooterLabelAdjustmentLimits {
    const val MIN_OFFSET_Y_MM = -50f
    const val MAX_OFFSET_Y_MM = 50f

    fun clampOffsetYMm(value: Float): Float = value.coerceIn(
        MIN_OFFSET_Y_MM,
        MAX_OFFSET_Y_MM,
    )
}

object FooterLabelPositionRules {
    fun resolveTopDots(
        baseTopDots: Float,
        offsetYMm: Float,
        blockHeightDots: Float,
        safeTopDots: Float,
        safeBottomDots: Float,
    ): Float {
        val requestedTop = baseTopDots + PrintUnits.mmToDots(
            FooterLabelAdjustmentLimits.clampOffsetYMm(offsetYMm),
        )
        val maximumTop = (safeBottomDots - blockHeightDots).coerceAtLeast(safeTopDots)
        return requestedTop.coerceIn(safeTopDots, maximumTop)
    }
}

enum class FooterTextOrientation(val label: String) {
    VERTICAL("竖排"),
    HORIZONTAL("横排"),
}

enum class FooterLabelPosition(val label: String) {
    LEFT("靠左"),
    CENTER("居中"),
    RIGHT("靠右"),
}

enum class FlowerStyle(val label: String) {
    NONE("无"),
    WHITE_CHRYSANTHEMUM("白菊"),
    CHRYSANTHEMUM_SINGLE("单朵菊花"),
    CHRYSANTHEMUM_DOUBLE("双层菊花"),
    LOTUS("莲花"),
    PLUM_BLOSSOM("梅花"),
    ORCHID_OUTLINE("兰花轮廓"),
    ROSE_OUTLINE("玫瑰轮廓"),
    CAMELLIA("山茶花"),
    FLOWER_BOUQUET("小花束"),
    FLOWER_BRANCH("枝叶花"),
    MEMORIAL_FLORAL("纪念花饰"),
    CHRYSANTHEMUM_REALISTIC("写实白菊"),
    MEMORIAL_SINGLE("单朵祭奠花"),
    DOUBLE_BLOOM("双花组合"),
    LEAFY_BOUQUET("叶片花束"),
    VERTICAL_FLORAL("竖向花饰"),
    HORIZONTAL_FLORAL("横向花组"),
    LILY_OUTLINE("百合线描"),
    MAGNOLIA("玉兰花"),
    PEONY_OUTLINE("牡丹轮廓"),
    DAHLIA("大丽花"),
    SUNFLOWER_LINE("向日葵线描"),
    DAISY_SPRAY("雏菊花簇"),
    CHINESE_KNOT_FLOWER("中式结花"),
    CORNER_BLOSSOM_LEFT("左角花"),
    CORNER_BLOSSOM_RIGHT("右角花"),
    CRESCENT_WREATH("月牙花环"),
    OVAL_WREATH("椭圆花环"),
    LOTUS_DOUBLE("重瓣莲花"),
    ROSE_SPRAY("玫瑰花枝"),
    PLUM_BRANCH("梅花枝"),
    ORCHID_SPRAY("兰花簇"),
    CAMELLIA_PAIR("双山茶"),
    CHRYSANTHEMUM_SPRAY("菊花簇"),
    MEMORIAL_WREATH("纪念花环"),
    RIBBON_BOUQUET("缎带花束"),
    LEAF_GARLAND("叶蔓花边"),
    BUD_BRANCH("花苞枝"),
    THREE_BLOOM("三花组合"),
    FAN_FLORAL("扇形花饰"),
}

data class FlowerSettings(
    val enabled: Boolean = true,
    val style: FlowerStyle = FlowerStyle.WHITE_CHRYSANTHEMUM,
    val sizeMm: Float = 14f,
    val offsetXmm: Float = 0f,
    val offsetYmm: Float = 0f,
    val rotationDegrees: Float = 0f,
)

object FlowerAdjustmentLimits {
    const val MIN_SIZE_MM = 8f
    const val MAX_SIZE_MM = 80f
    const val MIN_OFFSET_MM = -60f
    const val MAX_OFFSET_MM = 60f
    const val MIN_ROTATION_DEGREES = -30f
    const val MAX_ROTATION_DEGREES = 30f
}

object FooterTextLayoutRules {
    fun lines(settings: FooterLabelSettings): List<String> = buildList {
        addAll(settings.text.lineSequence().map(String::trim).filter(String::isNotEmpty))
        addAll(settings.secondaryText.lineSequence().map(String::trim).filter(String::isNotEmpty))
    }.ifEmpty { listOf(" ") }

    fun textRuns(settings: FooterLabelSettings): List<List<String>> = lines(settings).map { line ->
        when (settings.orientation) {
            FooterTextOrientation.VERTICAL -> codePoints(line)
            FooterTextOrientation.HORIZONTAL -> listOf(line)
        }
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
