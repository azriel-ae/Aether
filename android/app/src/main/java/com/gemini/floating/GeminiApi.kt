package com.gemini.floating

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApi {
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()

    suspend fun analyze(baseUrl: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        require(baseUrl.startsWith("https://")) { "API_BASE_URL harus menggunakan HTTPS dan domain Vercel yang benar." }
        val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true) else bitmap
        val bytes = ByteArrayOutputStream().use { out -> resized.compress(Bitmap.CompressFormat.JPEG, 82, out); out.toByteArray() }
        if (resized !== bitmap) resized.recycle()
        require(bytes.size <= 3_000_000) { "Gambar terlalu besar setelah kompresi. Pilih area yang lebih kecil." }
        val payload = JSONObject().put("prompt", "Analisis gambar yang dipilih pengguna. Jelaskan informasi yang terlihat secara akurat, susun langkah jika relevan, dan sebutkan jika ada bagian yang tidak terbaca.")
            .put("mimeType", "image/jpeg").put("imageBase64", Base64.encodeToString(bytes, Base64.NO_WRAP)).toString()
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/api/analyze").post(payload.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (!response.isSuccessful) throw IllegalStateException(json?.optString("error") ?: "HTTP ${response.code}")
            json?.optString("answer")?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Backend tidak mengembalikan jawaban.")
        }
    }
}
