package org.dpdns.bnai.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import org.dpdns.bnai.assistant.MainActivity

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: TextView
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var chatHistory: TextView
    private var isChatOpen = false

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingBubble()
    }

    private fun startForegroundNotification() {
        val channelId = "bnai_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "BNAI Floating Guide", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("AI Guide Active").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        } else {
            Notification.Builder(this).setContentTitle("AI Guide Active").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        }
        startForeground(1001, notification)
    }

    private fun createFloatingBubble() {
        val density = resources.displayMetrics.density
        val bubbleSizePx = (56 * density).toInt()

        // Root Container
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }

        // 1. Messenger Style Bubble
        bubbleView = TextView(this).apply {
            text = "AI"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#1E40AF")); setStroke(4, Color.parseColor("#60A5FA")) }
            layoutParams = LinearLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
        }

        // 2. Expandable Chat Panel
        chatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply { cornerRadius = 32f; setColor(Color.parseColor("#0F172A")) }
            layoutParams = LinearLayout.LayoutParams((300 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        chatHistory = TextView(this).apply {
            text = "Hi! How can I guide you?"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }

        inputField = EditText(this).apply {
            hint = "Ask AI..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = 16f; setColor(Color.parseColor("#1E293B")) }
            setPadding(16, 16, 16, 16)
            maxLines = 1
        }

        val sendBtn = Button(this).apply {
            text = "Send"
            setBackgroundColor(Color.parseColor("#3B82F6"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val q = inputField.text.toString()
                if (q.isNotBlank()) {
                    chatHistory.text = "$q\n\nThinking..."
                    inputField.setText("")
                    // Note: Yahan backend call hoga (future update)
                    // Abhi ke liye placeholder
                    chatHistory.postDelayed({ chatHistory.text = "$q\n\nAI Guide: This feature will connect to backend soon." }, 1000)
                }
            }
        }

        chatContainer.addView(chatHistory)
        chatContainer.addView(inputField)
        chatContainer.addView(sendBtn)

        root.addView(bubbleView)
        root.addView(chatContainer)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 50; y = 300 }

        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f; var downTime = 0L

        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt(); val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) { params.x = initialX + dx; params.y = initialY + dy; windowManager.updateViewLayout(root, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - downTime < 200) {
                        // Click toggle chat panel
                        isChatOpen = !isChatOpen
                        chatContainer.visibility = if (isChatOpen) View.VISIBLE else View.GONE
                        // Make it focusable when open so keyboard works
                        params.flags = if (isChatOpen) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        windowManager.updateViewLayout(root, params)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(root, params)
    }

    override fun onDestroy() {
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
