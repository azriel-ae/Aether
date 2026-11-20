package com.gemini.floating

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class SelectionActivity : ComponentActivity() {
    private lateinit var selection: CropSelectionView
    private lateinit var status: TextView
    private lateinit var analyze: Button
    private lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (path.isNullOrBlank() || !File(path).exists()) { finish(); return }
        bitmap = BitmapFactory.decodeFile(path) ?: run { finish(); return }
        selection = CropSelectionView(this, bitmap)
        status = TextView(this).apply {
            text = "Pilih area layar yang ingin dianalisis.\nGeser kotak untuk memindahkan, tarik sudut untuk mengubah ukuran."
            setTextColor(Color.rgb(32, 37, 31)); textSize = 14f; setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.WHITE)
        }
        analyze = Button(this).apply {
            text = "Analisis area"; isAllCaps = false; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); background = rounded(Color.rgb(155, 170, 131), 14)
        }
        val cancel = Button(this).apply {
            text = "Batal"; isAllCaps = false; textSize = 14f; setTextColor(Color.rgb(48, 54, 64))
            background = rounded(Color.rgb(238, 240, 244), 14); setOnClickListener { finish() }
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(16, 12, 16, 16); setBackgroundColor(Color.WHITE)
            addView(cancel, LinearLayout.LayoutParams(0, 52, 1f).apply { rightMargin = 8 })
            addView(analyze, LinearLayout.LayoutParams(0, 52, 1.7f))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(243, 242, 234))
            addView(status); addView(selection, LinearLayout.LayoutParams(-1, 0, 1f)); addView(actions)
        }
        setContentView(root)
        analyze.setOnClickListener { submitCrop() }
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * resources.displayMetrics.density
    }

    private fun submitCrop() {
        val crop = selection.crop() ?: run { status.text = "Area pilihan tidak valid."; return }
        analyze.isEnabled = false
        status.text = "Mengirim gambar untuk dianalisis…"
        lifecycleScope.launch {
            try {
                val answer = GeminiApi.analyze(BuildConfig.API_BASE_URL, crop)
                ChatHistoryDb(this@SelectionActivity).use { db -> db.add("user", "Tolong analisis area gambar yang saya pilih."); db.add("assistant", answer) }
                status.text = answer
            } catch (error: Exception) {
                status.text = "Gagal menganalisis: ${error.message ?: "Periksa koneksi dan konfigurasi backend."}"
            } finally { crop.recycle(); analyze.isEnabled = true }
        }
    }

    override fun onDestroy() { if (::bitmap.isInitialized && !bitmap.isRecycled) bitmap.recycle(); super.onDestroy() }
    companion object { const val EXTRA_IMAGE_PATH = "selected_capture_path" }
}

private class CropSelectionView(context: android.content.Context, private val source: Bitmap) : View(context) {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x77000000 }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9BAA83.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 30f; style = Paint.Style.FILL }
    private val rect = RectF()
    private var imageRect = RectF()
    private var mode = 0 // 1 move, 2 resize bottom-right
    private var lastX = 0f
    private var lastY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width.toFloat() / source.width, height.toFloat() / source.height)
        val w = source.width * scale; val h = source.height * scale
        imageRect = RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2)
        canvas.drawColor(0xFF20251F.toInt())
        canvas.drawBitmap(source, null, imageRect, imagePaint)
        if (rect.isEmpty) rect.set(imageRect.left + imageRect.width()*.12f, imageRect.top + imageRect.height()*.12f, imageRect.right - imageRect.width()*.12f, imageRect.bottom - imageRect.height()*.12f)
        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, rect.top, shadePaint)
        canvas.drawRect(imageRect.left, rect.bottom, imageRect.right, imageRect.bottom, shadePaint)
        canvas.drawRect(imageRect.left, rect.top, rect.left, rect.bottom, shadePaint)
        canvas.drawRect(rect.right, rect.top, imageRect.right, rect.bottom, shadePaint)
        canvas.drawRect(rect, borderPaint)
        val radius = 13f
        canvas.drawCircle(rect.left, rect.top, radius, handlePaint); canvas.drawCircle(rect.right, rect.top, radius, handlePaint)
        canvas.drawCircle(rect.left, rect.bottom, radius, handlePaint); canvas.drawCircle(rect.right, rect.bottom, radius, handlePaint)
        val cropWidth = ((rect.width() / imageRect.width()) * source.width).toInt()
        val cropHeight = ((rect.height() / imageRect.height()) * source.height).toInt()
        canvas.drawText("${cropWidth} × ${cropHeight} px", rect.left, (rect.top - 12f).coerceAtLeast(34f), labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                mode = when {
                    near(event.x, event.y, rect.left, rect.top) -> 2
                    near(event.x, event.y, rect.right, rect.top) -> 3
                    near(event.x, event.y, rect.left, rect.bottom) -> 4
                    near(event.x, event.y, rect.right, rect.bottom) -> 5
                    rect.contains(event.x, event.y) -> 1
                    else -> 0
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x-lastX; val dy = event.y-lastY
                when (mode) {
                    1 -> { rect.offset(dx,dy); rect.offset(clampOffsetX(),clampOffsetY()) }
                    2 -> { rect.left = (rect.left+dx).coerceIn(imageRect.left, rect.right-80); rect.top = (rect.top+dy).coerceIn(imageRect.top, rect.bottom-80) }
                    3 -> { rect.right = (rect.right+dx).coerceIn(rect.left+80, imageRect.right); rect.top = (rect.top+dy).coerceIn(imageRect.top, rect.bottom-80) }
                    4 -> { rect.left = (rect.left+dx).coerceIn(imageRect.left, rect.right-80); rect.bottom = (rect.bottom+dy).coerceIn(rect.top+80, imageRect.bottom) }
                    5 -> { rect.right = (rect.right+dx).coerceIn(rect.left+80, imageRect.right); rect.bottom = (rect.bottom+dy).coerceIn(rect.top+80, imageRect.bottom) }
                }
                lastX=event.x; lastY=event.y; invalidate(); return true
            }
            MotionEvent.ACTION_UP -> { mode=0; return true }
        }
        return true
    }
    private fun near(x: Float, y: Float, hx: Float, hy: Float): Boolean = kotlin.math.hypot((x-hx).toDouble(), (y-hy).toDouble()) < 52.0
    private fun clampOffsetX(): Float = when { rect.left < imageRect.left -> imageRect.left-rect.left; rect.right > imageRect.right -> imageRect.right-rect.right; else -> 0f }
    private fun clampOffsetY(): Float = when { rect.top < imageRect.top -> imageRect.top-rect.top; rect.bottom > imageRect.bottom -> imageRect.bottom-rect.bottom; else -> 0f }
    fun crop(): Bitmap? {
        if (imageRect.width() <= 0 || imageRect.height() <= 0) return null
        val sx = source.width / imageRect.width(); val sy = source.height / imageRect.height()
        val left = ((rect.left-imageRect.left)*sx).toInt().coerceIn(0, source.width-1)
        val top = ((rect.top-imageRect.top)*sy).toInt().coerceIn(0, source.height-1)
        val right = ((rect.right-imageRect.left)*sx).toInt().coerceIn(left+1, source.width)
        val bottom = ((rect.bottom-imageRect.top)*sy).toInt().coerceIn(top+1, source.height)
        return Bitmap.createBitmap(source, left, top, right-left, bottom-top)
    }
}
