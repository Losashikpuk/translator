package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class TranslationProvider(val displayName: String) {
    GOOGLE_GTX("Google Translate (Free)"),
    MY_MEMORY("MyMemory API (Free)"),
    LINGVA("Lingva Public Mirror"),
    AUTO_CHAIN("Все библиотеки (Мульти-движок)")
}

data class TranslationResult(
    val translatedText: String,
    val providerUsed: String,
    val isSuccess: Boolean,
    val durationMs: Long
)

class TranslationService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun translate(
        text: String,
        fromLang: String,
        toLang: String,
        preferredProvider: TranslationProvider = TranslationProvider.AUTO_CHAIN
    ): TranslationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (text.isBlank() || fromLang.equals(toLang, ignoreCase = true)) {
            return@withContext TranslationResult(
                translatedText = text,
                providerUsed = "Identical",
                isSuccess = true,
                durationMs = 0
            )
        }

        when (preferredProvider) {
            TranslationProvider.GOOGLE_GTX -> translateViaGoogle(text, fromLang, toLang, startTime)
            TranslationProvider.MY_MEMORY -> translateViaMyMemory(text, fromLang, toLang, startTime)
            TranslationProvider.LINGVA -> translateViaLingva(text, fromLang, toLang, startTime)
            TranslationProvider.AUTO_CHAIN -> {
                // Try Google GTX first
                val gtx = translateViaGoogle(text, fromLang, toLang, startTime)
                if (gtx.isSuccess) return@withContext gtx

                // Fallback to MyMemory
                val myMem = translateViaMyMemory(text, fromLang, toLang, startTime)
                if (myMem.isSuccess) return@withContext myMem

                // Fallback to Lingva
                val lingva = translateViaLingva(text, fromLang, toLang, startTime)
                if (lingva.isSuccess) return@withContext lingva

                // Return failed with original text
                TranslationResult(
                    translatedText = text,
                    providerUsed = "Fallback",
                    isSuccess = false,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    private fun translateViaGoogle(
        text: String,
        fromLang: String,
        toLang: String,
        startTime: Long
    ): TranslationResult {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$fromLang&tl=$toLang&dt=t&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return TranslationResult(text, "Google GTX (HTTP ${response.code})", false, System.currentTimeMillis() - startTime)
                }
                val body = response.body?.string() ?: return TranslationResult(text, "Google GTX (Empty)", false, System.currentTimeMillis() - startTime)
                val jsonArray = JSONArray(body)
                val sentences = jsonArray.optJSONArray(0)
                if (sentences != null && sentences.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val part = sentences.optJSONArray(i)?.optString(0)
                        if (!part.isNullOrEmpty()) {
                            sb.append(part)
                        }
                    }
                    val result = sb.toString().trim()
                    if (result.isNotEmpty()) {
                        return TranslationResult(result, "Google GTX", true, System.currentTimeMillis() - startTime)
                    }
                }
                TranslationResult(text, "Google GTX (Parse Error)", false, System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            TranslationResult(text, "Google GTX (${e.javaClass.simpleName})", false, System.currentTimeMillis() - startTime)
        }
    }

    private fun translateViaMyMemory(
        text: String,
        fromLang: String,
        toLang: String,
        startTime: Long
    ): TranslationResult {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$fromLang|$toLang"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TransMutateApp/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return TranslationResult(text, "MyMemory (HTTP ${response.code})", false, System.currentTimeMillis() - startTime)
                }
                val body = response.body?.string() ?: return TranslationResult(text, "MyMemory (Empty)", false, System.currentTimeMillis() - startTime)
                val json = JSONObject(body)
                val responseData = json.optJSONObject("responseData")
                val translatedText = responseData?.optString("translatedText")
                if (!translatedText.isNullOrBlank() && !translatedText.contains("MYMEMORY WARNING")) {
                    return TranslationResult(
                        android.text.Html.fromHtml(translatedText, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim(),
                        "MyMemory Translated",
                        true,
                        System.currentTimeMillis() - startTime
                    )
                }
                TranslationResult(text, "MyMemory (Invalid)", false, System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            TranslationResult(text, "MyMemory (${e.javaClass.simpleName})", false, System.currentTimeMillis() - startTime)
        }
    }

    private fun translateViaLingva(
        text: String,
        fromLang: String,
        toLang: String,
        startTime: Long
    ): TranslationResult {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://lingva.ml/api/v1/$fromLang/$toLang/$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return TranslationResult(text, "Lingva (HTTP ${response.code})", false, System.currentTimeMillis() - startTime)
                }
                val body = response.body?.string() ?: return TranslationResult(text, "Lingva (Empty)", false, System.currentTimeMillis() - startTime)
                val json = JSONObject(body)
                val translation = json.optString("translation")
                if (!translation.isNullOrBlank()) {
                    return TranslationResult(translation.trim(), "Lingva ML", true, System.currentTimeMillis() - startTime)
                }
                TranslationResult(text, "Lingva (Invalid)", false, System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            TranslationResult(text, "Lingva (${e.javaClass.simpleName})", false, System.currentTimeMillis() - startTime)
        }
    }
}
