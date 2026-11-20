package com.gemini.floating

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/** User-started visible overlay. No attempt is made to conceal it from other apps. */
class FloatingService : Service() {
    private var wm: WindowManager? = null
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Floating Assistant", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Gemini Floating Assistant").setContentText("Floating assistant aktif").setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTIFICATION_ID, notification)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { if (bubble == null) showBubble(); return START_STICKY }

    private fun showBubble() {
        val type = overlayType()
        bubbleParams = WindowManager.LayoutParams(dp(50), dp(50), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = dp(16); y = dp(220) }
        val view = TextView(this).apply { text = "G"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(-1); background = bg(accent(), 25); elevation = dp(7).toFloat(); contentDescription = "Open Gemini assistant" }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        view.setOnTouchListener { _, e -> when (e.action) {
            MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=bubbleParams.x; startY=bubbleParams.y; moved=false; true }
            MotionEvent.ACTION_MOVE -> { val dx=(e.rawX-downX).toInt(); val dy=(e.rawY-downY).toInt(); if (abs(dx)+abs(dy)>dp(7)) moved=true; bubbleParams.x=(startX-dx).coerceAtLeast(0); bubbleParams.y=(startY+dy).coerceAtLeast(0); runCatching { wm?.updateViewLayout(view,bubbleParams) }; true }
            MotionEvent.ACTION_UP -> { if (!moved) togglePanel(); true }
            else -> true
        } }
        bubble=view; wm?.addView(view,bubbleParams)
    }

    private fun togglePanel() {
        if (panel != null) { removePanel(); return }
        val light = prefs().getString("theme", "Dark") == "Light"
        val fg = if (light) 0xFF20251F.toInt() else 0xFFF3F5F8.toInt()
        val muted = if (light) 0xFF667085.toInt() else 0xFFB8BBAF.toInt()
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(17),dp(17),dp(17),dp(13)); background=bg(if(light) 0xFFF8FAFC.toInt() else 0xFF191D18.toInt(),22); elevation=dp(12).toFloat() }
        root.addView(TextView(this).apply { text="Gemini Assistant"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(fg) })
        root.addView(TextView(this).apply { text="Pilih tindakan"; textSize=12f; setTextColor(muted); setPadding(0,dp(3),0,dp(14)) })
        action(root,"Scan layar","Pindai area website atau aplikasi",true) { openScreenScan() }
        action(root,"Chat Gemini","Tanya dan lanjutkan percakapan",false) { open(ChatActivity::class.java) }
        action(root,"Pengaturan tampilan","Tema, warna, dan transparansi",false) { open(SettingsActivity::class.java) }
        root.addView(TextView(this).apply { text="Sembunyikan panel"; textSize=13f; gravity=Gravity.CENTER; setTextColor(muted); setPadding(0,dp(12),0,0); setOnClickListener { removePanel() } })
        val params=WindowManager.LayoutParams(dp(290),-2,overlayType(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.END; x=dp(12); y=dp(282) }
        panel=root; wm?.addView(root,params)
    }
    private fun action(root: LinearLayout,title: String,desc: String,primary: Boolean,onClick: () -> Unit) {
        val light=prefs().getString("theme","Dark")=="Light"
        val tile=if(primary) accent() else if(light) 0xFFE7EBDD.toInt() else 0xFF292F27.toInt()
        val text=if(primary) -1 else if(light) 0xFF20251F.toInt() else 0xFFF3F5F8.toInt()
        val sub=if(primary) 0xFFEAF0FF.toInt() else if(light) 0xFF667085.toInt() else 0xFFB8BBAF.toInt()
        val item=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(13),dp(11),dp(13),dp(11)); background=bg(tile,13); isClickable=true; isFocusable=true; setOnClickListener { onClick() } }
        item.addView(TextView(this).apply { this.text=title; textSize=14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(text) })
        item.addView(TextView(this).apply { this.text=desc; textSize=11f; setTextColor(sub); setPadding(0,dp(3),0,0) })
        root.addView(item,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
    }
    private fun open(cls: Class<*>) { startActivity(Intent(this,cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); removePanel() }
    private fun openScreenScan() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_START_SCAN, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        removePanel()
    }
    private fun removePanel() { panel?.let { runCatching { wm?.removeView(it) } }; panel=null }
    private fun prefs()=getSharedPreferences("appearance",MODE_PRIVATE)
    private fun accent()=when(prefs().getString("accent","Sage")){"Sage","Green"->0xFF9BAA83.toInt();"Orange"->0xFFB7794C.toInt();else->0xFF9BAA83.toInt()}
    private fun bg(color:Int,radius:Int)=GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun overlayType()=if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    override fun onDestroy() { removePanel(); bubble?.let { runCatching { wm?.removeView(it) } }; bubble=null; super.onDestroy() }
    override fun onBind(intent:Intent?):IBinder?=null
    companion object { private const val CHANNEL="floating_assistant"; private const val NOTIFICATION_ID=101 }
}
