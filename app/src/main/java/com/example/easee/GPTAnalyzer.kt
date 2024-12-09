package com.example.easee

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class GPTAnalyzer {

    private val client = OkHttpClient()

    suspend fun analyzeImage(image: Bitmap, apiKey: String): String {
        val endpoint = "https://api.openai.com/v1/completions"
        val prompt = "Analyze the following image and provide detailed observations."

        val imageBase64 = encodeImageToBase64(image)

        val requestBody = """
            {
                "model": "gpt-4",
                "messages": [
                    {
                        "role": "user",
                        "content": "Here is an image encoded in Base64: $imageBase64. $prompt"
                    }
                ],
                "temperature": 0.7
            }
        """

        return makePostRequest(endpoint, requestBody, apiKey)
    }

    private fun encodeImageToBase64(image: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun makePostRequest(url: String, body: String, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: throw Exception("Empty response from API")
            } else {
                throw Exception("API Error: ${response.code}")
            }
        }
    }
}

