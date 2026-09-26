package com.example.classifier

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

object LlmModelManager {
    private const val TAG = "LlmModelManager"
    private var appContext: Context? = null
    private var modelFilePath: String? = null
    private var isConfigured = false

    private val lazyLlmInference: LlmInference? by lazy {
        val path = modelFilePath
        val ctx = appContext
        if (path != null && ctx != null) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(512)
                    .setPreferredBackend(LlmInference.Backend.CPU)
                    .build()
                LlmInference.createFromOptions(ctx, options)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize LlmInference lazily", e)
                null
            }
        } else {
            null
        }
    }

    /**
     * On app startup, check if a file exists at context.getExternalFilesDir(null).path + "/model.task".
     * If it exists, initialize an LlmInference instance lazily using LlmInferenceOptions with that path,
     * preferredBackend CPU, maxTokens around 512.
     * If it doesn't exist, skip initialization entirely, no error, no crash.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val targetPath = extDir.path + "/model.task"
                val file = File(targetPath)
                if (file.exists()) {
                    modelFilePath = targetPath
                    isConfigured = true
                    Log.i(TAG, "Found on-device model at $targetPath, configured lazily")
                } else {
                    modelFilePath = null
                    isConfigured = false
                }
            } else {
                modelFilePath = null
                isConfigured = false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to check for model.task", e)
            modelFilePath = null
            isConfigured = false
        }
    }

    fun isModelInitialized(): Boolean {
        return isConfigured
    }

    fun getLlmInference(): LlmInference? {
        return if (isConfigured) lazyLlmInference else null
    }
}
