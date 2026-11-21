package com.zvisuals.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
 
class MainActivity : Activity() {
    private val bg = Color.rgb(13, 18, 27)
    private val panel = Color.rgb(24, 32, 44)
    private val accent = Color.rgb(67, 139, 245)
    private val muted = Color.rgb(155, 170, 190)
    private val prefs by lazy { getSharedPreferences("zvisuals_demo", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var page = "chats"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        render()
    }

    private fun render() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(text("Z Visuals", 24f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(chip("PREVIEW", Color.rgb(42, 91, 151)))
        header.addView(top)
        header.addView(text(headerSubtitle(), 12f, muted).apply { setPadding(0, dp(4), 0, 0) })
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
            setPadding(dp(5), dp(7), dp(5), dp(8))
            background = rounded(panel, 18)
        }
        listOf(Triple("Чаты", "chats", "▤"), Triple("Контакты", "contacts", "♙"),
            Triple("Подарки", "gifts", "🎁"), Triple("Профиль", "profile", "◉"),
            Triple("Настройки", "settings", "⚙")).forEach { item ->
            val b = TextView(this).apply {
                text = "${item.third}\n${item.first}"
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (page == item.second) Color.WHITE else muted)
                setPadding(dp(2), dp(5), dp(2), dp(5))
                setOnClickListener { page = item.second; render() }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun headerSubtitle() = when(page) {
        "contacts" -> "Люди"
        "gifts" -> "Коллекция и подарки"
        "profile" -> "Твой профиль и оформление"
        "settings" -> "Настройки приложения"
        else -> "Сообщения"
    }

    private fun renderPage() {
        content.removeAllViews()
        when(page) {
            "contacts" -> showTelegramRoadmap("Контакты", "Поиск людей, список контактов и синхронизация адресной книги появятся после подключения Telegram API.")
            "gifts" -> showGifts()
            "profile" -> showProfile()
            "settings" -> showSettings()
            else -> showChats()
        }
    }

    private fun showChats() {
        sectionTitle("Все чаты")
        content.addView(card().apply {
            addView(text("Добро пожаловать в Z Visuals", 19f, Color.WHITE, true))
            addView(text("Здесь будет список твоих личных чатов, групп и каналов.", 13f, muted).apply { setPadding(0, dp(8), 0, dp(12)) })
            addView(action("О проекте", accent) { info("Telegram-интеграция", "Для настоящих чатов, звонков и синхронизации нужна интеграция с Telegram API/TDLib и отдельная проверенная сборка.") })
        })
        sectionTitle("План клиента")
        content.addView(note("Интерфейс чатов — первый экран. Реальные диалоги пока не подключены: приложение не авторизовано в Telegram и не отправляет сообщения."))
        content.addView(rowAction("Профиль и оформление") { page="profile"; render() })
        content.addView(rowAction("Настройки анимаций") { page="settings"; render() })
    }

    private fun showTelegramRoadmap(title: String, description: String) {
        sectionTitle(title)
        content.addView(card().apply {
            addView(text("Раздел Z Visuals", 18f, Color.WHITE, true))
            addView(text(description, 13f, muted).apply { setPadding(0, dp(8), 0, dp(12)) })
            addView(chip("ТРЕБУЕТ TELEGRAM API", Color.rgb(70, 77, 91)))
        })
    }

    private fun showProfile() {
        val username = prefs.getString("username", "zvisuals_user") ?: "zvisuals_user"
        val displayName = prefs.getString("display_name", "Мой профиль") ?: "Мой профиль"
        val bio = prefs.getString("bio", "Добро пожаловать в мой Z Visuals ✨") ?: ""
        val banner = prefs.getInt("banner", 0)
        val bannerColors = listOf(Color.rgb(42,75,125), Color.rgb(87,48,112), Color.rgb(30,105,103), Color.rgb(115,68,43))
        val preview = card().apply {
            background = rounded(bannerColors[banner.coerceIn(0,3)], 22)
            addView(text("Z VISUALS PROFILE", 10f, Color.WHITE, true))
            addView(text("◉  $displayName", 23f, Color.WHITE, true).apply { setPadding(0, dp(12), 0, dp(4)) })
            addView(text("@$username", 13f, Color.WHITE))
            addView(text(bio, 13f, Color.WHITE).apply { setPadding(0, dp(12), 0, dp(4)) })
            addView(text("Обложка · стиль · эффекты", 10f, Color.WHITE))
        }
        sectionTitle("Предпросмотр профиля")
        content.addView(preview)
        content.addView(action("Изменить имя и описание", accent) { editProfileText() })
        content.addView(action("Выбрать обложку", Color.rgb(57, 76, 103)) {
            val names = arrayOf("Ночная синяя", "Фиолетовая", "Изумрудная", "Тёплая")
            AlertDialog.Builder(this).setTitle("Цвет обложки").setSingleChoiceItems(names, banner) { d, which ->
                prefs.edit().putInt("banner", which).apply(); d.dismiss(); render()
            }.setNegativeButton("Отмена", null).show()
        })
        content.addView(action("Стиль имени и шрифт", Color.rgb(57, 76, 103)) { chooseFont() })
        sectionTitle("Эффекты профиля")
        val effects = arrayOf("Снег", "Дождь", "Сакура", "Конфетти", "Сердечки", "Искры", "Северное сияние")
        effects.forEachIndexed { i, name ->
            val key = "effect_$i"
            val cb = CheckBox(this).apply {
                text = name
                textSize = 14f
                setTextColor(Color.WHITE)
                buttonTintList = android.content.res.ColorStateList.valueOf(accent)
                isChecked = prefs.getBoolean(key, false)
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
            }
            content.addView(cb)
        }
        content.addView(note("Пока это локальный предпросмотр на этом устройстве. Чтобы другие пользователи Z Visuals увидели оформление, потребуется серверный профиль и синхронизация."))
    }

    private fun editProfileText() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), 0) }
        val name = EditText(this).apply { hint = "Отображаемое имя"; setText(prefs.getString("display_name", "Мой профиль")) }
        val username = EditText(this).apply { hint = "username"; setText(prefs.getString("username", "zvisuals_user")) }
        val bio = EditText(this).apply { hint = "О себе"; setText(prefs.getString("bio", "")); minLines = 2 }
        box.addView(name); box.addView(username); box.addView(bio)
        AlertDialog.Builder(this).setTitle("Редактирование профиля").setView(box)
            .setNegativeButton("Отмена", null).setPositiveButton("Сохранить") { _, _ ->
                val u = username.text.toString().trim().removePrefix("@")
                if (u.length >= 3) {
                    prefs.edit().putString("display_name", name.text.toString().trim().ifBlank { "Пользователь" })
                        .putString("username", u).putString("bio", bio.text.toString().trim()).apply()
                    render()
                } else Toast.makeText(this, "Имя пользователя — минимум 3 символа", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun chooseFont() {
        val names = arrayOf("Обычный", "Жирный", "Моноширинный")
        AlertDialog.Builder(this).setTitle("Шрифт имени")
            .setSingleChoiceItems(names, prefs.getInt("font_style", 0)) { d, which ->
                prefs.edit().putInt("font_style", which).apply()
                d.dismiss()
                Toast.makeText(this, "Стиль сохранён для предпросмотра", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Закрыть", null).show()
    }

    private fun showSettings() {
        sectionTitle("Анимации")
        val all = prefs.getBoolean("animations", true)
        content.addView(card().apply {
            addView(text("Плавность интерфейса", 17f, Color.WHITE, true))
            addView(text("Управляй движением экранов и визуальными эффектами.", 12f, muted).apply { setPadding(0, dp(5), 0, dp(8)) })
            addView(switchRow("Все анимации", "animations", true))
            addView(switchRow("Эффекты профиля", "profile_fx", all))
            addView(switchRow("Переходы между экранами", "transitions", all))
            addView(switchRow("Уменьшить интенсивность", "reduced_motion", all))
        })
        sectionTitle("Прочее")
        content.addView(rowAction("Оформление профиля") { page="profile"; render() })
        content.addView(rowAction("О приложении") { info("Z Visuals", "Независимый Android-проект. Подключение Telegram, синхронизация и серверные функции ещё находятся в разработке.") })
        content.addView(note("Переключатели сохраняются на устройстве. На этом этапе они задают предпочтения; полная система анимаций будет подключаться вместе с готовыми экранами."))
    }

    private fun switchRow(label: String, key: String, enabled: Boolean): Switch {
        return Switch(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            isEnabled = enabled
            isChecked = prefs.getBoolean(key, if (key == "animations") true else true)
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                if (key == "animations" && !checked) {
                    prefs.edit().putBoolean("profile_fx", false).putBoolean("transitions", false).apply()
                    render()
                }
            }
        }
    }

    private fun showGifts() {
        val balance = prefs.getInt("stars", 1200)
        val count = prefs.getInt("nft_count", 2)
        sectionTitle("Мои подарки")
        content.addView(card().apply {
            addView(text("★  $balance демо-звёзд", 22f, Color.WHITE, true))
            addView(text("В коллекции: $count предметов", 13f, muted).apply { setPadding(0, dp(5), 0, dp(12)) })
            addView(action("Получить подарок за 350 ★", accent) {
                if (prefs.getInt("stars", 1200) >= 350) {
                    prefs.edit().putInt("stars", prefs.getInt("stars",1200)-350)
                        .putInt("nft_count", prefs.getInt("nft_count",2)+1).apply()
                    render()
                } else Toast.makeText(this@MainActivity, "Не хватает демо-звёзд", Toast.LENGTH_SHORT).show()
            })
        })
        content.addView(note("Демо-подарки и звёзды существуют только внутри локального прототипа. Это не настоящие Telegram Stars или NFT."))
    }

    private fun rowAction(label: String, click: () -> Unit) = TextView(this).apply {
        text = "$label   ›"
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(dp(15), dp(17), dp(15), dp(17))
        background = rounded(panel, 14)
        setOnClickListener { click() }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(8); layoutParams = lp
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Понятно", null).show()
    }

    private fun sectionTitle(value: String) {
        content.addView(text(value, 17f, Color.WHITE, true).apply { setPadding(dp(2), dp(17), 0, dp(10)) })
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(panel, 18)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(10); layoutParams = lp
    }

    private fun note(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(muted)
        setPadding(dp(14), dp(13), dp(14), dp(13)); background = rounded(Color.rgb(20,27,37), 14)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(9); layoutParams = lp
    }

    private fun action(label: String, color: Int, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false; setTextColor(Color.WHITE)
        background = rounded(color, 12); setOnClickListener { click() }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(8); layoutParams = lp
    }

    private fun chip(label: String, color: Int) = TextView(this).apply {
        text = label; textSize = 9f; letterSpacing = 0.08f; setTextColor(Color.WHITE)
        setPadding(dp(10), dp(7), dp(10), dp(7)); background = rounded(color, 20)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
