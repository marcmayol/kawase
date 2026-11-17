package com.marcmayol.kawase

/**
 * Convierte lo que se escribe o se pega en un importe. Acepta lo que sale en una
 * etiqueta japonesa o en una web: "3,980円（税込）", "¥1.500", "１５００", "3万5千",
 * "1.5万", "2k", "12 000" y sumas ("1500 + 300").
 */
object Importe {

    private val unidades = mapOf(
        '百' to 100.0, '千' to 1_000.0, 'k' to 1_000.0,
        '万' to 10_000.0, '萬' to 10_000.0, '億' to 100_000_000.0,
    )
    private val grandes = setOf('万', '萬', '億')

    fun leer(texto: String): Double? {
        val limpio = normalizar(texto)
        if (limpio.isBlank()) return null
        var total = 0.0
        var alguno = false
        for (termino in limpio.split('+')) {
            val valor = leerTermino(termino) ?: continue
            total += valor
            alguno = true
        }
        return if (alguno) total else null
    }

    /** Ancho completo a ASCII, minúsculas, y fuera todo lo que no sea número, separador o unidad. */
    private fun normalizar(texto: String): String = buildString {
        for (c in texto.lowercase()) {
            val n = when (c) {
                in '０'..'９' -> '0' + (c - '０')
                '，', '、' -> ','
                '．' -> '.'
                '＋' -> '+'
                else -> c
            }
            if (n.isDigit() || n == '.' || n == ',' || n == '+' || n in unidades) append(n)
        }
    }

    private fun leerTermino(termino: String): Double? {
        var total = 0.0
        var seccion = 0.0
        var numero = StringBuilder()
        var alguno = false
        for (c in termino) {
            val unidad = unidades[c]
            if (unidad == null) {
                numero.append(c)
                continue
            }
            val n = if (numero.isEmpty()) 1.0 else leerNumero(numero.toString()) ?: return null
            numero = StringBuilder()
            alguno = true
            if (c in grandes) {
                total += (seccion + n) * unidad
                seccion = 0.0
            } else {
                seccion += n * unidad
            }
        }
        if (numero.isNotEmpty()) {
            val n = leerNumero(numero.toString())
            if (n != null) {
                seccion += n
                alguno = true
            }
        }
        return if (alguno) total + seccion else null
    }

    /**
     * Separadores: si hay punto y coma, el último es el decimal. Si solo hay uno repetido,
     * son de miles. Si aparece una vez seguido de exactamente tres cifras, es de miles
     * ("1.500" y "1,500" son 1500); si no, decimal ("1,5" y "2.50").
     */
    internal fun leerNumero(s: String): Double? {
        val t = s.trim('.', ',')
        if (t.isEmpty() || t.none { it.isDigit() }) return null
        val puntos = t.count { it == '.' }
        val comas = t.count { it == ',' }
        val normal = when {
            puntos > 0 && comas > 0 -> {
                val dec = if (t.lastIndexOf('.') > t.lastIndexOf(',')) '.' else ','
                val miles = if (dec == '.') ',' else '.'
                t.replace(miles.toString(), "").replace(dec, '.')
            }
            puntos + comas == 0 -> t
            else -> {
                val sep = if (puntos > 0) '.' else ','
                val tras = t.substringAfterLast(sep)
                if (puntos + comas > 1 || tras.length == 3) t.replace(sep.toString(), "")
                else t.replace(sep, '.')
            }
        }
        return normal.toDoubleOrNull()
    }
}
