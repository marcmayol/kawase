package com.marcmayol.kawase

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Mismo fichero y claves que la 0.1: al actualizar se conserva el tipo guardado. */
class AlmacenPrefs(context: Context) : Almacen {
    private val prefs = context.getSharedPreferences("tipo", Context.MODE_PRIVATE)
    override fun leer(clave: String): String? = prefs.getString(clave, null)
    override fun guardar(clave: String, valor: String) {
        prefs.edit().putString(clave, valor).apply()
    }
}

actual suspend fun descargarTexto(url: String): String = withContext(Dispatchers.IO) {
    val con = URL(url).openConnection() as HttpURLConnection
    con.connectTimeout = 6_000
    con.readTimeout = 6_000
    try {
        if (con.responseCode != 200) error("HTTP ${con.responseCode}")
        con.inputStream.bufferedReader().use { it.readText() }
    } finally {
        con.disconnect()
    }
}

actual fun formatoNumero(valor: Double, decimales: Int): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimales
        maximumFractionDigits = decimales
    }.format(valor)

actual fun fechaLegible(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrDefault(iso)

@Composable
actual fun esquemaColores(oscuro: Boolean): ColorScheme {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val contexto = LocalContext.current
        return if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
    }
    return if (oscuro) esquemaOscuro else esquemaClaro
}
