package com.marcmayol.kawase

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Cuántos yenes vale un euro, de qué día es (yyyy-MM-dd) y de dónde salió. */
data class Tipo(val yenesPorEuro: Double, val fecha: String, val fuente: String)

object Fuentes {
    /** Referencia diaria del Banco Central Europeo: oficial, sin clave y sin cuotas. */
    const val BCE_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"

    /** Frankfurter sirve los mismos datos del BCE; solo se usa si el BCE no responde. */
    const val FRANKFURTER_URL = "https://api.frankfurter.dev/v1/latest?base=EUR&symbols=JPY"

    /** Último tipo conocido al publicar: solo se usa si la app nunca ha tenido red. */
    val SEMILLA = Tipo(176.83, "2026-11-13", "BCE")

    fun leerBce(xml: String): Tipo? {
        val fecha = Regex("""time=['"](\d{4}-\d{2}-\d{2})['"]""").find(xml)?.groupValues?.get(1) ?: return null
        val tipo = Regex("""currency=['"]JPY['"]\s+rate=['"]([\d.]+)['"]""").find(xml)
            ?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return if (tipo > 0) Tipo(tipo, fecha, "BCE") else null
    }

    fun leerFrankfurter(json: String): Tipo? {
        val fecha = Regex(""""date"\s*:\s*"(\d{4}-\d{2}-\d{2})"""").find(json)?.groupValues?.get(1) ?: return null
        val tipo = Regex(""""JPY"\s*:\s*([\d.]+)""").find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return if (tipo > 0) Tipo(tipo, fecha, "BCE") else null
    }
}

/** Guarda el último tipo bueno y lo renueva cuando hay red. */
class TipoCambio(context: Context) {
    private val prefs = context.getSharedPreferences("tipo", Context.MODE_PRIVATE)

    fun guardado(): Tipo {
        val tipo = prefs.getString("tipo", null)?.toDoubleOrNull()
        val fecha = prefs.getString("fecha", null)
        val fuente = prefs.getString("fuente", null)
        return if (tipo != null && tipo > 0 && fecha != null && fuente != null) Tipo(tipo, fecha, fuente)
        else Fuentes.SEMILLA
    }

    /** null si ahora no hay manera de conseguirlo: se sigue con el guardado. */
    suspend fun actualizar(): Tipo? = withContext(Dispatchers.IO) {
        val nuevo = runCatching { Fuentes.leerBce(descargar(Fuentes.BCE_URL)) }.getOrNull()
            ?: runCatching { Fuentes.leerFrankfurter(descargar(Fuentes.FRANKFURTER_URL)) }.getOrNull()
            ?: return@withContext null
        prefs.edit()
            .putString("tipo", nuevo.yenesPorEuro.toString())
            .putString("fecha", nuevo.fecha)
            .putString("fuente", nuevo.fuente)
            .apply()
        nuevo
    }

    private fun descargar(url: String): String {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 6_000
        con.readTimeout = 6_000
        try {
            if (con.responseCode != 200) error("HTTP ${con.responseCode}")
            return con.inputStream.bufferedReader().use { it.readText() }
        } finally {
            con.disconnect()
        }
    }
}
