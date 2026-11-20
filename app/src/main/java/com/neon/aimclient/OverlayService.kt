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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var pill: View? = null
    private var panel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelVisible = false

    override fun onCreate() {
        super.onCreate()
        startForeground(41, makeNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPill()
    }

    private fun makeNotification(): Notification {
        val channelId = "neon_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId, "NEON floating controls", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("NEON AIM CLIENT")
            .setContentText("Floating watermark is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun overlayParams(x: Int, y: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

    private fun showPill() {
        val pillView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(19), dp(11), dp(19), dp(11))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(37, 25, 75), Color.rgb(14, 32, 61))
            ).apply {
                cornerRadius = dp(40).toFloat()
                setStroke(dp(2), Color.rgb(155, 92, 255))
            }
            elevation = dp(10).toFloat()
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(75, 205, 255))
            }
            layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                rightMargin = dp(9)
            }
        }
        val title = TextView(this).apply {
            text = "NEON  •  AIM CLIENT"
            textSize = 12f
            letterSpacing = 0.08f
            setTextColor(Color.WHITE)
        }
        pillView.addView(dot)
        pillView.addView(title)

        val params = overlayParams(dp(18), dp(42))
        attachDragAndClick(pillView, params) { togglePanel() }
        windowManager.addView(pillView, params)
        pill = pillView
        pillView.alpha = 0f
        pillView.scaleX = 0.85f
        pillView.scaleY = 0.85f
        pillView.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(260).setInterpolator(AccelerateDecelerateInterpolator()).start()
    }

    private fun togglePanel() {
        if (panelVisible) hidePanel() else showPanel()
    }

    private fun showPanel() {
        if (panelVisible) return
        val size = dp(getSharedPreferences("neon", MODE_PRIVATE).getInt("fov", 180))
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(13))
            background = GradientDrawable().apply {
                setColor(Color.argb(242, 13, 17, 38))
                cornerRadius = dp(23).toFloat()
                setStroke(dp(2), Color.rgb(120, 83, 245))
            }
            elevation = dp(12).toFloat()
        }

        val header = TextView(this).apply {
            text = "NEON CONTROL  •  DRAG HERE"
            textSize = 11f
            letterSpacing = 0.06f
            setTextColor(Color.rgb(194, 158, 255))
            setPadding(dp(3), dp(2), dp(3), dp(11))
        }
        val ring = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(3), Color.argb(220, 65, 190, 255))
            }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
        }
        val footer = TextView(this).apply {
            text = "VISUAL FOV  •  WATERMARK TO CLOSE"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(10), 0, 0)
        }
        container.addView(header)
        container.addView(ring)
        container.addView(footer)

        val params = overlayParams(dp(18), dp(105))
        attachDragOnly(header, container, params)
        windowManager.addView(container, params)
        panel = container
        panelParams = params
        panelVisible = true

        container.alpha = 0f
        container.scaleX = 0.88f
        container.scaleY = 0.88f
        container.translationY = -dp(12).toFloat()
        container.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .translationY(0f).setDuration(230)
            .setInterpolator(AccelerateDecelerateInterpolator()).start()
    }

    private fun hidePanel() {
        val current = panel ?: run {
            panelVisible = false
            return
        }
        current.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f)
            .translationY(-dp(10).toFloat()).setDuration(180)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (current.isAttachedToWindow) windowManager.removeView(current)
                if (panel === current) panel = null
                panelParams = null
                panelVisible = false
            }.start()
    }

    private fun attachDragAndClick(view: View, params: WindowManager.LayoutParams, click: () -> Unit) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) moved = true
                        if (moved) {
                            params.x = startX + dx.toInt()
                            params.y = startY + dy.toInt()
                            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) click()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return true
            }
        })
    }

    private fun attachDragOnly(handle: View, target: View, params: WindowManager.LayoutParams) {
        handle.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = params.x
                        startY = params.y
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) moved = true
                        if (moved) {
                            params.x = startX + dx.toInt()
                            params.y = startY + dy.toInt()
                            if (target.isAttachedToWindow) windowManager.updateViewLayout(target, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
                }
                return true
            }
        })
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)

    override fun onDestroy() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        pill?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        panel = null
        pill = null
        panelVisible = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
