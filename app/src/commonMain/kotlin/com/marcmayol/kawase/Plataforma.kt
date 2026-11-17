package com.marcmayol.kawase

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Donde se guarda el último tipo bueno: SharedPreferences en Android, NSUserDefaults en iOS. */
interface Almacen {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
}

/** GET de texto. Lanza si no hay red o la respuesta no es 200. */
expect suspend fun descargarTexto(url: String): String

/** Número con los separadores del idioma del móvil y exactamente estos decimales. */
expect fun formatoNumero(valor: Double, decimales: Int): String

/** "2026-11-13" como lo escribe el móvil ("13 nov 2026"). Si no se entiende, tal cual. */
expect fun fechaLegible(iso: String): String

/** Colores del sistema donde los hay (Android 12+); si no, los de la app. */
@Composable
expect fun esquemaColores(oscuro: Boolean): ColorScheme
