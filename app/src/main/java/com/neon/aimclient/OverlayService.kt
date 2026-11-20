package com.neon.aimclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: View? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(41, makeNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    private fun makeNotification(): Notification {
        val channelId = "neon_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "NEON floating panel",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("NEON AIM CLIENT")
            .setContentText("Floating visual panel is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun showPanel() {
        val size = getSharedPreferences("neon", MODE_PRIVATE).getInt("fov", 180)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            background = GradientDrawable().apply {
                setColor(Color.argb(225, 17, 20, 42))
                cornerRadius = 22f
                setStroke(2, Color.rgb(155, 92, 255))
            }
        }
        val header = TextView(this).apply {
            text = "NEON  •  DRAG"
            textSize = 13f
            setTextColor(Color.rgb(190, 150, 255))
            setPadding(4, 2, 4, 10)
        }
        val ring = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(3, Color.argb(190, 70, 190, 255))
            }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
        }
        val note = TextView(this).apply {
            text = "VISUAL FOV  •  TAP PANEL TO CLOSE"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 0)
        }
        container.addView(header)
        container.addView(ring)
        container.addView(note)

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 180
        }
        header.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x; startY = p.y
                        touchX = event.rawX; touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = startX + (event.rawX - touchX).toInt()
                        p.y = startY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(container, p)
                        return true
                    }
                }
                return false
            }
        })
        container.setOnClickListener { stopSelf() }
        windowManager.addView(container, p)
        panel = container
    }

    override fun onDestroy() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
