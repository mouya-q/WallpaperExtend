package com.wallpaperextend.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wallpaperextend.ai.AIApiClient
import com.wallpaperextend.ai.AIApiConfig

class AIViewModel {
    var isProcessing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var resultImageUri by mutableStateOf<String?>(null)

    val defaultModels = listOf(
        com.wallpaperextend.ai.AIModel("dall-e-2", "DALL-E 2", "OpenAI"),
        com.wallpaperextend.ai.AIModel("dall-e-3", "DALL-E 3", "OpenAI"),
        com.wallpaperextend.ai.AIModel("stable-diffusion-xl", "Stable Diffusion XL", "Stability AI")
    )

    suspend fun processWithAI(
        context: Context,
        config: AIApiConfig,
        imageBytes: ByteArray
    ): Boolean {
        isProcessing = true
        errorMessage = null
        return try {
            val result = AIApiClient.requestImageExtension(
                context = context,
                imageStream = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).also { 
                    it.recycle()
                }.let {
                    java.io.ByteArrayInputStream(imageBytes)
                },
                config = config
            )
            result.onSuccess {
                resultImageUri = saveToCache(context, it)
            }.onFailure {
                errorMessage = it.message
            }
            result.isSuccess
        } catch (e: Exception) {
            errorMessage = e.message
            false
        } finally {
            isProcessing = false
        }
    }

    private fun saveToCache(context: Context, data: ByteArray): String {
        val file = context.cacheDir.resolve("ai_result.png")
        file.writeBytes(data)
        return file.absolutePath
    }
}
