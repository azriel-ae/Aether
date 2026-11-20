package com.gemini.floating

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val Ink = Color(0xFF20251F)
private val Muted = Color(0xFF777D72)
private val Accent = Color(0xFF9BAA83)
private val Canvas = Color(0xFFF3F2EA)
private val SurfaceTone = Color(0xFFFFFEF9)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
            // Return to the app the user was viewing before the consent dialog.
            window.decorView.post { moveTaskToBack(true) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent, onPrimary = Ink, background = Canvas, surface = SurfaceTone, onSurface = Ink)) {
                var status by remember { mutableStateOf("Assistant belum aktif") }
                Column(Modifier.fillMaxSize().background(Canvas).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("GEMINI ASSISTANT", style = MaterialTheme.typography.labelMedium, color = Accent, fontWeight = FontWeight.Bold)
                        Text("Bantu apa hari ini?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Pilih cara untuk mulai menggunakan assistant.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                    Surface(shape = RoundedCornerShape(22.dp), color = Ink, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(38.dp).background(Color(0xFF303846), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("G", color = Color.White, fontWeight = FontWeight.Bold) }
                                Column { Text("Floating assistant", color = Color.White, fontWeight = FontWeight.SemiBold); Text("Akses cepat dari layar mana pun", color = Color(0xFFB7BEC9), style = MaterialTheme.typography.bodySmall) }
                            }
                            Button(onClick = {
                                if (!Settings.canDrawOverlays(this@MainActivity)) {
                                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); status = "Izinkan tampil di atas aplikasi lain, lalu mulai lagi."
                                } else {
                                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, FloatingService::class.java)); status = "Floating assistant sedang aktif."
                                }
                            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Aktifkan bubble") }
                            TextButton(onClick = { stopService(Intent(this@MainActivity, FloatingService::class.java)); status = "Floating assistant dihentikan." }, modifier = Modifier.align(Alignment.End)) { Text("Hentikan assistant", color = Color(0xFFD0D5DE)) }
                        }
                    }
                    Text("FITUR", style = MaterialTheme.typography.labelMedium, color = Muted, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureRow("Scan layar", "Pilih area dari website atau aplikasi lain", "01", onClick = { startScreenScan() })
                        FeatureRow("Percakapan", "Tanya dan lanjutkan diskusi dengan Gemini", "02", onClick = { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) })
                        FeatureRow("Tampilan", "Atur tema, warna, dan transparansi", "03", onClick = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
                    }
                    Surface(color = Color(0xFFEDEFF3), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(status, color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Analisis layar hanya berjalan setelah izin Android diberikan dan kamu menekan tombol analisis.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Gemini Floating Assistant  ·  ${BuildConfig.VERSION_NAME}", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
        if (intent.getBooleanExtra(EXTRA_START_SCAN, false)) {
            intent.removeExtra(EXTRA_START_SCAN)
            window.decorView.post { startScreenScan() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_START_SCAN, false)) {
            intent.removeExtra(EXTRA_START_SCAN)
            startScreenScan()
        }
    }

    private fun startScreenScan() {
        captureLauncher.launch(ScreenCaptureService.createConsentIntent(this))
    }

    companion object { const val EXTRA_START_SCAN = "aether_start_screen_scan" }
}

@Composable
private fun FeatureRow(title: String, description: String, number: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = SurfaceTone, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(number, color = Accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = Muted, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
