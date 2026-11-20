package com.neon.aimclient

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private val purple = Color.rgb(174, 55, 255)
    private val cyan = Color.rgb(45, 205, 255)
    private val bg = Color.rgb(5, 7, 19)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "Z CLIENT"
            textSize = 25f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "NEON EDITION  •  ANDROID"
            textSize = 11f
            setTextColor(cyan)
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        })
        root.addView(label("Overlay permission"))
        root.addView(actionButton("GRANT DISPLAY OVER OTHER APPS") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else buildScreen()
        })
        root.addView(label("Visual FOV circle size"))
        val value = TextView(this).apply {
            text = "FOV: 180 px"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        root.addView(value)
        val seek = SeekBar(this).apply {
            max = 260
            progress = getSharedPreferences("neon", MODE_PRIVATE).getInt("fov", 180) - 40
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            thumbTintList = android.content.res.ColorStateList.valueOf(purple)
        }
        value.text = "FOV: ${seek.progress + 40} px"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                value.text = "FOV: ${p + 40} px"
                getSharedPreferences("neon", MODE_PRIVATE).edit().putInt("fov", p + 40).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(seek)
        root.addView(label("The circle is a visual guide only; it does not aim or interact with the game."))
        root.addView(actionButton("SHOW FLOATING PANEL") {
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, OverlayService::class.java))
            } else {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        })
        root.addView(actionButton("STOP FLOATING PANEL") {
            stopService(Intent(this, OverlayService::class.java))
        })
        root.addView(label("NEON PURPLE / BLUE  •  CONFIG SAVED ON DEVICE"))
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.LTGRAY)
        setPadding(2, 18, 2, 8)
    }

    private fun actionButton(caption: String, action: () -> Unit) = Button(this).apply {
        text = caption
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(75, 27, 145))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
    }
}
