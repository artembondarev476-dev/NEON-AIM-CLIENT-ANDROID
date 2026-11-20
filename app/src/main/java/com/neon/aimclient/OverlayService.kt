package com.neon.aimclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
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
    private var hudView: FovHudView? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(41, makeNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showHud()
        showPill()
    }

    private fun makeNotification(): Notification {
        val channelId = "neon_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId, "Z Client floating controls", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("Z Client")
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
                intArrayOf(Color.rgb(44, 12, 90), Color.rgb(7, 28, 58))
            ).apply {
                cornerRadius = dp(40).toFloat()
                setStroke(dp(2), Color.rgb(183, 54, 255))
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
            text = "Z CLIENT"
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


    // A real, touch-transparent visual HUD. It never reads or changes game state.
    private fun showHud() {
        val view = FovHudView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(view, params)
        hudView = view
    }

    private inner class FovHudView(context: android.content.Context) : View(context) {
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            color = Color.rgb(66, 218, 255)
        }
        private val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = Color.rgb(190, 70, 255)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val prefs = getSharedPreferences("neon", MODE_PRIVATE)
            val cx = width / 2f
            val cy = height / 2f
            val maxRadius = minOf(width, height) * 0.47f

            if (prefs.getBoolean("aim_bot_enabled", false)) {
                val fov = prefs.getInt("aim_bot_fov", 90).coerceIn(0, 360)
                val radius = maxRadius * (fov / 360f)
                if (radius > 0f) canvas.drawCircle(cx, cy, radius, ringPaint)
            }

            if (prefs.getBoolean("client_aim_enabled", false)) {
                val arm = dp(9).toFloat()
                val gap = dp(4).toFloat()
                canvas.drawLine(cx - arm - gap, cy, cx - gap, cy, reticlePaint)
                canvas.drawLine(cx + gap, cy, cx + arm + gap, cy, reticlePaint)
                canvas.drawLine(cx, cy - arm - gap, cx, cy - gap, reticlePaint)
                canvas.drawLine(cx, cy + gap, cx, cy + arm + gap, reticlePaint)
            }
        }
    }

    private fun togglePanel() {
        if (panelVisible) hidePanel() else showPanel()
    }

    private fun showPanel() {
        if (panelVisible) return
        val prefs = getSharedPreferences("neon", MODE_PRIVATE)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            minimumWidth = dp(310)
            background = GradientDrawable().apply {
                setColor(Color.argb(246, 10, 15, 34))
                cornerRadius = dp(23).toFloat()
                setStroke(dp(2), Color.rgb(164, 55, 255))
            }
            elevation = dp(12).toFloat()
        }

        val header = TextView(this).apply {
            text = "Z CLIENT  •  DRAG HERE"
            textSize = 11f
            letterSpacing = 0.06f
            setTextColor(Color.rgb(205, 150, 255))
            setPadding(dp(3), dp(2), dp(3), dp(12))
        }
        container.addView(header)
        container.addView(makeAimSection(
            "AIM BOT",
            "Визуальный FOV-круг; не наводит оружие",
            "aim_bot_enabled",
            "aim_bot_fov",
            prefs
        ))
        val divider = View(this).apply {
            setBackgroundColor(Color.argb(100, 150, 130, 220))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        }
        container.addView(divider)
        container.addView(makeAimSection(
            "CLIENT AIM",
            "Визуальный маркер прицела; не меняет полёт пуль",
            "client_aim_enabled",
            "client_aim_fov",
            prefs
        ))

        val footer = TextView(this).apply {
            text = "VISUAL SETTINGS ONLY  •  NO GAME CONTROL"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(14), 0, dp(1))
        }
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

    private fun makeAimSection(
        titleText: String,
        descriptionText: String,
        enabledKey: String,
        fovKey: String,
        prefs: android.content.SharedPreferences
    ): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = titleText
            textSize = 15f
            letterSpacing = 0.08f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        titleRow.addView(title, LinearLayout.LayoutParams(0, dp(32), 1f))

        val enabled = prefs.getBoolean(enabledKey, false)
        val toggle = TextView(this).apply {
            text = if (enabled) "ON" else "OFF"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(7), dp(18), dp(7))
            background = toggleBackground(enabled)
            isClickable = true
            isFocusable = true
        }
        titleRow.addView(toggle)
        toggle.setOnClickListener {
            val next = !prefs.getBoolean(enabledKey, false)
            prefs.edit().putBoolean(enabledKey, next).apply()
            toggle.text = if (next) "ON" else "OFF"
            toggle.background = toggleBackground(next)
            hudView?.invalidate()
        }
        section.addView(titleRow)

        val description = TextView(this).apply {
            text = descriptionText
            textSize = 10f
            setTextColor(Color.rgb(165, 174, 198))
            setPadding(0, dp(3), 0, dp(8))
        }
        section.addView(description)

        val currentFov = prefs.getInt(fovKey, 90).coerceIn(0, 360)
        val fovLabel = TextView(this).apply {
            text = "FOV: ${currentFov}°"
            textSize = 12f
            setTextColor(Color.rgb(65, 205, 255))
        }
        section.addView(fovLabel)

        val slider = android.widget.SeekBar(this).apply {
            max = 360
            progress = currentFov
            contentDescription = "$titleText FOV"
        }
        slider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                fovLabel.text = "FOV: ${progress}°"
                if (fromUser) prefs.edit().putInt(fovKey, progress).apply()
                hudView?.invalidate()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        section.addView(slider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(38)
        ))
        return section
    }

    private fun toggleBackground(enabled: Boolean): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(30).toFloat()
            setColor(if (enabled) Color.rgb(19, 115, 105) else Color.rgb(54, 59, 79))
            setStroke(dp(1), if (enabled) Color.rgb(70, 235, 205) else Color.rgb(115, 125, 150))
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
        hudView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        pill?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        panel = null
        pill = null
        hudView = null
        panelVisible = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
