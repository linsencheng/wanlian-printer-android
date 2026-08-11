package com.wanlian.printer.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wanlian.printer.model.BluetoothTransport
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderTemplate
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.CoupletPairDocument
import com.wanlian.printer.model.CoupletSide
import com.wanlian.printer.model.CutGuideSettings
import com.wanlian.printer.model.CutGuideStyle
import com.wanlian.printer.model.DevicePreferences
import com.wanlian.printer.model.DocumentMode
import com.wanlian.printer.model.FlowerSettings
import com.wanlian.printer.model.FlowerStyle
import com.wanlian.printer.model.FooterLabelSettings
import com.wanlian.printer.model.FooterPerson
import com.wanlian.printer.model.PersonLayoutState
import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.model.TextHorizontalAlignment
import com.wanlian.printer.model.TextWeight
import com.wanlian.printer.model.TemplateNameRules
import com.wanlian.printer.model.personLayoutState
import com.wanlian.printer.model.renamedTo
import com.wanlian.printer.model.withPersonLayoutState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private val Context.wanlianDataStore by preferencesDataStore(name = "wanlian_local_data")

class TemplateRepository(context: Context) {
    private val dataStore = context.applicationContext.wanlianDataStore

    val templates: Flow<List<CoupletTemplate>> = safeData().map { preferences ->
        parseTemplates(preferences[TEMPLATES_JSON].orEmpty())
    }

    val devicePreferences: Flow<DevicePreferences> = safeData().map { preferences ->
        DevicePreferences(
            lastDevice = parseDevice(preferences[LAST_DEVICE_JSON]),
            autoReconnect = preferences[AUTO_RECONNECT] ?: true,
        )
    }

    suspend fun saveTemplate(
        name: String,
        settings: PrintSettings,
        documentMode: DocumentMode = DocumentMode.SINGLE,
        pairDocument: CoupletPairDocument? = null,
        id: String? = null,
    ): String? {
        val normalizedName = TemplateNameRules.normalize(name)
        if (normalizedName.isEmpty()) return null
        val templateId = id ?: UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            val current = parseTemplates(preferences[TEMPLATES_JSON].orEmpty()).toMutableList()
            val savedTemplate = CoupletTemplate(
                id = templateId,
                name = normalizedName,
                settings = settings,
                updatedAt = System.currentTimeMillis(),
                documentMode = documentMode,
                pairDocument = pairDocument,
            )
            val existingIndex = current.indexOfFirst { it.id == templateId }
            if (existingIndex >= 0) {
                current[existingIndex] = savedTemplate
            } else {
                current += savedTemplate
            }
            preferences[TEMPLATES_JSON] = templatesToJson(current.sortedByDescending { it.updatedAt })
        }
        return templateId
    }

    suspend fun renameTemplate(id: String, name: String): Boolean {
        val normalizedName = TemplateNameRules.normalize(name)
        if (normalizedName.isEmpty()) return false
        var renamed = false
        dataStore.edit { preferences ->
            val current = parseTemplates(preferences[TEMPLATES_JSON].orEmpty()).toMutableList()
            val templateIndex = current.indexOfFirst { it.id == id }
            if (templateIndex >= 0) {
                current[templateIndex].renamedTo(normalizedName)?.let { updated ->
                    current[templateIndex] = updated
                    renamed = true
                }
            }
            if (renamed) preferences[TEMPLATES_JSON] = templatesToJson(current)
        }
        return renamed
    }

    suspend fun deleteTemplate(id: String) {
        dataStore.edit { preferences ->
            val remaining = parseTemplates(preferences[TEMPLATES_JSON].orEmpty())
                .filterNot { it.id == id }
            preferences[TEMPLATES_JSON] = templatesToJson(remaining)
        }
    }

    suspend fun saveLastDevice(device: PrinterDevice) {
        dataStore.edit { preferences ->
            preferences[LAST_DEVICE_JSON] = deviceToJson(device).toString()
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[AUTO_RECONNECT] = enabled }
    }

    private fun safeData(): Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    private fun templatesToJson(templates: List<CoupletTemplate>): String = JSONArray().apply {
        templates.forEach { template ->
            put(
                JSONObject()
                    .put("id", template.id)
                    .put("name", template.name)
                    .put("updatedAt", template.updatedAt)
                    .put("documentMode", template.documentMode.name)
                    .put("settings", settingsToJson(template.settings))
                    .apply {
                        template.pairDocument?.let { put("pairDocument", pairToJson(it)) }
                    },
            )
        }
    }.toString()

    private fun parseTemplates(json: String): List<CoupletTemplate> = runCatching {
        val array = if (json.isBlank()) JSONArray() else JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    CoupletTemplate(
                        id = item.getString("id"),
                        name = item.optString("name", "未命名模板"),
                        settings = settingsFromJson(item.getJSONObject("settings")),
                        updatedAt = item.optLong("updatedAt", 0L),
                        documentMode = enumOrDefault(
                            item.optString("documentMode"),
                            DocumentMode.SINGLE,
                        ),
                        pairDocument = item.optJSONObject("pairDocument")?.let(::pairFromJson),
                    ),
                )
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    private fun pairToJson(document: CoupletPairDocument): JSONObject = JSONObject().apply {
        put("left", settingsToJson(document.left))
        put("right", settingsToJson(document.right))
        put("selectedSide", document.selectedSide.name)
        put("sharedBorderSettings", document.sharedBorderSettings)
        put("sharedFooterLayout", document.sharedFooterLayout)
        put("sharedPageLength", document.sharedPageLength)
    }

    private fun pairFromJson(json: JSONObject): CoupletPairDocument {
        val left = settingsFromJson(json.getJSONObject("left"))
        val right = settingsFromJson(json.getJSONObject("right"))
        return CoupletPairDocument(
            left = left,
            right = right,
            selectedSide = enumOrDefault(json.optString("selectedSide"), CoupletSide.LEFT),
            sharedBorderSettings = json.optBoolean("sharedBorderSettings", true),
            sharedFooterLayout = json.optBoolean("sharedFooterLayout", true),
            sharedPageLength = json.optBoolean("sharedPageLength", true),
        )
    }

    private fun settingsToJson(settings: PrintSettings): JSONObject = JSONObject().apply {
        put("text", settings.text)
        put("fontId", settings.fontId)
        put("autoFontSize", settings.autoFontSize)
        put("fontSizeDots", settings.fontSizeDots)
        put("characterSpacingDots", settings.characterSpacingDots)
        put("topMarginMm", settings.topMarginMm)
        put("bottomMarginMm", settings.bottomMarginMm)
        put("paperWidthMm", settings.paperWidthMm)
        put("autoPaperLength", settings.autoPaperLength)
        put("paperLengthMm", settings.paperLengthMm)
        put("preferredAutoLengthMm", settings.preferredAutoLengthMm)
        put("density", settings.density)
        put("speed", settings.speedInchesPerSecond)
        put("threshold", settings.threshold)
        put("textAlignment", settings.textAlignment.name)
        put("textWeight", settings.textWeight.name)
        put("printDirection", settings.printDirection.name)
        put("reversePrinting", settings.reversePrinting)
        put("bitmapChunkSize", settings.bitmapChunkSize)
        put("chunkDelayMs", settings.chunkDelayMs)
        put("border", borderToJson(settings.border))
        put("footerLabel", footerLabelToJson(settings.footerLabel))
        put("cutGuide", cutGuideToJson(settings.cutGuide))
    }

    private fun settingsFromJson(json: JSONObject): PrintSettings {
        val defaults = PrintSettings()
        return defaults.copy(
            text = json.optString("text", defaults.text),
            fontId = json.optString("fontId", defaults.fontId),
            autoFontSize = json.optBoolean("autoFontSize", defaults.autoFontSize),
            fontSizeDots = json.optDouble("fontSizeDots", defaults.fontSizeDots.toDouble()).toFloat(),
            characterSpacingDots = json.optDouble(
                "characterSpacingDots",
                defaults.characterSpacingDots.toDouble(),
            ).toFloat(),
            topMarginMm = json.optDouble("topMarginMm", defaults.topMarginMm.toDouble()).toFloat(),
            bottomMarginMm = json.optDouble("bottomMarginMm", defaults.bottomMarginMm.toDouble()).toFloat(),
            paperWidthMm = json.optDouble("paperWidthMm", defaults.paperWidthMm.toDouble()).toFloat(),
            autoPaperLength = json.optBoolean("autoPaperLength", defaults.autoPaperLength),
            paperLengthMm = json.optDouble("paperLengthMm", defaults.paperLengthMm.toDouble()).toFloat(),
            preferredAutoLengthMm = json.optDouble(
                "preferredAutoLengthMm",
                defaults.preferredAutoLengthMm.toDouble(),
            ).toFloat(),
            density = json.optInt("density", defaults.density),
            speedInchesPerSecond = json.optDouble("speed", defaults.speedInchesPerSecond.toDouble()).toFloat(),
            threshold = json.optInt("threshold", defaults.threshold),
            textAlignment = enumOrDefault(json.optString("textAlignment"), defaults.textAlignment),
            textWeight = enumOrDefault(json.optString("textWeight"), defaults.textWeight),
            printDirection = enumOrDefault(json.optString("printDirection"), defaults.printDirection),
            reversePrinting = json.optBoolean("reversePrinting", defaults.reversePrinting),
            bitmapChunkSize = json.optInt("bitmapChunkSize", defaults.bitmapChunkSize),
            chunkDelayMs = json.optLong("chunkDelayMs", defaults.chunkDelayMs),
            border = json.optJSONObject("border")?.let(::borderFromJson) ?: defaults.border,
            footerLabel = json.optJSONObject("footerLabel")?.let(::footerLabelFromJson)
                ?: defaults.footerLabel,
            cutGuide = json.optJSONObject("cutGuide")?.let(::cutGuideFromJson)
                ?: defaults.cutGuide,
        )
    }

    private fun footerLabelToJson(settings: FooterLabelSettings): JSONObject {
        val personState = settings.personLayoutState()
        return JSONObject().apply {
        put("enabled", settings.enabled)
        put("text", settings.text)
        put("secondaryText", settings.secondaryText)
        put("fontId", settings.fontId)
        put("fontSizeDots", settings.fontSizeDots)
        put("spacingDots", settings.spacingDots)
        put("distanceFromMainMm", settings.distanceFromMainMm)
        put("bottomMarginMm", settings.bottomMarginMm)
        put("position", settings.position.name)
        put("orientation", settings.orientation.name)
        put("personLayout", personState.layout.name)
        put("personColumnGapMm", personState.columnGapMm)
        put("personGroupOffsetXMm", personState.groupOffsetXMm)
        put("personGroupOffsetYMm", personState.groupOffsetYMm)
        put("personFontId", personState.fontId)
        put("personFontSizeDots", personState.fontSizeDots)
        put("persons", JSONArray().apply {
            personState.persons.forEach { person ->
                put(JSONObject().apply {
                    put("id", person.id)
                    put("relation", person.relation)
                    put("name", person.name)
                })
            }
        })
        put("flower", flowerToJson(settings.flower))
        }
    }

    private fun footerLabelFromJson(json: JSONObject): FooterLabelSettings {
        val defaults = FooterLabelSettings()
        val personState = PersonLayoutState(
            layout = enumOrDefault(json.optString("personLayout"), defaults.personLayout),
            columnGapMm = json.optDouble(
                "personColumnGapMm",
                defaults.personColumnGapMm.toDouble(),
            ).toFloat(),
            groupOffsetXMm = json.optDouble(
                "personGroupOffsetXMm",
                defaults.personGroupOffsetXMm.toDouble(),
            ).toFloat(),
            groupOffsetYMm = json.optDouble(
                "personGroupOffsetYMm",
                defaults.personGroupOffsetYMm.toDouble(),
            ).toFloat(),
            fontId = json.optString("personFontId", defaults.personFontId),
            fontSizeDots = json.optDouble(
                "personFontSizeDots",
                defaults.personFontSizeDots.toDouble(),
            ).toFloat(),
            persons = json.optJSONArray("persons")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        add(
                            FooterPerson(
                                relation = item.optString("relation"),
                                name = item.optString("name"),
                                id = item.optString("id").ifBlank { "legacy-person-$index" },
                            ),
                        )
                    }
                }
            } ?: defaults.persons,
        )
        return defaults.copy(
            enabled = json.optBoolean("enabled", defaults.enabled),
            text = json.optString("text", defaults.text),
            secondaryText = json.optString("secondaryText", defaults.secondaryText),
            fontId = json.optString("fontId", defaults.fontId),
            fontSizeDots = json.optDouble("fontSizeDots", defaults.fontSizeDots.toDouble()).toFloat(),
            spacingDots = json.optDouble("spacingDots", defaults.spacingDots.toDouble()).toFloat(),
            distanceFromMainMm = json.optDouble(
                "distanceFromMainMm",
                defaults.distanceFromMainMm.toDouble(),
            ).toFloat(),
            bottomMarginMm = json.optDouble(
                "bottomMarginMm",
                defaults.bottomMarginMm.toDouble(),
            ).toFloat(),
            position = enumOrDefault(json.optString("position"), defaults.position),
            orientation = enumOrDefault(json.optString("orientation"), defaults.orientation),
            flower = json.optJSONObject("flower")?.let(::flowerFromJson) ?: defaults.flower.copy(
                style = legacyFlowerStyle(json.optString("flowerStyle"), defaults.flower.style),
            ),
        ).withPersonLayoutState(personState)
    }

    private fun flowerToJson(settings: FlowerSettings): JSONObject = JSONObject().apply {
        put("enabled", settings.enabled)
        put("style", settings.style.name)
        put("sizeMm", settings.sizeMm)
        put("offsetXmm", settings.offsetXmm)
        put("offsetYmm", settings.offsetYmm)
        put("rotationDegrees", settings.rotationDegrees)
    }

    private fun flowerFromJson(json: JSONObject): FlowerSettings {
        val defaults = FlowerSettings()
        return defaults.copy(
            enabled = json.optBoolean("enabled", defaults.enabled),
            style = legacyFlowerStyle(json.optString("style"), defaults.style),
            sizeMm = json.optDouble("sizeMm", defaults.sizeMm.toDouble()).toFloat(),
            offsetXmm = json.optDouble("offsetXmm", defaults.offsetXmm.toDouble()).toFloat(),
            offsetYmm = json.optDouble("offsetYmm", defaults.offsetYmm.toDouble()).toFloat(),
            rotationDegrees = json.optDouble(
                "rotationDegrees",
                defaults.rotationDegrees.toDouble(),
            ).toFloat(),
        )
    }

    private fun legacyFlowerStyle(value: String, default: FlowerStyle): FlowerStyle = when (value) {
        "SIMPLE_CHRYSANTHEMUM" -> FlowerStyle.CHRYSANTHEMUM_SINGLE
        else -> enumOrDefault(value, default)
    }

    private fun cutGuideToJson(settings: CutGuideSettings): JSONObject = JSONObject().apply {
        put("enabled", settings.enabled)
        put("style", settings.style.name)
        put("bottomOffsetMm", settings.bottomOffsetMm)
        put("edgeInsetMm", settings.edgeInsetMm)
        put("notchDepthMm", settings.notchDepthMm)
        put("lineWidthMm", settings.lineWidthMm)
    }

    private fun cutGuideFromJson(json: JSONObject): CutGuideSettings {
        val defaults = CutGuideSettings()
        val savedStyle = json.optString("style")
        val legacyDisabled = savedStyle == "NONE"
        return defaults.copy(
            enabled = if (json.has("enabled")) {
                json.optBoolean("enabled", defaults.enabled)
            } else {
                savedStyle.isNotBlank() && !legacyDisabled
            },
            style = if (legacyDisabled) defaults.style else enumOrDefault(savedStyle, defaults.style),
            bottomOffsetMm = json.optDouble(
                "bottomOffsetMm",
                json.optDouble("offsetFromBottomMm", defaults.bottomOffsetMm.toDouble()),
            ).toFloat(),
            edgeInsetMm = json.optDouble(
                "edgeInsetMm",
                defaults.edgeInsetMm.toDouble(),
            ).toFloat(),
            notchDepthMm = json.optDouble(
                "notchDepthMm",
                defaults.notchDepthMm.toDouble(),
            ).toFloat(),
            lineWidthMm = json.optDouble(
                "lineWidthMm",
                defaults.lineWidthMm.toDouble(),
            ).toFloat(),
        )
    }

    private fun borderToJson(border: BorderSettings): JSONObject = JSONObject().apply {
        put("style", border.style.name)
        put("position", border.position.name)
        put("widthMm", border.widthMm)
        put("edgeInsetMm", border.edgeInsetMm)
        put("strokeWidthMm", border.strokeWidthMm)
        put("patternUnitHeightMm", border.patternUnitHeightMm)
        put("textGapMm", border.textGapMm)
    }

    private fun borderFromJson(json: JSONObject): BorderSettings {
        val defaults = BorderSettings()
        return defaults.copy(
            style = borderTemplateOrDefault(json.optString("style"), defaults.style),
            position = enumOrDefault(json.optString("position"), defaults.position),
            widthMm = json.optDouble("widthMm", defaults.widthMm.toDouble()).toFloat(),
            edgeInsetMm = json.optDouble("edgeInsetMm", defaults.edgeInsetMm.toDouble()).toFloat(),
            strokeWidthMm = json.optDouble("strokeWidthMm", defaults.strokeWidthMm.toDouble()).toFloat(),
            patternUnitHeightMm = json.optDouble(
                "patternUnitHeightMm",
                defaults.patternUnitHeightMm.toDouble(),
            ).toFloat(),
            textGapMm = json.optDouble("textGapMm", defaults.textGapMm.toDouble()).toFloat(),
        )
    }

    private fun deviceToJson(device: PrinterDevice): JSONObject = JSONObject().apply {
        put("name", device.name)
        put("address", device.address)
        put("bonded", device.bonded)
        put("transports", JSONArray(device.transports.map { it.name }))
    }

    private fun parseDevice(json: String?): PrinterDevice? = runCatching {
        if (json.isNullOrBlank()) return@runCatching null
        val objectValue = JSONObject(json)
        val transportArray = objectValue.getJSONArray("transports")
        val transports = buildSet {
            for (index in 0 until transportArray.length()) {
                runCatching { add(BluetoothTransport.valueOf(transportArray.getString(index))) }
            }
        }
        PrinterDevice(
            name = objectValue.optString("name", ""),
            address = objectValue.getString("address"),
            transports = transports.ifEmpty { setOf(BluetoothTransport.CLASSIC) },
            bonded = objectValue.optBoolean("bonded", false),
        )
    }.getOrNull()

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun borderTemplateOrDefault(
        value: String,
        default: BorderTemplate,
    ): BorderTemplate = when (value) {
        // Names written by releases before the expanded template library.
        "WAVE" -> BorderTemplate.WAVE_THIN
        "CLOUD" -> BorderTemplate.CLOUD_SOFT
        "SCALLOP" -> BorderTemplate.SCALLOP_SMALL
        "SWIRL" -> BorderTemplate.SWIRL_THIN
        else -> runCatching { BorderTemplate.valueOf(value) }.getOrDefault(default)
    }

    companion object {
        private val TEMPLATES_JSON = stringPreferencesKey("templates_json")
        private val LAST_DEVICE_JSON = stringPreferencesKey("last_device_json")
        private val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    }
}
