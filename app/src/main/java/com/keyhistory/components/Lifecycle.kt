package com.keyhistory.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Executa [action] sempre que a tela volta ao primeiro plano (ex.: após ativar o teclado). */
@Composable
fun OnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current = rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) current.value() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
