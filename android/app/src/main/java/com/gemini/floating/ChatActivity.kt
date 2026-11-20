package com.gemini.floating

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {
    private lateinit var db: ChatHistoryDb
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ChatHistoryDb(this)
        val accent = Color(0xFF9BAA83)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = Color(0xFFF3F2EA), surface = Color(0xFFFFFEF9), onSurface = Color(0xFF171A20))) {
                var messages by remember { mutableStateOf(db.latest()) }
                var input by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf("") }
                val listState = rememberLazyListState()
                Column(Modifier.fillMaxSize().background(Color(0xFFF3F2EA)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Percakapan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Gemini Assistant", color = Color(0xFF747B86), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { db.clear(); messages = emptyList(); error = "" }) { Text("Hapus chat") }
                    }
                    if (messages.isEmpty()) {
                        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mulai percakapan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Tulis pertanyaan atau minta bantuan Gemini.", color = Color(0xFF747B86), style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(messages) { _, item ->
                                val isUser = item.first == "user"
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                                    Surface(color = if (isUser) Color(0xFFE7EBDD) else Color(0xFFFFFEF9), shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 310.dp)) {
                                        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(if (isUser) "KAMU" else "GEMINI", style = MaterialTheme.typography.labelSmall, color = if (isUser) accent else Color(0xFF747B86), fontWeight = FontWeight.Bold)
                                            Text(item.second, color = Color(0xFF20242B), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (busy) Text("Gemini sedang menyiapkan jawaban…", color = Color(0xFF747B86), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
                    Surface(color = Color(0xFFFFFEF9), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tulis pesan…") }, enabled = !busy, shape = RoundedCornerShape(14.dp), minLines = 1, maxLines = 5, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent))
                            Button(onClick = {
                                val prompt = input.trim(); if (prompt.isEmpty() || busy) return@Button
                                input = ""; error = ""; db.add("user", prompt); messages = db.latest(); busy = true
                                lifecycleScope.launch {
                                    try { val answer = ChatApi.send(BuildConfig.API_BASE_URL, messages); db.add("assistant", answer); messages = db.latest() }
                                    catch (e: Exception) { error = e.message ?: "Permintaan gagal." }
                                    finally { busy = false }
                                }
                            }, enabled = !busy && input.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text(if (busy) "Mengirim…" else "Kirim pesan") }
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() { db.close(); super.onDestroy() }
}
