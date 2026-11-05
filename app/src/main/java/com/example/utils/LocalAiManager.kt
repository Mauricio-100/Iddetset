package com.example.utils

import android.content.Context
import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.WhisperBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

enum class AiModelState {
    IDLE, DOWNLOADING, LOADING, READY, ERROR
}

object LocalAiManager {
    private const val TAG = "LocalAiManager"
    private const val LLM_MODEL_NAME = "phi-2.Q4_0.gguf"
    private const val WHISPER_MODEL_NAME = "ggml-tiny-q8_0.bin"

    private val _state = MutableStateFlow(AiModelState.IDLE)
    val state: StateFlow<AiModelState> = _state.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Initializes the models. 
     * In a real app, we would download them from a URL if not present.
     * Here we check if they are in assets and copy them to internal storage.
     */
    fun initOnce(context: Context) {
        if (_state.value != AiModelState.IDLE && _state.value != AiModelState.ERROR) return

        scope.launch {
            try {
                _state.value = AiModelState.LOADING
                
                // 1. Prepare LLM
                val llmPath = prepareModel(context, LLM_MODEL_NAME)
                if (llmPath != null) {
                    LlamaBridge.updateGenerateParams(
                        temperature = 0.3f,
                        topP = 0.9f,
                        topK = 40,
                        repeatPenalty = 1.1f,
                        maxTokens = 256,
                        contextLength = 2048,
                        useMmap = true,
                        numThreads = 4,
                        flashAttention = false,
                        batchSize = 512
                    )
                    val loaded = LlamaBridge.initGenerateModel(llmPath)
                    if (!loaded) {
                        Log.w(TAG, "Could not initialize LLM model bridge. Fallback heuristics active.")
                        _state.value = AiModelState.IDLE
                        return@launch
                    }
                } else {
                    Log.i(TAG, "Local LLM model asset ($LLM_MODEL_NAME) not bundled. Fallback heuristics active.")
                    _state.value = AiModelState.IDLE
                    return@launch
                }

                // 2. Prepare Whisper (optional, but requested)
                val whisperPath = prepareModel(context, WHISPER_MODEL_NAME)
                if (whisperPath != null) {
                    WhisperBridge.initModel(whisperPath)
                }

                _state.value = AiModelState.READY
                Log.d(TAG, "AI Models initialized successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Initialization info: ${e.message}")
                _state.value = AiModelState.IDLE
            }
        }
    }

    private suspend fun prepareModel(context: Context, modelName: String): String? = withContext(Dispatchers.IO) {
        val destFile = File(context.filesDir, modelName)
        if (destFile.exists()) return@withContext destFile.absolutePath

        // If not exists, try to copy from assets (simulating download or inclusion)
        try {
            context.assets.open(modelName).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            return@withContext destFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Model $modelName not in assets, it should be downloaded.")
            return@withContext null
        }
    }

    /**
     * Automatic category suggestion.
     */
    suspend fun suggestCategory(text: String): String? = withContext(Dispatchers.Default) {
        if (_state.value != AiModelState.READY) return@withContext null

        val jsonSchema = """
        {
          "type": "object",
          "properties": {
            "category": { "type": "string", "enum": ["Fun","Amour","Motivation","Tech","Sport","Musique","Actu","Business","Spiritualité","Autres"] }
          },
          "required": ["category"]
        }
        """.trimIndent()

        return@withContext try {
            val result = LlamaBridge.generateJsonWithContext(
                systemPrompt = "Tu es un expert en classification de contenu social. Réponds en JSON.",
                contextBlock = "",
                userPrompt = "Quelle est la meilleure catégorie pour ce texte ? \"$text\"",
                jsonSchema = jsonSchema
            )
            // Parse result to get category. Simplification: regex for now
            val match = "\"category\":\\s*\"([^\"]+)\"".toRegex().find(result)
            match?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.d(TAG, "Category suggestion fallback")
            null
        }
    }

    /**
     * Summarize content.
     */
    suspend fun summarize(text: String): String? = withContext(Dispatchers.Default) {
        if (_state.value != AiModelState.READY) return@withContext null
        try {
            LlamaBridge.generate(
                "Résume ce texte en une phrase courte et percutante : \"$text\""
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Local moderation pre-check.
     */
    suspend fun checkAppropriate(text: String): Boolean = withContext(Dispatchers.Default) {
        if (_state.value != AiModelState.READY) return@withContext true
        try {
            val result = LlamaBridge.generate(
                "Est-ce que ce texte contient du contenu inapproprié (haine, violence, etc.) ? Réponds uniquement par OUI ou NON. Texte : \"$text\""
            )
            !result.contains("OUI", ignoreCase = true)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * S3 AI Translation.
     */
    suspend fun translateWithS3(text: String, targetLanguage: String): String? = withContext(Dispatchers.Default) {
        if (_state.value != AiModelState.READY) return@withContext null
        try {
            LlamaBridge.generateWithContext(
                systemPrompt = "Tu es un traducteur expert. Traduis le texte de l'utilisateur vers le $targetLanguage. Ne réponds que par la traduction.",
                contextBlock = "",
                userPrompt = text
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Smart Reply suggestion.
     */
    suspend fun suggestReplies(lastMessage: String): List<String> = withContext(Dispatchers.Default) {
        if (_state.value != AiModelState.READY) return@withContext emptyList<String>()
        try {
            val jsonSchema = """
            {
              "type": "object",
              "properties": {
                "replies": { "type": "array", "items": { "type": "string" }, "maxItems": 3 }
              },
              "required": ["replies"]
            }
            """.trimIndent()

            val result = LlamaBridge.generateJsonWithContext(
                systemPrompt = "Tu es un assistant qui suggère des réponses courtes et naturelles à un message de chat.",
                contextBlock = "",
                userPrompt = "Suggère 3 réponses courtes à : \"$lastMessage\"",
                jsonSchema = jsonSchema
            )
            // Extract array. Simple regex for brevity
            "\"([^\"]+)\"".toRegex().findAll(result).map { it.groupValues[1] }.filter { it != "replies" }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
