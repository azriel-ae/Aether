package com.gemini.floating

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChatApi {
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
    suspend fun send(baseUrl: String, messages: List<Pair<String,String>>): String = withContext(Dispatchers.IO) {
        require(baseUrl.startsWith("https://")) { "API_BASE_URL harus menggunakan HTTPS." }
        val items = JSONArray()
        messages.takeLast(24).forEach { (role, text) -> items.put(JSONObject().put("role", if (role == "assistant") "model" else "user").put("text", text)) }
        val payload = JSONObject().put("messages", items).toString()
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/api/chat").post(payload.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty(); val json = runCatching { JSONObject(body) }.getOrNull()
            if (!response.isSuccessful) throw IllegalStateException(json?.optString("error") ?: "HTTP ${response.code}")
            json?.optString("answer")?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Backend tidak mengembalikan jawaban.")
        }
    }
}
