package com.keyhistory.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.keyhistory.R

enum class ShiftState { OFF, ONCE, CAPS }

/** Estado exibido no indicador de registro do teclado. */
sealed interface RecordingIndicator {
    data object Active : RecordingIndicator
    data object Paused : RecordingIndicator
    data object Disabled : RecordingIndicator
    data class Protected(val reason: String) : RecordingIndicator
}

interface KeyboardListener {
    fun onText(text: String)
    fun onBackspace()
    fun onEnter()
    fun onTogglePause()
    fun onSaveNow()
    fun onOpenApp()
    fun onSwitchKeyboard()
}

/** Teclado desenhado com Views nativas (mais leve e robusto que Compose dentro de um IME). */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val listener: KeyboardListener) : LinearLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).toInt()

    private val cBackground = Color.parseColor("#070B14")
    private val cKey = Color.parseColor("#1E2735")
    private val cSpecial = Color.parseColor("#2A3445")
    private val cPressed = Color.parseColor("#3A4659")
    private val cPrimary = Color.parseColor("#3B82F6")
    private val cText = Color.parseColor("#E6EAF2")
    private val cMuted = Color.parseColor("#8A94A6")
    private val cGreen = Color.parseColor("#22C55E")
    private val cRed = Color.parseColor("#EF4444")
    private val cAmber = Color.parseColor("#F59E0B")

    var layer: Layer = Layer.LETTERS
        private set
    var shift: ShiftState = ShiftState.OFF
        private set
    private var lastShiftTap = 0L
    var pendingDead: String? = null
        private set
    var hapticEnabled = true
    var enterLabel: String = "enter"
        set(value) { field = value; refreshLabels() }

    private val toolbar = FrameLayout(context)
    private val statusRow = LinearLayout(context)
    private val statusDot = View(context)
    private val statusText = TextView(context)
    private val pauseButton = ImageView(context)
    private val saveButton = ImageView(context)
    private val variantsRow = LinearLayout(context)
    private val keysContainer = LinearLayout(context)
    private val keyViews = mutableListOf<Pair<TextView, Key>>()
    private val iconViews = mutableListOf<Pair<ImageView, Key>>()
    private var keyHeight = dp(52f)
    private var indicator: RecordingIndicator = RecordingIndicator.Disabled
    private var pulse = true
    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (indicator == RecordingIndicator.Active) {
                pulse = !pulse
                statusDot.animate().alpha(if (pulse) 1f else 0.35f).setDuration(700).start()
            } else statusDot.alpha = 1f
            handler.postDelayed(this, 750)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(cBackground)
        computeKeyHeight()
        buildToolbar()
        addView(toolbar, LayoutParams(LayoutParams.MATCH_PARENT, dp(42f)))
        keysContainer.orientation = VERTICAL
        keysContainer.setPadding(dp(3f), 0, dp(3f), dp(4f))
        addView(keysContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        buildKeys()
        setOnApplyWindowInsetsListener { v, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }
            v.setPadding(0, 0, 0, bottom)
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(pulseRunnable)
        handler.post(pulseRunnable)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun computeKeyHeight() {
        val dm = resources.displayMetrics
        val landscape = dm.widthPixels > dm.heightPixels
        val target = dm.heightPixels * if (landscape) 0.085f else 0.064f
        keyHeight = target.toInt().coerceIn(dp(36f), dp(60f))
    }

    // ------------------------------------------------------------- toolbar

    private fun roundRect(color: Int, radius: Float = 8f) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius * density
    }

    private fun toolbarIcon(res: Int, desc: String, onClick: () -> Unit) = ImageView(context).apply {
        setImageResource(res)
        setColorFilter(cText)
        contentDescription = desc
        setPadding(dp(9f), dp(9f), dp(9f), dp(9f))
        background = roundRect(cSpecial, 10f)
        setOnClickListener { haptic(this); onClick() }
    }

    private fun buildToolbar() {
        statusRow.orientation = HORIZONTAL
        statusRow.gravity = Gravity.CENTER_VERTICAL
        statusRow.setPadding(dp(10f), dp(4f), dp(6f), dp(4f))

        statusDot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cMuted) }
        statusRow.addView(statusDot, LayoutParams(dp(10f), dp(10f)).apply { marginEnd = dp(8f) })

        statusText.setTextColor(cText)
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        statusText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        statusText.maxLines = 1
        statusText.contentDescription = "Status do registro"
        statusRow.addView(statusText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val size = dp(34f)
        fun lp() = LayoutParams(size, size).apply { marginStart = dp(6f) }
        saveButton.apply {
            setImageResource(R.drawable.ic_kb_save); setColorFilter(cText)
            contentDescription = "Salvar registro agora"
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f)); background = roundRect(cSpecial, 10f)
            setOnClickListener { haptic(this); listener.onSaveNow() }
        }
        pauseButton.apply {
            setImageResource(R.drawable.ic_kb_pause); setColorFilter(cText)
            contentDescription = "Pausar registro"
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f)); background = roundRect(cSpecial, 10f)
            setOnClickListener { haptic(this); listener.onTogglePause() }
        }
        statusRow.addView(saveButton, lp())
        statusRow.addView(pauseButton, lp())
        statusRow.addView(toolbarIcon(R.drawable.ic_kb_history, "Abrir KeyHistory") { listener.onOpenApp() }, lp())
        statusRow.addView(toolbarIcon(R.drawable.ic_kb_keyboard, "Trocar teclado") { listener.onSwitchKeyboard() }, lp())

        variantsRow.orientation = HORIZONTAL
        variantsRow.gravity = Gravity.CENTER_VERTICAL
        variantsRow.setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(variantsRow)
            visibility = GONE
            tag = "variants"
        }
        toolbar.addView(statusRow, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        toolbar.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setIndicator(RecordingIndicator.Disabled, canSave = false)
    }

    fun setIndicator(state: RecordingIndicator, canSave: Boolean) {
        indicator = state
        val (color, text) = when (state) {
            RecordingIndicator.Active -> cGreen to "● REC · Registro ativo"
            RecordingIndicator.Paused -> cAmber to "Registro pausado"
            RecordingIndicator.Disabled -> cMuted to "Registro desativado"
            is RecordingIndicator.Protected -> cRed to "Não registrado · ${state.reason}"
        }
        (statusDot.background as GradientDrawable).setColor(color)
        statusText.text = text.removePrefix("● ")
        statusText.setTextColor(if (state is RecordingIndicator.Protected) Color.parseColor("#FCA5A5") else cText)
        val showPause = state == RecordingIndicator.Active || state == RecordingIndicator.Paused
        pauseButton.visibility = if (showPause) VISIBLE else GONE
        pauseButton.setImageResource(if (state == RecordingIndicator.Paused) R.drawable.ic_kb_play else R.drawable.ic_kb_pause)
        pauseButton.contentDescription = if (state == RecordingIndicator.Paused) "Retomar registro" else "Pausar registro"
        saveButton.visibility = if (state == RecordingIndicator.Active) VISIBLE else GONE
        saveButton.alpha = if (canSave) 1f else 0.4f
        saveButton.isEnabled = canSave
        if (pendingDead != null) statusText.text = "Acento ${pendingDead} · digite a vogal"
    }

    private fun showVariants(base: Key, variants: List<String>) {
        variantsRow.removeAllViews()
        val all = listOf(applyShift(base.label)) + variants.map { applyShift(it) }
        for (v in all.distinct()) {
            val tv = TextView(context).apply {
                text = v
                gravity = Gravity.CENTER
                setTextColor(cText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                background = roundRect(cPrimary, 10f)
                contentDescription = "Variante $v"
                setOnClickListener { haptic(this); hideVariants(); commitChar(v) }
            }
            variantsRow.addView(tv, LayoutParams(dp(42f), dp(34f)).apply { marginEnd = dp(6f) })
        }
        statusRow.visibility = INVISIBLE
        toolbar.findViewWithTag<View>("variants").visibility = VISIBLE
    }

    fun hideVariants() {
        toolbar.findViewWithTag<View>("variants").visibility = GONE
        statusRow.visibility = VISIBLE
    }

    // ---------------------------------------------------------------- keys

    fun setLayer(newLayer: Layer) {
        layer = newLayer
        buildKeys()
    }

    fun setShift(state: ShiftState) {
        if (shift == state) return
        shift = state
        refreshLabels()
    }

    private fun buildKeys() {
        keysContainer.removeAllViews()
        keyViews.clear()
        iconViews.clear()
        for (row in KeyboardLayouts.rows(layer)) {
            val rowView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                weightSum = 10f
            }
            for (key in row) {
                val view = createKeyView(key)
                rowView.addView(view, LayoutParams(0, keyHeight, key.weight).apply {
                    setMargins(dp(2.5f), dp(3f), dp(2.5f), dp(3f))
                })
            }
            keysContainer.addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        refreshLabels()
    }

    private fun isSpecial(key: Key) = key.kind != KeyKind.CHAR && key.kind != KeyKind.SPACE && key.kind != KeyKind.DEAD

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: Key): View {
        val iconRes = when (key.kind) {
            KeyKind.BACKSPACE -> R.drawable.ic_kb_backspace
            KeyKind.SHIFT -> R.drawable.ic_kb_shift
            else -> 0
        }
        val baseColor = when {
            key.kind == KeyKind.ENTER -> cPrimary
            isSpecial(key) -> cSpecial
            else -> cKey
        }
        val view: View = if (iconRes != 0) {
            ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(cText)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
            }.also { iconViews += it to key }
        } else {
            TextView(context).apply {
                gravity = Gravity.CENTER
                setTextColor(cText)
                includeFontPadding = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.kind == KeyKind.CHAR || key.kind == KeyKind.DEAD) 21f else 14f)
                if (key.kind != KeyKind.CHAR) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }.also { keyViews += it to key }
        }
        val normal = roundRect(baseColor, 7f)
        val pressed = roundRect(if (key.kind == KeyKind.ENTER) Color.parseColor("#2563EB") else cPressed, 7f)
        view.background = normal
        view.elevation = dp(1f).toFloat()
        view.isClickable = true

        var longPressed = false
        var repeating = false
        val repeatRunnable = object : Runnable {
            override fun run() {
                repeating = true
                listener.onBackspace()
                handler.postDelayed(this, 55)
            }
        }
        val longPressRunnable = Runnable {
            when {
                key.kind == KeyKind.CHAR && key.variants.isNotEmpty() -> {
                    longPressed = true; haptic(view); showVariants(key, key.variants)
                }
                key.kind == KeyKind.SPACE -> {
                    longPressed = true; haptic(view); listener.onSwitchKeyboard()
                }
            }
        }

        view.setOnClickListener { onKeyClicked(key) }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longPressed = false; repeating = false
                    v.background = pressed
                    haptic(v)
                    if (key.kind == KeyKind.BACKSPACE) handler.postDelayed(repeatRunnable, 420)
                    else handler.postDelayed(longPressRunnable, 380)
                }
                MotionEvent.ACTION_UP -> {
                    v.background = normal
                    handler.removeCallbacks(repeatRunnable)
                    handler.removeCallbacks(longPressRunnable)
                    if (!longPressed && !repeating) v.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.background = normal
                    handler.removeCallbacks(repeatRunnable)
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            true
        }
        return view
    }

    private fun applyShift(label: String) = if (shift != ShiftState.OFF) label.uppercase() else label

    private fun refreshLabels() {
        for ((tv, key) in keyViews) {
            val text = when (key.kind) {
                KeyKind.CHAR -> applyShift(key.label)
                KeyKind.SPACE -> "KeyHistory"
                KeyKind.ENTER -> enterLabel
                else -> key.label
            }
            tv.text = text
            if (key.kind == KeyKind.SPACE) tv.setTextColor(cMuted)
            tv.contentDescription = when (key.kind) {
                KeyKind.CHAR, KeyKind.DEAD -> "Tecla ${key.label}"
                KeyKind.SPACE -> "Tecla Espaço"
                KeyKind.ENTER -> "Tecla Enter"
                else -> "Tecla ${key.label}"
            }
        }
        for ((iv, key) in iconViews) {
            when (key.kind) {
                KeyKind.SHIFT -> {
                    iv.setImageResource(
                        when (shift) {
                            ShiftState.OFF -> R.drawable.ic_kb_shift
                            ShiftState.ONCE -> R.drawable.ic_kb_shift_on
                            ShiftState.CAPS -> R.drawable.ic_kb_caps
                        }
                    )
                    iv.setColorFilter(if (shift == ShiftState.OFF) cText else cPrimary)
                    iv.contentDescription = "Tecla Shift"
                }
                KeyKind.BACKSPACE -> iv.contentDescription = "Tecla Apagar"
                else -> Unit
            }
        }
    }

    private fun onKeyClicked(key: Key) {
        hideVariants()
        when (key.kind) {
            KeyKind.CHAR -> commitChar(applyShift(key.label))
            KeyKind.DEAD -> {
                pendingDead = key.label
                setLayer(Layer.LETTERS)
                statusText.text = "Acento ${key.label} · digite a vogal"
            }
            KeyKind.SPACE -> commitChar(" ")
            KeyKind.BACKSPACE -> {
                if (pendingDead != null) { pendingDead = null; listener.onText("") } else listener.onBackspace()
            }
            KeyKind.ENTER -> listener.onEnter()
            KeyKind.SHIFT -> {
                val now = System.currentTimeMillis()
                shift = when {
                    shift == ShiftState.CAPS -> ShiftState.OFF
                    now - lastShiftTap < 350 -> ShiftState.CAPS
                    shift == ShiftState.ONCE -> ShiftState.OFF
                    else -> ShiftState.ONCE
                }
                lastShiftTap = now
                refreshLabels()
            }
            KeyKind.TO_SYMBOLS -> setLayer(Layer.SYMBOLS)
            KeyKind.TO_SYMBOLS2 -> setLayer(Layer.SYMBOLS2)
            KeyKind.TO_LETTERS -> setLayer(Layer.LETTERS)
        }
    }

    private fun commitChar(value: String) {
        val dead = pendingDead
        val out = if (dead != null) {
            pendingDead = null
            KeyboardLayouts.compose(dead, value)
        } else value
        listener.onText(out)
        if (shift == ShiftState.ONCE && value != " ") {
            shift = ShiftState.OFF
            refreshLabels()
        }
    }

    private fun haptic(v: View) {
        if (hapticEnabled) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
