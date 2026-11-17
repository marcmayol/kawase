package com.marcmayol.kawase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIViewController

private val tipoCambio by lazy { TipoCambio(AlmacenUserDefaults()) }

/** Sube cada vez que la app vuelve a primer plano, como el onResume de Android. */
private var vueltas by mutableIntStateOf(0)

/** Lo que pinta la app de iOS (iosApp/iosApp/ContentView.swift). */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = UIApplicationWillEnterForegroundNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> vueltas++ }
    return ComposeUIViewController { App(tipoCambio, vueltas = vueltas) }
}
