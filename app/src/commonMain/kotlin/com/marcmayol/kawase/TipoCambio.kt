package com.marcmayol.kawase

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
class TipoCambio(private val almacen: Almacen) {

    fun guardado(): Tipo {
        val tipo = almacen.leer("tipo")?.toDoubleOrNull()
        val fecha = almacen.leer("fecha")
        val fuente = almacen.leer("fuente")
        return if (tipo != null && tipo > 0 && fecha != null && fuente != null) Tipo(tipo, fecha, fuente)
        else Fuentes.SEMILLA
    }

    /** null si ahora no hay manera de conseguirlo: se sigue con el guardado. */
    suspend fun actualizar(): Tipo? {
        val nuevo = runCatching { Fuentes.leerBce(descargarTexto(Fuentes.BCE_URL)) }.getOrNull()
            ?: runCatching { Fuentes.leerFrankfurter(descargarTexto(Fuentes.FRANKFURTER_URL)) }.getOrNull()
            ?: return null
        almacen.guardar("tipo", nuevo.yenesPorEuro.toString())
        almacen.guardar("fecha", nuevo.fecha)
        almacen.guardar("fuente", nuevo.fuente)
        return nuevo
    }
}
