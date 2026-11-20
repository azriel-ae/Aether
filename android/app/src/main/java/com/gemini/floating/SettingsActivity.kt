package com.gemini.floating

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF9BAA83), background = Color(0xFFF3F2EA), surface = Color(0xFFFFFEF9), onSurface = Color(0xFF171A20))) {
                var accent by remember { mutableStateOf(prefs.getString("accent", "Sage") ?: "Sage") }
                var opacity by remember { mutableFloatStateOf(prefs.getInt("opacity", 92).toFloat()) }
                var theme by remember { mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark") }
                Column(Modifier.fillMaxSize().background(Color(0xFFF3F2EA)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("Atur tampilan assistant sesuai kenyamananmu.", color = Color(0xFF747B86), style = MaterialTheme.typography.bodyMedium)
                    }
                    SettingGroup("Tema", "Pilih tampilan panel") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = theme == "Dark", onClick = { theme = "Dark"; prefs.edit().putString("theme", theme).apply() }, label = { Text("Gelap") })
                            FilterChip(selected = theme == "Light", onClick = { theme = "Light"; prefs.edit().putString("theme", theme).apply() }, label = { Text("Terang") })
                        }
                    }
                    SettingGroup("Warna aksen", "Warna untuk tombol dan penanda") {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("Sage" to "Sage", "Orange" to "Terracotta").forEach { (key, label) ->
                                FilterChip(selected = accent == key, onClick = { accent = key; prefs.edit().putString("accent", key).apply() }, label = { Text(label) })
                            }
                        }
                    }
                    SettingGroup("Transparansi panel", "${opacity.toInt()}%") {
                        Slider(value = opacity, onValueChange = { opacity = it; prefs.edit().putInt("opacity", it.toInt()).apply() }, valueRange = 35f..100f)
                    }
                    Surface(color = Color(0xFFEDEFF3), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Tentang bubble", fontWeight = FontWeight.SemiBold)
                            Text("Bubble tetap tampil saat panel diminimalkan. Untuk menghentikannya, gunakan kontrol aplikasi atau notifikasi Android.", color = Color(0xFF747B86), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingGroup(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color(0xFFFFFEF9), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF747B86), style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}
