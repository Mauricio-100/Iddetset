package com.example.utils

import android.text.Html
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bush.translator.Language
import me.bush.translator.Translator
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object TranslationHelper {
    private const val TAG = "TranslationHelper"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val bushTranslator by lazy { Translator() }

    private val languageCodeMap = mapOf(
        "french" to "fr", "français" to "fr", "fr" to "fr",
        "english" to "en", "anglais" to "en", "en" to "en",
        "spanish" to "es", "espagnol" to "es", "es" to "es",
        "german" to "de", "allemand" to "de", "de" to "de",
        "italian" to "it", "italien" to "it", "it" to "it",
        "portuguese" to "pt", "portugais" to "pt", "pt" to "pt",
        "russian" to "ru", "russe" to "ru", "ru" to "ru",
        "chinese" to "zh", "chinois" to "zh", "zh" to "zh",
        "japanese" to "ja", "japonais" to "ja", "ja" to "ja",
        "arabic" to "ar", "arabe" to "ar", "ar" to "ar",
        "dutch" to "nl", "néerlandais" to "nl", "nl" to "nl",
        "korean" to "ko", "coréen" to "ko", "ko" to "ko",
        "hindi" to "hi", "hi" to "hi",
        "turkish" to "tr", "turc" to "tr", "tr" to "tr",
        "polish" to "pl", "polonais" to "pl", "pl" to "pl",
        "ukrainian" to "uk", "ukrainien" to "uk", "uk" to "uk",
        "swedish" to "sv", "suédois" to "sv", "sv" to "sv",
        "vietnamese" to "vi", "vietnamien" to "vi", "vi" to "vi",
        "thai" to "th", "thaïlandais" to "th", "th" to "th",
        "indonesian" to "id", "indonésien" to "id", "id" to "id",
        "malay" to "ms", "malais" to "ms", "ms" to "ms",
        "greek" to "el", "grec" to "el", "el" to "el",
        "czech" to "cs", "tchèque" to "cs", "cs" to "cs",
        "romanian" to "ro", "roumain" to "ro", "ro" to "ro",
        "hungarian" to "hu", "hongrois" to "hu", "hu" to "hu",
        "danish" to "da", "danois" to "da", "da" to "da",
        "finnish" to "fi", "finnois" to "fi", "fi" to "fi",
        "norwegian" to "no", "norvégien" to "no", "no" to "no",
        "hebrew" to "he", "hébreu" to "he", "he" to "he",
        "persian" to "fa", "farsi" to "fa", "persan" to "fa", "fa" to "fa",
        "bengali" to "bn", "bengali" to "bn", "bn" to "bn",
        "punjabi" to "pa", "pendjabi" to "pa", "pa" to "pa",
        "tamil" to "ta", "tamoul" to "ta", "ta" to "ta",
        "telugu" to "te", "télougou" to "te", "te" to "te",
        "gujarati" to "gu", "gujaratis" to "gu", "gu" to "gu",
        "swahili" to "sw", "swahili" to "sw", "sw" to "sw",
        "tagalog" to "tl", "filipino" to "tl", "tl" to "tl",
        "catalan" to "ca", "catalan" to "ca", "ca" to "ca",
        "croatian" to "hr", "croate" to "hr", "hr" to "hr",
        "slovak" to "sk", "slovaque" to "sk", "sk" to "sk",
        "bulgarian" to "bg", "bulgare" to "bg", "bg" to "bg",
        "serbian" to "sr", "serbe" to "sr", "sr" to "sr"
    )

    /**
     * List of 40+ supported languages provided by the engine.
     */
    val supportedLanguages: List<String> by lazy {
        val baseList = listOf(
            "Français", "Anglais", "Espagnol", "Allemand", "Italien",
            "Portugais", "Russe", "Chinois", "Japonais", "Arabe",
            "Néerlandais", "Coréen", "Hindi", "Turc", "Polonais",
            "Ukrainien", "Suédois", "Vietnamien", "Thaïlandais", "Indonésien",
            "Malais", "Grec", "Tchèque", "Roumain", "Hongrois",
            "Danois", "Finnois", "Norvégien", "Hébreu", "Persan",
            "Bengali", "Pendjabi", "Tamoul", "Télougou", "Gujarati",
            "Swahili", "Filipino", "Catalan", "Croate", "Slovaque",
            "Bulgare", "Serbe"
        )
        val bushList = try {
            Language.values().map { it.name.lowercase().capitalize() }
        } catch (_: Exception) {
            emptyList()
        }
        (baseList + bushList).distinct().sorted()
    }

    fun getLanguageCode(targetLanguageName: String): String {
        val clean = targetLanguageName.trim().lowercase()
        languageCodeMap[clean]?.let { return it }
        
        // Try finding by Language enum
        try {
            val enumMatch = Language.values().find {
                it.name.equals(clean, ignoreCase = true) || it.toString().equals(clean, ignoreCase = true)
            }
            if (enumMatch != null) {
                val code = enumMatch.name.lowercase().take(2)
                return code
            }
        } catch (_: Exception) {}
        
        return "fr"
    }

    /**
     * Translates a given text to the target language name using resilient multi-tier fallback.
     */
    suspend fun translateText(text: String, targetLanguageName: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext text

        val targetCode = getLanguageCode(targetLanguageName)

        // 1. Try Primary: Lingva ML API (fast, reliable, auto-detects source)
        try {
            val result = translateViaLingva(trimmed, targetCode, "https://lingva.ml/api/v1/auto/$targetCode")
            if (!result.isNullOrBlank()) {
                Log.d(TAG, "Translation succeeded via Lingva ML")
                return@withContext cleanHtml(result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lingva ML failed: ${e.message}")
        }

        // 2. Try Secondary: Lingva Mirror
        try {
            val result = translateViaLingva(trimmed, targetCode, "https://lingva.lunar.icu/api/v1/auto/$targetCode")
            if (!result.isNullOrBlank()) {
                Log.d(TAG, "Translation succeeded via Lingva Lunar")
                return@withContext cleanHtml(result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lingva Lunar failed: ${e.message}")
        }

        // 3. Try Tertiary: MyMemory Translated API
        try {
            val result = translateViaMyMemory(trimmed, targetCode)
            if (!result.isNullOrBlank()) {
                Log.d(TAG, "Translation succeeded via MyMemory")
                return@withContext cleanHtml(result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "MyMemory failed: ${e.message}")
        }

        // 4. Try Quaternary: Google GTX Client Endpoint
        try {
            val result = translateViaGoogleGtx(trimmed, targetCode)
            if (!result.isNullOrBlank()) {
                Log.d(TAG, "Translation succeeded via Google GTX")
                return@withContext cleanHtml(result)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google GTX failed: ${e.message}")
        }

        // 5. Try Quinary: me.bush.translator
        try {
            val bushLang = Language.values().find { it.name.equals(targetCode, ignoreCase = true) }
                ?: Language.values().find { it.name.equals(targetLanguageName, ignoreCase = true) }
                ?: Language.FRENCH
            val result = bushTranslator.translate(trimmed, bushLang, Language.AUTO)
            if (result.translatedText.isNotBlank()) {
                Log.d(TAG, "Translation succeeded via Bush Translator")
                return@withContext cleanHtml(result.translatedText)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bush Translator failed: ${e.message}")
        }

        // Return original text if all networks fail gracefully
        Log.e(TAG, "All translation providers exhausted, returning original content")
        trimmed
    }

    private fun translateViaLingva(text: String, targetCode: String, baseUrl: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "$baseUrl/$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            return json.optString("translation", null)
        }
    }

    private fun translateViaMyMemory(text: String, targetCode: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        // MyMemory accepts langpair auto|targetCode or en|targetCode
        val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=auto|$targetCode"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val responseData = json.optJSONObject("responseData") ?: return null
            val translated = responseData.optString("translatedText", "")
            return if (translated.isNotBlank()) translated else null
        }
    }

    private fun translateViaGoogleGtx(text: String, targetCode: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetCode&dt=t&q=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val jsonArray = JSONArray(body)
            val sentences = jsonArray.optJSONArray(0) ?: return null
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val sentence = sentences.optJSONArray(i) ?: continue
                sb.append(sentence.optString(0, ""))
            }
            return if (sb.isNotBlank()) sb.toString() else null
        }
    }

    private fun cleanHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }
}

// Helper extension for capitalization
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
