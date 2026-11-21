package com.keyhistory.keyboard

import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.keyhistory.KeyHistoryApp
import com.keyhistory.MainActivity
import com.keyhistory.repository.AppSettings
import com.keyhistory.security.SensitiveContentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Teclado KeyHistory.
 *
 * Regras de registro (todas precisam ser verdadeiras):
 *  1. O usuário ativou o registro no aplicativo (consentimento explícito).
 *  2. O registro não está pausado (botão no próprio teclado).
 *  3. O campo é elegível ([FieldEligibility]): não é senha, PIN, numérico, sensível,
 *     marcado como não pessoal, interno ou de origem não verificada.
 *  4. O conteúdo não aparenta ser dado sensível ([SensitiveContentFilter]).
 *
 * Somente o texto digitado através deste teclado é considerado; o conteúdo pré-existente
 * do campo nunca é lido.
 */
class KeyHistoryImeService : InputMethodService(), KeyboardListener {

    private val container by lazy { (application as KeyHistoryApp).container }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    private var keyboardView: KeyboardView? = null
    private var settings = AppSettings()
    private val buffer = TypedTextBuffer()

    private var currentEditor: EditorInfo? = null
    private var eligibility: Eligibility = Eligibility.Blocked(BlockReason.UNKNOWN_ORIGIN)
    private var originPackage: String? = null
    private var originAppName: String? = null
    private var category: String = CategoryResolver.TEXT

    override fun onCreate() {
        super.onCreate()
        settingsJob = serviceScope.launch {
            container.settingsRepository.settings.collect {
                settings = it
                keyboardView?.hapticEnabled = it.hapticFeedback
                currentEditor?.let { info -> evaluateField(info) }
                refreshIndicator()
            }
        }
    }

    override fun onDestroy() {
        finalizeSession(save = true)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        return KeyboardView(this, this).also {
            it.hapticEnabled = settings.hapticFeedback
            keyboardView = it
            refreshIndicator()
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val sameField = restarting && currentEditor?.fieldId == attribute.fieldId &&
            currentEditor?.packageName == attribute.packageName
        if (!sameField || fieldIsEmpty()) finalizeSession(save = true)
        currentEditor = attribute
        evaluateField(attribute)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val view = keyboardView ?: return
        view.hideVariants()
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        view.setLayer(
            if (inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_PHONE ||
                inputClass == InputType.TYPE_CLASS_DATETIME
            ) Layer.SYMBOLS else Layer.LETTERS
        )
        view.enterLabel = enterLabelFor(info)
        updateAutoShift()
        refreshIndicator()
    }

    override fun onFinishInput() {
        finalizeSession(save = true)
        currentEditor = null
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // O aplicativo limpou o campo (ex.: botão "enviar" próprio do app): encerra o registro atual.
        if (newSelStart == 0 && newSelEnd == 0 && !buffer.isEmpty() && fieldIsEmpty()) {
            finalizeSession(save = true)
            updateAutoShift()
        }
    }

    // ----------------------------------------------------------- KeyboardListener

    override fun onText(text: String) {
        if (text.isEmpty()) { refreshIndicator(); return }
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        if (isTracking()) buffer.append(text)
        updateAutoShift()
        refreshIndicator()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            buffer.deleteLast()
        }
        updateAutoShift()
        refreshIndicator()
    }

    override fun onEnter() {
        val info = currentEditor
        val ic = currentInputConnection ?: return
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (info == null || noEnterAction || action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.commitText("\n", 1)
            if (isTracking()) buffer.append("\n")
        } else {
            // Ação explícita do usuário (enviar/buscar/concluir): registra antes de executar.
            finalizeSession(save = true)
            ic.performEditorAction(action)
        }
        updateAutoShift()
        refreshIndicator()
    }

    override fun onTogglePause() {
        val paused = !settings.recordingPaused
        if (paused) buffer.clear()
        container.appScope.launch { container.settingsRepository.setPaused(paused) }
    }

    override fun onSaveNow() {
        finalizeSession(save = true, manual = true)
        refreshIndicator()
    }

    override fun onOpenApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onSwitchKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && shouldOfferSwitchingToNextInputMethod()) {
            if (!switchToNextInputMethod(false)) imm.showInputMethodPicker()
        } else imm.showInputMethodPicker()
    }

    // ------------------------------------------------------------------ lógica

    private fun isTracking(): Boolean = settings.isRecordingActive && eligibility == Eligibility.Eligible

    private fun evaluateField(info: EditorInfo) {
        val pkg = info.packageName
        originPackage = pkg
        originAppName = resolveAppName(pkg)
        eligibility = FieldEligibility.evaluate(
            FieldInfo(
                inputType = info.inputType,
                imeOptions = info.imeOptions,
                packageName = pkg,
                hintText = info.hintText?.toString(),
                label = info.label?.toString(),
            ),
            ownPackage = packageName,
            originVerified = originAppName != null,
            excludedPackages = settings.excludedPackages,
        )
        category = CategoryResolver.resolve(info.inputType, info.imeOptions, pkg)
        if (eligibility != Eligibility.Eligible) buffer.clear()
    }

    private fun resolveAppName(pkg: String?): String? {
        if (pkg.isNullOrBlank()) return null
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") packageManager.getApplicationInfo(pkg, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun finalizeSession(save: Boolean, manual: Boolean = false) {
        val text = buffer.drain()
        if (!save || text.isBlank()) return
        if (!settings.isRecordingActive || eligibility != Eligibility.Eligible) return
        if (!settings.autoSave && !manual) return
        val verdict = SensitiveContentFilter.evaluate(text)
        if (verdict != SensitiveContentFilter.Verdict.ALLOWED) {
            Log.i(TAG, "Conteúdo descartado pelo filtro de dados sensíveis: $verdict")
            return
        }
        val pkg = originPackage
        val appName = originAppName
        val cat = category
        container.appScope.launch {
            runCatching { container.historyRepository.save(text.trim(), pkg, appName, cat) }
                .onFailure { Log.e(TAG, "Falha ao salvar registro", it) }
        }
    }

    private fun fieldIsEmpty(): Boolean {
        val ic = currentInputConnection ?: return true
        val before = ic.getTextBeforeCursor(1, 0)
        val after = ic.getTextAfterCursor(1, 0)
        return before.isNullOrEmpty() && after.isNullOrEmpty()
    }

    private fun updateAutoShift() {
        val view = keyboardView ?: return
        val info = currentEditor ?: return
        if (view.shift == ShiftState.CAPS || view.layer != Layer.LETTERS) return
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val caps = inputClass == InputType.TYPE_CLASS_TEXT &&
            (currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0) != 0
        view.setShift(if (caps) ShiftState.ONCE else ShiftState.OFF)
    }

    private fun refreshIndicator() {
        val view = keyboardView ?: return
        val state = when {
            !settings.recordingEnabled -> RecordingIndicator.Disabled
            eligibility is Eligibility.Blocked -> RecordingIndicator.Protected((eligibility as Eligibility.Blocked).reason.message)
            settings.recordingPaused -> RecordingIndicator.Paused
            else -> RecordingIndicator.Active
        }
        view.setIndicator(state, canSave = buffer.isNotBlank())
    }

    private fun enterLabelFor(info: EditorInfo): String {
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return "↵"
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEND -> "Enviar"
            EditorInfo.IME_ACTION_SEARCH -> "Buscar"
            EditorInfo.IME_ACTION_GO -> "Ir"
            EditorInfo.IME_ACTION_NEXT -> "Próx."
            EditorInfo.IME_ACTION_DONE -> "OK"
            EditorInfo.IME_ACTION_PREVIOUS -> "Ant."
            else -> "↵"
        }
    }

    companion object {
        const val TAG = "KeyHistoryIME"
        fun imeId(packageName: String) = "$packageName/${KeyHistoryImeService::class.java.name}"
    }
}
