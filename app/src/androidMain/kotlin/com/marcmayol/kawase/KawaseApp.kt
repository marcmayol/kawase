package com.marcmayol.kawase

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig

class KawaseApp : Application() {
    /** Kawase se reparte por DracApps: mismo módulo de actualización que Rodet y Faltis. */
    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/kawase/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    val tipoCambio: TipoCambio by lazy { TipoCambio(AlmacenPrefs(this)) }
}
