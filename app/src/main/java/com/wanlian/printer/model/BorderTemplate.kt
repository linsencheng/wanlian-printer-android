package com.wanlian.printer.model

enum class BorderTemplateCategory(val label: String) {
    SIMPLE_LINES("线条"),
    GREEK_KEY("回纹"),
    CLOUD("云纹"),
    SCALLOP("波浪"),
    FLORAL("花边"),
    ORNAMENTAL("装饰"),
    BOLD_DECORATIVE("粗饰"),
}

/**
 * Programmatic border templates. [unitHeightScale] and [strokeScale] provide a
 * characteristic default while the user's global unit height and line width remain adjustable.
 */
enum class BorderTemplate(
    val label: String,
    val category: BorderTemplateCategory,
    val unitHeightScale: Float = 1f,
    val strokeScale: Float = 1f,
) {
    NONE("无边框", BorderTemplateCategory.SIMPLE_LINES),
    DOUBLE_LINE("双线", BorderTemplateCategory.SIMPLE_LINES),
    TRIPLE_LINE("三线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 0.85f),
    DOUBLE_LINE_WITH_DOTS("双线点珠", BorderTemplateCategory.SIMPLE_LINES, unitHeightScale = 0.75f),
    THICK_DOUBLE_LINE("粗双线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 1.8f),
    SINGLE_LINE("单线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 0.9f),
    THIN_THICK_LINE("粗细组合线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 1.1f),
    FOUR_LINE("四线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 0.7f),
    DASHED_DOUBLE_LINE("断续双线", BorderTemplateCategory.SIMPLE_LINES, strokeScale = 0.9f),
    LINE_BEAD_CHAIN("线珠组合", BorderTemplateCategory.SIMPLE_LINES, unitHeightScale = 0.65f),

    THIN_GREEK_KEY("细回纹", BorderTemplateCategory.GREEK_KEY, strokeScale = 0.8f),
    THICK_GREEK_KEY("粗回纹", BorderTemplateCategory.GREEK_KEY, strokeScale = 1.65f),
    BROKEN_KEY("断续回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 1.15f),
    INNER_OUTER_KEY("内外双回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 1.1f),
    DENSE_KEY_PATTERN("密集几何花边", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 0.65f),
    GREEK_KEY_SMALL("小回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 0.58f, strokeScale = 0.85f),
    GREEK_KEY_DOUBLE_ROW("双排回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 0.72f, strokeScale = 0.75f),
    GREEK_KEY_SQUARE("方折回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 0.82f),
    GREEK_KEY_OPEN("疏朗回纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 1.55f),
    LEI_WEN("雷纹", BorderTemplateCategory.GREEK_KEY, unitHeightScale = 0.68f),

    CLOUD_SOFT("柔和云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 1.25f),
    CLOUD_TALL("高挑云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 1.65f),
    CLOUD_SPIRAL("卷云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 1.4f),
    DOUBLE_CLOUD("双云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 1.25f),
    CLOUD_FINE("细云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 0.82f, strokeScale = 0.72f),
    CLOUD_LINKED("连续云纹", BorderTemplateCategory.CLOUD, unitHeightScale = 0.95f),
    CLOUD_BEAD("云珠纹", BorderTemplateCategory.CLOUD, unitHeightScale = 0.82f),
    RUYI_CLOUD("如意云边", BorderTemplateCategory.CLOUD, unitHeightScale = 1.1f),

    SCALLOP_SMALL("小半圆纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.7f),
    SCALLOP_LARGE("大半圆纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 1.35f),
    DOUBLE_SCALLOP("双半圆纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.9f),
    WAVE_THIN("细波浪", BorderTemplateCategory.SCALLOP, strokeScale = 0.75f),
    WAVE_BOLD("粗波浪", BorderTemplateCategory.SCALLOP, strokeScale = 1.8f),
    FISH_SCALE("鱼鳞纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.8f),
    WAVE_DOUBLE("双波浪", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.85f, strokeScale = 0.78f),
    WATER_RIPPLE("水纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.62f, strokeScale = 0.72f),
    ZIGZAG_WAVE("折线波纹", BorderTemplateCategory.SCALLOP, unitHeightScale = 0.72f),

    SWIRL_THIN("细卷草纹", BorderTemplateCategory.FLORAL, unitHeightScale = 1.35f, strokeScale = 0.8f),
    SWIRL_DOUBLE("双卷草纹", BorderTemplateCategory.FLORAL, unitHeightScale = 1.35f),
    FLORAL_LOOP("花环连续纹", BorderTemplateCategory.FLORAL, unitHeightScale = 1.15f),
    PETAL_CHAIN("花瓣连续纹", BorderTemplateCategory.FLORAL, unitHeightScale = 0.9f),
    LEAF_VINE("叶蔓边", BorderTemplateCategory.FLORAL, unitHeightScale = 0.95f),

    SEAL_PATTERN("印章方块纹", BorderTemplateCategory.ORNAMENTAL, unitHeightScale = 0.9f),
    MAZE_PATTERN("迷宫几何纹", BorderTemplateCategory.ORNAMENTAL, unitHeightScale = 1.05f),
    KNOT_PATTERN("中国结纹", BorderTemplateCategory.ORNAMENTAL, unitHeightScale = 1.3f),
    DIAMOND_CHAIN("菱格边", BorderTemplateCategory.ORNAMENTAL, unitHeightScale = 0.72f),
    PEARL_CHAIN("连珠纹", BorderTemplateCategory.ORNAMENTAL, unitHeightScale = 0.58f),

    BOLD_CLOUD_STRIP("粗云带纹", BorderTemplateCategory.BOLD_DECORATIVE, unitHeightScale = 1.35f, strokeScale = 2f),
    BOLD_ORNAMENT_STRIP("强装饰中式花边", BorderTemplateCategory.BOLD_DECORATIVE, unitHeightScale = 1.2f, strokeScale = 1.45f),
    BOLD_BLACK_WHITE_PATTERN("粗装饰黑白纹", BorderTemplateCategory.BOLD_DECORATIVE, unitHeightScale = 0.8f, strokeScale = 1.4f),
}
