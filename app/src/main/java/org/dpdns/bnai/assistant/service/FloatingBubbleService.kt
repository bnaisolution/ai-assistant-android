package org.dpdns.bnai.assistant.service
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var statusLabel: TextView
    private lateinit var panelLayout: LinearLayout
    private var isCapturingHold = false
    private var captureCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val holdCaptureRunnable = object : Runnable {
        override fun run() { if (isCapturingHold) { captureCount++; statusLabel.text = "ACTIVE CAPTURE: Frame #${'$'}captureCount"; if (captureCount >= 30) stopHoldCapture("Limit reached.") else handler.postDelayed(this, 1000L) } }
    }
    override fun onCreate() { super.onCreate(); startForegroundNotification(); windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager; createFloatingBubbleUi() }
    private fun startForegroundNotification() {
        val channelId = "bnai_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { val channel = NotificationChannel(channelId, "BNAI Floating Assistant", NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(channel) }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { Notification.Builder(this, channelId).setContentTitle("Floating Assistant Active").setSmallIcon(android.R.drawable.ic_dialog_info).build() } else { Notification.Builder(this).setContentTitle("Floating Assistant Active").setSmallIcon(android.R.drawable.ic_dialog_info).build() }
        startForeground(1001, notification)
    }
    private fun createFloatingBubbleUi() {
        val density = resources.displayMetrics.density; val bubbleSizePx = (60 * density).toInt(); rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bubbleView = TextView(this).apply { text = "AI"; setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#1E40AF")); setStroke(4, Color.parseColor("#60A5FA")) }; layoutParams = LinearLayout.LayoutParams(bubbleSizePx, bubbleSizePx) }
        panelLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(24, 24, 24, 24); background = GradientDrawable().apply { cornerRadius = 24f; setColor(Color.parseColor("#0F172A")) } }
        statusLabel = TextView(this).apply { text = "Idle"; setTextColor(Color.parseColor("#93C5FD")); textSize = 12f }
        val stopServiceBtn = Button(this).apply { text = "STOP"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE); setOnClickListener { stopHoldCapture("Stopped"); stopSelf() } }
        panelLayout.addView(statusLabel); panelLayout.addView(stopServiceBtn); rootLayout.addView(bubbleView); rootLayout.addView(panelLayout)
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, overlayType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 40; y = 260 }
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f; var downTime = 0L
        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; downTime = System.currentTimeMillis(); handler.postDelayed({ if (System.currentTimeMillis() - downTime >= 600 && !isCapturingHold) { isCapturingHold = true; captureCount = 0; panelLayout.visibility = View.VISIBLE; handler.post(holdCaptureRunnable) } }, 650L); true }
                MotionEvent.ACTION_MOVE -> { val dx = (event.rawX - initialTouchX).toInt(); val dy = (event.rawY - initialTouchY).toInt(); if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) { params.x = initialX + dx; params.y = initialY + dy; windowManager.updateViewLayout(rootLayout, params) }; true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { handler.removeCallbacksAndMessages(null); if (isCapturingHold) stopHoldCapture("Hold released.") else if (System.currentTimeMillis() - downTime < 350) { panelLayout.visibility = if (panelLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE }; true }
                else -> false
            }
        }
        windowManager.addView(rootLayout, params)
    }
    private fun stopHoldCapture(reason: String) { isCapturingHold = false; handler.removeCallbacks(holdCaptureRunnable); statusLabel.text = reason }
    override fun onDestroy() { stopHoldCapture("Service destroyed"); if (::rootLayout.isInitialized) windowManager.removeView(rootLayout); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
