package com.marcmayol.kawase

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marcm.actualizador.Modo

class MainActivity : ComponentActivity() {
    private val app get() = application as KawaseApp

    /** Sube en cada onResume: la pantalla vuelve a pedir el tipo al volver a la app. */
    private var vueltas by mutableIntStateOf(0)

    /** Texto que llega de "Compartir" o del menú de selección de texto. */
    private var entrante by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) entrante = textoDe(intent)
        val updates = if (BuildConfig.DEBUG) null else app.actualizador
        setContent {
            App(
                tipoCambio = app.tipoCambio,
                vueltas = vueltas,
                entrante = entrante,
                onEntranteUsado = { entrante = null },
                cabecera = {
                    if (updates != null) {
                        LaunchedEffect(Unit) { updates.comprobar(Modo.AUTOMATICO) }
                        val estado by updates.estado.collectAsState()
                        UpdateBanner(estado, onUpdate = { updates.actualizarAhora() })
                    }
                },
            )
        }
        if (!BuildConfig.DEBUG) app.actualizador.programarPeriodica()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        textoDe(intent)?.let { entrante = it }
    }

    override fun onResume() {
        super.onResume()
        vueltas++
        if (!BuildConfig.DEBUG) app.actualizador.onPermisoQuizaConcedido()
    }

    private fun textoDe(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }
}
