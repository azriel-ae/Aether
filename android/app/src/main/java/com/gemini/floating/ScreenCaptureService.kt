package com.gemini.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var saved = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, buildNotification("Menyiapkan screen capture…"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIFICATION_ID, buildNotification("Menyiapkan screen capture…"))
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java) else intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        if (data == null || resultCode == 0) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            worker = HandlerThread("screen-capture-worker").also { it.start() }
            handler = Handler(worker!!.looper)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { cleanup(); stopSelf() }
            }, handler)

            // The consent screen returns to Aether first. Wait briefly so moveTaskToBack()
            // can reveal the user's previous app before taking the one-frame capture.
            handler?.postDelayed({
                if (projection == null || reader == null) return@postDelayed
                reader?.setOnImageAvailableListener({ source ->
                    if (saved) return@setOnImageAvailableListener
                    val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try { saveImage(image, width, height) }
                    catch (_: Exception) { updateNotification("Capture gagal. Coba lagi.") }
                    finally { image.close() }
                }, handler)
                virtualDisplay = projection?.createVirtualDisplay(
                    "AetherUserSelectedScreenCapture", width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
                )
                updateNotification("Mengambil satu frame layar untuk dipilih.")
            }, 1200L)
        } catch (_: Exception) {
            updateNotification("Screen capture tidak dapat dimulai.")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun saveImage(image: Image, width: Int, height: Int) {
        if (saved) return
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        padded.recycle()
        val file = File(cacheDir, "capture-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
        cropped.recycle()
        saved = true
        val open = Intent(this, SelectionActivity::class.java).putExtra(SelectionActivity.EXTRA_IMAGE_PATH, file.absolutePath).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(this, 44, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("Capture siap ditinjau").setContentText("Ketuk untuk memilih area layar yang akan dianalisis").setContentIntent(pending).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        stopForeground(STOP_FOREGROUND_DETACH)
        cleanup()
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_DEFAULT))
    }
    private fun buildNotification(message: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_camera).setContentTitle("Gemini Floating Assistant").setContentText(message).setOngoing(true).build()
    private fun updateNotification(message: String) { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(message)) }
    private fun cleanup() {
        runCatching { virtualDisplay?.release() }; virtualDisplay = null
        runCatching { reader?.close() }; reader = null
        val activeProjection = projection; projection = null
        runCatching { activeProjection?.stop() }
        worker?.quitSafely(); worker = null; handler = null
    }
    override fun onDestroy() { cleanup(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 102
        fun createConsentIntent(context: Context): Intent = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
    }
}
