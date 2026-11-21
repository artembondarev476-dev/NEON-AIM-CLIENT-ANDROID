package com.zvisuals.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(15, 19, 26)
    private val panel = Color.rgb(25, 31, 41)
    private val accent = Color.rgb(48, 129, 237)
    private val muted = Color.rgb(151, 164, 181)
    private val prefs by lazy { getSharedPreferences("zvisuals_demo", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var page = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        render()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
        }
        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(text("Z Visuals", 23f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(chip("DEMO MODE", Color.rgb(38, 91, 153)))
        header.addView(brandRow)
        header.addView(text("Визуальная коллекция и подарки", 12f, muted).apply {
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(20))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        renderPage()

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(10))
            background = rounded(panel, 0, 0, 0, 0)
        }
        nav.addView(navButton("Главная", "home"), LinearLayout.LayoutParams(0, dp(52), 1f))
        nav.addView(navButton("Подарки", "gifts"), LinearLayout.LayoutParams(0, dp(52), 1f))
        nav.addView(navButton("Профиль", "profile"), LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(nav)
        setContentView(root)
    }

    private fun renderPage() {
        content.removeAllViews()
        when (page) {
            "gifts" -> showGifts()
            "profile" -> showProfile()
            else -> showHome()
        }
    }

    private fun showHome() {
        val balance = prefs.getInt("stars", 1200)
        val nftCount = prefs.getInt("nft_count", 2)
        val balanceCard = card()
        balanceCard.addView(text("МОИ ЗВЁЗДЫ", 11f, muted, true))
        balanceCard.addView(text("★  $balance", 34f, Color.WHITE, true).apply {
            setPadding(0, dp(7), 0, dp(3))
        })
        balanceCard.addView(text("Тестовый баланс — не связан с Telegram Stars", 11f, muted))
        balanceCard.addView(action("Начислить 100 ★", accent) {
            prefs.edit().putInt("stars", balance + 100).apply()
            render()
        })
        content.addView(balanceCard)

        sectionTitle("Коллекция")
        content.addView(text("В инвентаре: $nftCount подарков", 13f, muted).apply {
            setPadding(dp(2), 0, 0, dp(10))
        })
        val giftCard = card()
        giftCard.addView(text("🎁  Космический кот", 18f, Color.WHITE, true))
        giftCard.addView(text("Коллекционный подарок · 350 ★", 12f, muted).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        giftCard.addView(action("Получить за 350 ★", accent) { buyGift() })
        content.addView(giftCard)

        val second = card()
        second.addView(text("💎  Сапфировое сердце", 18f, Color.WHITE, true))
        second.addView(text("Коллекционный подарок · 500 ★", 12f, muted).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        second.addView(action("Получить за 500 ★", accent) { buyGift(500) })
        content.addView(second)

        sectionTitle("Важно")
        content.addView(note("Это самостоятельный демонстрационный проект. Баланс и коллекция пока хранятся только на этом устройстве. Они не меняют Telegram-профиль, не создают реальные NFT и не отправляют настоящие подарки."))
    }

    private fun showGifts() {
        sectionTitle("Мои подарки")
        val count = prefs.getInt("nft_count", 2)
        content.addView(card().apply {
            addView(text("🎁  Коллекция", 20f, Color.WHITE, true))
            addView(text("Предметов: $count", 13f, muted).apply { setPadding(0, dp(6), 0, dp(12)) })
            if (count > 0) {
                addView(action("Подарить пользователю (демо)", accent) { showSendDialog() })
            } else {
                addView(text("Пока нет подарков. Загляни в каталог.", 13f, muted))
            }
        })
        sectionTitle("История")
        val history = prefs.getString("history", "") ?: ""
        content.addView(note(if (history.isBlank()) "Здесь появятся демонстрационные действия." else history))
    }

    private fun showProfile() {
        val username = prefs.getString("username", "zvisuals_user") ?: "zvisuals_user"
        sectionTitle("Профиль")
        content.addView(card().apply {
            addView(text("👤  $username", 20f, Color.WHITE, true))
            addView(text("Публичный демо-профиль Z Visuals", 12f, muted).apply {
                setPadding(0, dp(5), 0, dp(12))
            })
            addView(action("Изменить имя пользователя", accent) { editUsername() })
        })
        sectionTitle("Аккаунт")
        content.addView(note("Вход по реальному номеру телефона и общие профили пока не включены. Для этого нужно подключить серверную авторизацию по SMS и базу данных. Номер телефона не запрашивается и не сохраняется в этой демо-версии."))
        content.addView(note("Z Visuals — независимый проект, не связанный с Telegram."))
    }

    private fun buyGift(cost: Int = 350) {
        val balance = prefs.getInt("stars", 1200)
        if (balance < cost) {
            Toast.makeText(this, "Не хватает демонстрационных звёзд", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit()
            .putInt("stars", balance - cost)
            .putInt("nft_count", prefs.getInt("nft_count", 2) + 1)
            .apply()
        Toast.makeText(this, "Демо-подарок добавлен в коллекцию", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun showSendDialog() {
        val field = EditText(this).apply {
            hint = "@username получателя"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Демо-отправка подарка")
            .setMessage("Подарок будет списан из локальной коллекции. Общая доставка появится после подключения сервера.")
            .setView(field)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Продолжить") { _, _ ->
                val target = field.text.toString().trim()
                if (target.isBlank()) {
                    Toast.makeText(this, "Укажи имя получателя", Toast.LENGTH_SHORT).show()
                } else {
                    val count = prefs.getInt("nft_count", 0)
                    if (count > 0) {
                        val old = prefs.getString("history", "") ?: ""
                        prefs.edit().putInt("nft_count", count - 1)
                            .putString("history", "Демо-подарок для $target\n$old").apply()
                        Toast.makeText(this, "Демо-подарок списан из коллекции", Toast.LENGTH_SHORT).show()
                        render()
                    }
                }
            }.show()
    }

    private fun editUsername() {
        val field = EditText(this).apply {
            setSingleLine(true)
            setText(prefs.getString("username", "zvisuals_user"))
            hint = "username"
        }
        AlertDialog.Builder(this).setTitle("Имя пользователя").setView(field)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить") { _, _ ->
                val value = field.text.toString().trim().removePrefix("@")
                if (value.length >= 3) {
                    prefs.edit().putString("username", value).apply()
                    render()
                } else Toast.makeText(this, "Минимум 3 символа", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun navButton(label: String, target: String): TextView = TextView(this).apply {
        text = label
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(if (page == target) Color.WHITE else muted)
        setOnClickListener { page = target; render() }
    }

    private fun sectionTitle(value: String) {
        content.addView(text(value, 17f, Color.WHITE, true).apply {
            setPadding(dp(2), dp(18), 0, dp(10))
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(panel, 18)
        elevation = dp(2).toFloat()
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(10)
        layoutParams = lp
    }

    private fun note(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(muted)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = rounded(Color.rgb(22, 29, 39), 14)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(10)
        layoutParams = lp
    }

    private fun action(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(color, 12)
        setPadding(dp(12), dp(3), dp(12), dp(3))
        setOnClickListener { onClick() }
    }

    private fun chip(label: String, color: Int) = TextView(this).apply {
        text = label
        textSize = 9f
        letterSpacing = 0.06f
        setTextColor(Color.WHITE)
        setPadding(dp(9), dp(6), dp(9), dp(6))
        background = rounded(color, 20)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = 0, left: Int = 0, top: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
