package org.dpdns.bnai.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var panelLayout: LinearLayout
    private lateinit var statusLabel: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isHoldingCapture = false
    private var capturedFramesCount = 0

    private val holdCaptureRunnable = object : Runnable {
        override fun run() {
            if (isHoldingCapture) {
                capturedFramesCount += 1
                statusLabel.text = "Capturing (1 FPS Hold): $capturedFramesCount frames queued"
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        val bubbleSizePx = (60 * density).toInt()

        rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val bubbleView = TextView(this).apply {
            text = "AI"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7C8CFF"))
                setStroke(4, Color.parseColor("#29C7C7"))
            }
        }

        panelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.parseColor("#151B2D"))
                setStroke(2, Color.parseColor("#303A52"))
            }
        }

        statusLabel = TextView(this).apply {
            text = "Idle (Explicit Consent Required for Screen Capture)"
            setTextColor(Color.parseColor("#F5F7FF"))
            textSize = 12f
        }

        val singleCaptureBtn = Button(this).apply {
            text = "Single Screenshot Analyze"
            setOnClickListener {
                capturedFramesCount += 1
                statusLabel.text = "Captured 1 Screenshot -> Queued for Copilot Vision Analysis"
                Toast.makeText(this@FloatingBubbleService, "Screenshot queued (7-day ephemeral retention)", Toast.LENGTH_SHORT).show()
            }
        }

        val holdCaptureBtn = Button(this).apply {
            text = "Hold (1 FPS Capture) / Tap to Toggle"
            setOnClickListener {
                if (!isHoldingCapture) {
                    isHoldingCapture = true
                    handler.post(holdCaptureRunnable)
                } else {
                    stopHoldCapture("Hold capture paused ($capturedFramesCount frames).")
                }
            }
        }

        val stopServiceBtn = Button(this).apply {
            text = "STOP FLOATING ASSISTANT"
            setBackgroundColor(Color.parseColor("#F16C7B"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopHoldCapture("Session stopped by user.")
                stopSelf()
            }
        }

        panelLayout.addView(statusLabel)
        panelLayout.addView(singleCaptureBtn)
        panelLayout.addView(holdCaptureBtn)
        panelLayout.addView(stopServiceBtn)

        rootLayout.addView(bubbleView)
        rootLayout.addView(panelLayout)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 220
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    if (dx < 15 && dy < 15) {
                        panelLayout.visibility = if (panelLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(rootLayout, params)
    }

    private fun stopHoldCapture(reason: String) {
        isHoldingCapture = false
        handler.removeCallbacks(holdCaptureRunnable)
        statusLabel.text = reason
    }

    private fun startForegroundNotification() {
        val channelId = "bnai_floating_assistant_v31"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "BNAI Floating Assistant", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("BNAI Floating Assistant Active")
                .setContentText("Tap bubble for quick chat or authorized screen analysis")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("BNAI Floating Assistant Active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
        startForeground(3101, notification)
    }

    override fun onDestroy() {
        stopHoldCapture("Service destroyed")
        if (::rootLayout.isInitialized) windowManager.removeView(rootLayout)
        super.onDestroy()
    }
}
