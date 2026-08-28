package com.wallpaperextend.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class AIApiConfig(
    val endpoint: String = "https://api.openai.com/v1/images/variations",
    val apiKey: String = "",
    val model: String = "dall-e-2"
)

data class AIModel(
    val id: String,
    val name: String,
    val provider: String
)

object AIApiClient {
    suspend fun requestImageExtension(
        context: Context,
        imageStream: InputStream,
        config: AIApiConfig,
        width: Int = 1080,
        height: Int = 1920
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = URL(config.endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")

            val requestBody = """
                {
                    "model": "${config.model}",
                    "image": "${imageStream.readBytes().toBase64()}",
                    "size": "${width}x${height}",
                    "response_format": "b64json"
                }
            """.trimIndent()

            connection.outputStream.write(requestBody.toByteArray(StandardCharsets.UTF_8))

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseData = connection.inputStream.readBytes()
                Result.success(responseData)
            } else {
                Result.failure(Exception("API Error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ByteArray.toBase64(): String = android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)
}
