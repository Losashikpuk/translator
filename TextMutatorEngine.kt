package com.example.data.engine

import com.example.data.network.TranslationProvider
import com.example.data.network.TranslationService
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

enum class DistortionIntensity(val displayName: String, val description: String) {
    LOW("Низкая (Мягкая)", "Перевод через близкие языки, сохранение общего смысла с лёгким акцентом"),
    MEDIUM("Средняя (Заметная)", "Перевод через разнородные языковые семьи, потеря идиом и смысловой дрифт"),
    HIGH("Высокая (Сильная)", "Редкие языки, синтаксический сдвиг, синонимические мутации"),
    CHAOS("Хаос (Глитч)", "Экстремальная трансформация, фонетический сдвиг, глитч-эффекты и эмодзи-вкрапления")
}

data class TransformationHop(
    val stepIndex: Int,
    val fromLang: String,
    val toLang: String,
    val text: String,
    val engineName: String,
    val durationMs: Long,
    val isSuccess: Boolean
)

data class TransformationPipelineResult(
    val originalText: String,
    val finalText: String,
    val hops: List<TransformationHop>,
    val totalDurationMs: Long
)

class TextMutatorEngine(private val translationService: TranslationService = TranslationService()) {
    private val random = Random()

    private val closeLanguages = listOf("en", "de", "fr", "es", "it", "nl")
    private val exoticLanguages = listOf("ja", "zh", "ar", "hi", "fi", "tr", "ko")
    private val extremeLanguages = listOf("la", "zu", "is", "eo", "el", "sw", "ga")

    suspend fun mutateText(
        inputText: String,
        sourceLang: String,
        targetLang: String,
        hopsCount: Int,
        intensity: DistortionIntensity,
        provider: TranslationProvider,
        onHopCompleted: (TransformationHop) -> Unit = {}
    ): TransformationPipelineResult {
        val totalStart = System.currentTimeMillis()
        val hopsList = mutableListOf<TransformationHop>()

        val safeHopsCount = hopsCount.coerceIn(2, 25)
        val intermediateLanguages = selectIntermediateLanguages(sourceLang, targetLang, safeHopsCount, intensity)

        var currentText = inputText.trim()
        var currentLang = sourceLang

        for (i in 0 until safeHopsCount) {
            val nextLang = if (i == safeHopsCount - 1) targetLang else intermediateLanguages[i]
            val hopStart = System.currentTimeMillis()

            // 1. Try real translation via API
            var result = translationService.translate(currentText, currentLang, nextLang, provider)

            var stepText = if (result.isSuccess && result.translatedText.isNotBlank()) {
                result.translatedText
            } else {
                // Offline fallback linguistic mutation
                applyOfflineMutation(currentText, intensity, i, nextLang)
            }

            // 2. Apply intensity-based post-hop mutation
            stepText = applyIntensityEffects(stepText, intensity, i, isFinalHop = (i == safeHopsCount - 1))

            val hopDuration = System.currentTimeMillis() - hopStart
            val hop = TransformationHop(
                stepIndex = i + 1,
                fromLang = currentLang,
                toLang = nextLang,
                text = stepText,
                engineName = if (result.isSuccess) result.providerUsed else "Linguistic Mutation Engine",
                durationMs = hopDuration,
                isSuccess = true
            )

            hopsList.add(hop)
            onHopCompleted(hop)

            currentText = stepText
            currentLang = nextLang

            // Gentle delay between hops to be kind to free rate limits
            delay(120)
        }

        return TransformationPipelineResult(
            originalText = inputText,
            finalText = currentText,
            hops = hopsList,
            totalDurationMs = System.currentTimeMillis() - totalStart
        )
    }

    private fun selectIntermediateLanguages(
        sourceLang: String,
        targetLang: String,
        count: Int,
        intensity: DistortionIntensity
    ): List<String> {
        val pool = when (intensity) {
            DistortionIntensity.LOW -> closeLanguages
            DistortionIntensity.MEDIUM -> closeLanguages + exoticLanguages
            DistortionIntensity.HIGH -> exoticLanguages + extremeLanguages
            DistortionIntensity.CHAOS -> closeLanguages + exoticLanguages + extremeLanguages
        }.filter { it != sourceLang && it != targetLang }.shuffled()

        val selected = mutableListOf<String>()
        for (i in 0 until count - 1) {
            val lang = pool[i % pool.size]
            selected.add(lang)
        }
        return selected
    }

    private fun applyIntensityEffects(
        text: String,
        intensity: DistortionIntensity,
        stepIndex: Int,
        isFinalHop: Boolean
    ): String {
        return when (intensity) {
            DistortionIntensity.LOW -> text
            DistortionIntensity.MEDIUM -> {
                // Occasional subtle synonym/phrasing shift
                if (stepIndex % 3 == 0) subtleSynonymShift(text) else text
            }
            DistortionIntensity.HIGH -> {
                // Pronunciation shift or word order nudge
                val shifted = subtleSynonymShift(text)
                if (random.nextInt(3) == 0) addLinguisticColor(shifted) else shifted
            }
            DistortionIntensity.CHAOS -> {
                // Glitch characters, zalgo, phonetic echoes or emoji accents
                applyChaosGlitch(text, isFinalHop)
            }
        }
    }

    private fun subtleSynonymShift(text: String): String {
        val replacements = mapOf(
            "очень" to "чрезвычайно",
            "хорошо" to "превосходно",
            "быстро" to "стремительно",
            "сказал" to "молвил",
            "думать" to "размышлять",
            "человек" to "персонаж",
            "мир" to "вселенная",
            "проблема" to "головоломка",
            "very" to "exceedingly",
            "good" to "splendid",
            "fast" to "swiftly",
            "said" to "proclaimed",
            "think" to "ponder"
        )
        var result = text
        for ((k, v) in replacements) {
            if (result.contains(k, ignoreCase = true) && random.nextBoolean()) {
                result = result.replace(Regex("(?i)\\b$k\\b"), v)
            }
        }
        return result
    }

    private fun addLinguisticColor(text: String): String {
        val particles = listOf("воистину", "пожалуй", "несомненно", "говорят", "вероятно", "дескать")
        val words = text.split(" ")
        if (words.size > 3) {
            val insertPos = random.nextInt(words.size - 1) + 1
            val particle = particles[random.nextInt(particles.size)]
            val list = words.toMutableList()
            list.add(insertPos, particle)
            return list.joinToString(" ")
        }
        return text
    }

    private fun applyChaosGlitch(text: String, isFinalHop: Boolean): String {
        val glitchMarks = listOf('\u0300', '\u0301', '\u0302', '\u0303', '\u0308', '\u0336', '\u0337', '\u035C')
        val chaosEmojis = listOf("⚡", "🌀", "👁️", "🔀", "👾", "✨", "📡", "🪐", "🔥")

        val sb = java.lang.StringBuilder()
        for (ch in text) {
            sb.append(ch)
            // 8% chance of zalgo diacritic accent
            if (ch.isLetter() && random.nextFloat() < 0.08f) {
                sb.append(glitchMarks[random.nextInt(glitchMarks.size)])
            }
        }

        var result = sb.toString()
        if (isFinalHop && random.nextFloat() < 0.4f) {
            // Add a chaos emoji accent
            result += " " + chaosEmojis[random.nextInt(chaosEmojis.size)]
        }
        return result
    }

    private fun applyOfflineMutation(
        text: String,
        intensity: DistortionIntensity,
        stepIndex: Int,
        targetLang: String
    ): String {
        // High quality offline fallback with language stylistic transforms
        val words = text.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return text

        return when (targetLang) {
            "ja" -> {
                // Japanese linguistic echo
                words.joinToString(" ") { "$it-сан" }
            }
            "de" -> {
                // German-like compounding
                if (words.size >= 2) words.chunked(2).joinToString(" ") { it.joinToString("") } else text
            }
            "la" -> {
                // Latinized endings
                words.joinToString(" ") {
                    if (it.length > 3) it.dropLast(1) + "us" else it
                }
            }
            "fr" -> {
                // French aesthetic
                "Le " + words.joinToString(" ")
            }
            else -> {
                words.shuffled(random).joinToString(" ")
            }
        }
    }

    fun serializeHopsToJson(hops: List<TransformationHop>): String {
        val jsonArray = JSONArray()
        for (h in hops) {
            val obj = JSONObject()
            obj.put("step", h.stepIndex)
            obj.put("from", h.fromLang)
            obj.put("to", h.toLang)
            obj.put("text", h.text)
            obj.put("engine", h.engineName)
            obj.put("duration", h.durationMs)
            obj.put("success", h.isSuccess)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserializeHopsFromJson(json: String): List<TransformationHop> {
        val list = mutableListOf<TransformationHop>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TransformationHop(
                        stepIndex = obj.optInt("step", i + 1),
                        fromLang = obj.optString("from", ""),
                        toLang = obj.optString("to", ""),
                        text = obj.optString("text", ""),
                        engineName = obj.optString("engine", "Google GTX"),
                        durationMs = obj.optLong("duration", 0L),
                        isSuccess = obj.optBoolean("success", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
