package com.wanlian.printer.printing

import android.content.Context
import android.graphics.Rect
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import com.wanlian.printer.model.FontDefinition
import com.wanlian.printer.model.ImportedFont
import com.wanlian.printer.model.TextWeight
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

sealed interface FontImportResult {
    data class Success(val font: FontDefinition) : FontImportResult
    data class Failure(val message: String) : FontImportResult
}

object FontRepository {
    private val builtInFonts: List<FontDefinition> = listOf(
        FontDefinition("song", "宋体", "serif", Typeface.NORMAL),
        FontDefinition("hei", "黑体", "sans-serif", Typeface.BOLD),
        FontDefinition("kai", "楷体", "serif", Typeface.NORMAL, textScaleX = 0.98f, textSkewX = -0.08f),
        FontDefinition("fang-song", "仿宋", "serif", Typeface.NORMAL, textScaleX = 0.88f),
        FontDefinition("xing-kai", "行楷", "cursive", Typeface.ITALIC, textScaleX = 0.96f, textSkewX = -0.18f),
        FontDefinition("li-shu", "隶书", "serif", Typeface.BOLD, textScaleX = 1.16f, textSkewX = 0.03f),
        FontDefinition("wei-bei", "魏碑风格", "sans-serif-black", Typeface.BOLD, textScaleX = 1.06f),
        FontDefinition("calligraphy", "书法风格", "cursive", Typeface.ITALIC, textScaleX = 0.92f, textSkewX = -0.14f),
        FontDefinition("brush-kai", "毛笔楷书", "serif", Typeface.BOLD, textScaleX = 0.95f, textSkewX = -0.04f),
        FontDefinition("brush-xing", "毛笔行书", "cursive", Typeface.BOLD_ITALIC, textScaleX = 0.92f, textSkewX = -0.22f),
        FontDefinition("elegant-song", "典雅宋体", "serif", Typeface.NORMAL, textScaleX = 0.94f),
        FontDefinition("modern-hei", "现代黑体", "sans-serif-light", Typeface.NORMAL, textScaleX = 0.98f),
        FontDefinition("upright-kai", "端正楷体", "serif", Typeface.NORMAL, textScaleX = 1.02f, textSkewX = -0.02f),
        FontDefinition("slim-kai", "清瘦楷体", "serif", Typeface.NORMAL, textScaleX = 0.82f, textSkewX = -0.06f),
        FontDefinition("broad-kai", "宽碑楷体", "serif", Typeface.BOLD, textScaleX = 1.22f, textSkewX = -0.03f),
        FontDefinition("stele-kai", "碑刻楷体", "serif", Typeface.BOLD, textScaleX = 1.05f, textSkewX = 0.04f),
        FontDefinition("ritual-song", "礼仪宋体", "serif", Typeface.NORMAL, textScaleX = 0.90f),
        FontDefinition("bold-song", "厚重宋体", "serif", Typeface.BOLD, textScaleX = 1.02f),
        FontDefinition("slim-song", "清雅宋体", "serif", Typeface.NORMAL, textScaleX = 0.80f),
        FontDefinition("condensed-song", "窄宋体", "serif-condensed", Typeface.NORMAL, textScaleX = 0.86f),
        FontDefinition("modern-fang-song", "现代仿宋", "serif", Typeface.NORMAL, textScaleX = 0.91f, textSkewX = 0.02f),
        FontDefinition("narrow-fang-song", "细窄仿宋", "serif", Typeface.NORMAL, textScaleX = 0.76f, textSkewX = 0.02f),
        FontDefinition("broad-li-shu", "宽隶书", "serif", Typeface.BOLD, textScaleX = 1.28f, textSkewX = 0.04f),
        FontDefinition("square-li-shu", "方正隶意", "serif", Typeface.BOLD, textScaleX = 1.10f, textSkewX = 0.01f),
        FontDefinition("slim-li-shu", "清瘦隶意", "serif", Typeface.NORMAL, textScaleX = 1.08f, textSkewX = 0.04f),
        FontDefinition("ancient-li-shu", "古朴隶意", "serif", Typeface.BOLD, textScaleX = 1.18f, textSkewX = 0.07f),
        FontDefinition("monument-wei", "碑刻魏体", "sans-serif-black", Typeface.BOLD, textScaleX = 1.12f, textSkewX = 0.02f),
        FontDefinition("sharp-wei", "峻拔魏体", "sans-serif-condensed", Typeface.BOLD, textScaleX = 0.96f, textSkewX = -0.03f),
        FontDefinition("heavy-wei", "厚重魏体", "sans-serif-black", Typeface.BOLD, textScaleX = 1.20f),
        FontDefinition("slim-wei", "清瘦魏体", "sans-serif-condensed", Typeface.NORMAL, textScaleX = 0.84f),
        FontDefinition("elegant-xing-kai", "典雅行楷", "cursive", Typeface.ITALIC, textScaleX = 0.98f, textSkewX = -0.12f),
        FontDefinition("brisk-xing-kai", "洒脱行楷", "cursive", Typeface.BOLD_ITALIC, textScaleX = 0.94f, textSkewX = -0.26f),
        FontDefinition("broad-xing", "宽笔行书", "cursive", Typeface.BOLD, textScaleX = 1.12f, textSkewX = -0.16f),
        FontDefinition("slim-xing", "清瘦行书", "cursive", Typeface.ITALIC, textScaleX = 0.80f, textSkewX = -0.20f),
        FontDefinition("running-cao", "行草风格", "cursive", Typeface.ITALIC, textScaleX = 0.88f, textSkewX = -0.30f),
        FontDefinition("brush-cao", "毛笔草意", "cursive", Typeface.BOLD_ITALIC, textScaleX = 0.86f, textSkewX = -0.34f),
        FontDefinition("ritual-brush", "庄重毛笔体", "serif", Typeface.BOLD, textScaleX = 1.00f, textSkewX = -0.08f),
        FontDefinition("memorial-brush", "挽联书法体", "cursive", Typeface.BOLD, textScaleX = 0.97f, textSkewX = -0.18f),
        FontDefinition("square-hei", "方正黑体风", "sans-serif-medium", Typeface.BOLD, textScaleX = 1.04f),
        FontDefinition("slim-hei", "清瘦黑体", "sans-serif-thin", Typeface.NORMAL, textScaleX = 0.88f),
        FontDefinition("bold-hei", "厚重黑体", "sans-serif-black", Typeface.BOLD, textScaleX = 1.08f),
        FontDefinition("condensed-hei", "窄黑体", "sans-serif-condensed", Typeface.BOLD, textScaleX = 0.84f),
    )

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    private val _fontsFlow = MutableStateFlow(builtInFonts)
    val fontsFlow: StateFlow<List<FontDefinition>> = _fontsFlow.asStateFlow()
    val fonts: List<FontDefinition> get() = _fontsFlow.value
    val defaultFont: FontDefinition get() = builtInFonts.first()

    @Volatile
    private var appContext: Context? = null

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val imported = loadImportedFonts(context.applicationContext)
        _fontsFlow.value = builtInFonts + imported.map { it.toDefinition() }
    }

    fun hasFont(id: String): Boolean = fonts.any { it.id == id }

    fun resolve(id: String): FontDefinition = fonts.firstOrNull { it.id == id } ?: fonts.first()

    fun importFont(context: Context, uri: Uri): FontImportResult {
        val applicationContext = context.applicationContext
        initializeIfNeeded(applicationContext)
        val info = queryFontFileInfo(applicationContext, uri)
        val extension = info.name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension !in SUPPORTED_EXTENSIONS) {
            return FontImportResult.Failure("仅支持导入 TTF/OTF 字体文件。")
        }
        if (info.size != null && info.size !in MIN_FONT_BYTES..MAX_FONT_BYTES) {
            return FontImportResult.Failure("字体文件大小不合理，请选择小于 50 MB 的有效字体。")
        }

        val fontsDirectory = File(applicationContext.filesDir, FONTS_DIRECTORY).apply { mkdirs() }
        val temporaryFile = File(fontsDirectory, ".import-${UUID.randomUUID()}.tmp")
        return try {
            val copiedBytes = applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_FONT_BYTES) throw FontFileTooLargeException()
                        output.write(buffer, 0, count)
                    }
                    total
                }
            } ?: return FontImportResult.Failure(INVALID_FONT_MESSAGE)
            if (copiedBytes < MIN_FONT_BYTES || !isSupportedFontFile(temporaryFile)) {
                temporaryFile.delete()
                return FontImportResult.Failure(INVALID_FONT_MESSAGE)
            }
            val typeface = runCatching { Typeface.createFromFile(temporaryFile) }.getOrNull()
            if (typeface == null || !canRenderChinese(typeface)) {
                temporaryFile.delete()
                return FontImportResult.Failure(INVALID_FONT_MESSAGE)
            }

            val id = "imported-${UUID.randomUUID()}"
            val destination = File(fontsDirectory, "$id.$extension")
            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.copyTo(destination, overwrite = true)
                temporaryFile.delete()
            }
            val importedFont = ImportedFont(
                id = id,
                displayName = info.name.substringBeforeLast('.').ifBlank { "我的字体" },
                filePath = destination.absolutePath,
            )
            val definitions = fonts.filter { it.localFilePath != null }
                .map { ImportedFont(it.id, it.displayName, requireNotNull(it.localFilePath)) }
                .plus(importedFont)
            persistImportedFonts(applicationContext, definitions)
            typefaceCache["$id:base"] = Typeface.createFromFile(destination)
            _fontsFlow.value = builtInFonts + definitions.map { it.toDefinition() }
            FontImportResult.Success(importedFont.toDefinition())
        } catch (_: FontFileTooLargeException) {
            temporaryFile.delete()
            FontImportResult.Failure("字体文件过大，请选择小于 50 MB 的字体。")
        } catch (_: Throwable) {
            temporaryFile.delete()
            FontImportResult.Failure(INVALID_FONT_MESSAGE)
        }
    }

    @Synchronized
    fun deleteImportedFont(id: String): Boolean {
        val context = appContext ?: return false
        val target = fonts.firstOrNull { it.id == id && it.localFilePath != null } ?: return false
        val file = File(requireNotNull(target.localFilePath))
        if (file.exists() && !file.delete()) return false
        val remaining = fonts.filter { it.localFilePath != null && it.id != id }
            .map { ImportedFont(it.id, it.displayName, requireNotNull(it.localFilePath)) }
        persistImportedFonts(context, remaining)
        typefaceCache.keys.removeAll { it.startsWith("$id:") }
        _fontsFlow.value = builtInFonts + remaining.map { it.toDefinition() }
        return true
    }

    fun createPaint(
        fontId: String,
        sizeDots: Float,
        weight: TextWeight,
        color: Int = Color.WHITE,
        align: Paint.Align = Paint.Align.CENTER,
    ): Paint {
        val definition = resolve(fontId)
        val requestedStyle = if (weight == TextWeight.BOLD) {
            definition.style or Typeface.BOLD
        } else {
            definition.style
        }
        return Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = sizeDots
            textAlign = align
            typeface = typefaceFor(definition, requestedStyle)
            textScaleX = definition.textScaleX
            textSkewX = definition.textSkewX
        }
    }

    private fun typefaceFor(definition: FontDefinition, style: Int): Typeface {
        val key = "${definition.id}:$style"
        return typefaceCache[key] ?: run {
            val loaded = runCatching {
                val base = definition.localFilePath?.let { path ->
                    val baseKey = "${definition.id}:base"
                    typefaceCache[baseKey] ?: Typeface.createFromFile(path).also {
                        typefaceCache[baseKey] = it
                    }
                }
                if (base == null) {
                    Typeface.create(definition.familyName, style)
                } else {
                    Typeface.create(base, style)
                }
            }.getOrElse {
                Typeface.create(defaultFont.familyName, style)
            }
            typefaceCache[key] = loaded
            loaded
        }
    }

    private fun initializeIfNeeded(context: Context) {
        if (appContext == null) initialize(context)
    }

    private fun loadImportedFonts(context: Context): List<ImportedFont> {
        val saved = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(PREFERENCES_KEY, null)
            .orEmpty()
        if (saved.isBlank()) return emptyList()
        val parsed = runCatching {
            val array = JSONArray(saved)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val font = ImportedFont(
                        id = item.getString("id"),
                        displayName = item.optString("displayName", "我的字体"),
                        filePath = item.getString("filePath"),
                    )
                    if (File(font.filePath).isFile && runCatching {
                            Typeface.createFromFile(font.filePath)
                        }.isSuccess
                    ) {
                        add(font)
                    }
                }
            }
        }.getOrDefault(emptyList())
        persistImportedFonts(context, parsed)
        return parsed
    }

    private fun persistImportedFonts(context: Context, fonts: List<ImportedFont>) {
        val json = JSONArray().apply {
            fonts.forEach { font ->
                put(
                    JSONObject().apply {
                        put("id", font.id)
                        put("displayName", font.displayName)
                        put("filePath", font.filePath)
                    },
                )
            }
        }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCES_KEY, json.toString())
            .apply()
    }

    private fun ImportedFont.toDefinition(): FontDefinition = FontDefinition(
        id = id,
        displayName = displayName,
        familyName = "",
        style = Typeface.NORMAL,
        source = "用户自行导入",
        license = "由用户负责确认字体授权",
        localFilePath = filePath,
    )

    private fun queryFontFileInfo(context: Context, uri: Uri): FontFileInfo {
        var name = "font.ttf"
        var size: Long? = null
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return FontFileInfo(name = name, size = size)
    }

    private fun isSupportedFontFile(file: File): Boolean {
        val header = ByteArray(4)
        if (FileInputStream(file).use { it.read(header) } != header.size) return false
        val tag = String(header, Charsets.US_ASCII)
        val trueTypeVersion = header[0] == 0.toByte() && header[1] == 1.toByte() &&
            header[2] == 0.toByte() && header[3] == 0.toByte()
        return trueTypeVersion || tag == "OTTO" || tag == "true" || tag == "ttcf"
    }

    private fun canRenderChinese(typeface: Typeface): Boolean {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 64f
        }
        val bounds = Rect()
        paint.getTextBounds(CHINESE_TEST_TEXT, 0, CHINESE_TEST_TEXT.length, bounds)
        return bounds.width() > 0 && bounds.height() > 0 &&
            paint.hasGlyph("永") && paint.hasGlyph("悼")
    }

    private data class FontFileInfo(val name: String, val size: Long?)

    private class FontFileTooLargeException : RuntimeException()

    private const val PREFERENCES_NAME = "imported_fonts"
    private const val PREFERENCES_KEY = "fonts_json"
    private const val FONTS_DIRECTORY = "fonts"
    private const val MIN_FONT_BYTES = 1024L
    private const val MAX_FONT_BYTES = 50L * 1024L * 1024L
    private const val CHINESE_TEST_TEXT = "永悼"
    private const val INVALID_FONT_MESSAGE = "无法读取该字体文件，请选择有效的 TTF/OTF 字体。"
    private val SUPPORTED_EXTENSIONS = setOf("ttf", "otf")
}
