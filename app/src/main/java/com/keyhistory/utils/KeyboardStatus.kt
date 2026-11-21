package com.keyhistory.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.keyhistory.keyboard.KeyHistoryImeService

data class KeyboardState(val installed: Boolean, val enabled: Boolean, val selected: Boolean)

object KeyboardStatus {
    fun read(context: Context): KeyboardState {
        val imm = context.getSystemService(InputMethodManager::class.java)
        val ownPkg = context.packageName
        val installed = imm.inputMethodList.any { it.packageName == ownPkg }
        val enabled = imm.enabledInputMethodList.any { it.packageName == ownPkg }
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        val selected = current.startsWith("$ownPkg/") || current == KeyHistoryImeService.imeId(ownPkg)
        return KeyboardState(installed, enabled, selected)
    }

    fun openInputMethodSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun showPicker(context: Context) {
        context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
    }
}
