package com.marcmayol.kawase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImporteTest {
    private fun ok(esperado: Double, texto: String) =
        assertEquals(texto, esperado, Importe.leer(texto) ?: Double.NaN, 1e-9)

    @Test fun numerosSencillos() {
        ok(1500.0, "1500")
        ok(1500.0, "1.500")
        ok(1500.0, "1,500")
        ok(1234567.0, "1.234.567")
        ok(12000.0, "12 000")
    }

    @Test fun decimales() {
        ok(1.5, "1,5")
        ok(2.5, "2.50")
        ok(1234.56, "1.234,56")
        ok(1234.56, "1,234.56")
    }

    @Test fun etiquetasJaponesas() {
        ok(3980.0, "3,980円（税込）")
        ok(1500.0, "¥1,500")
        ok(1500.0, "１５００")
        ok(1500.0, "１，５００円")
    }

    @Test fun unidades() {
        ok(30000.0, "3万")
        ok(35000.0, "3万5千")
        ok(15000.0, "1.5万")
        ok(2000.0, "2k")
        ok(1000.0, "千円")
        ok(120_000_000.0, "1億2000万")
    }

    @Test fun sumas() {
        ok(1800.0, "1500 + 300")
        ok(11000.0, "1万+1000")
    }

    @Test fun nadaQueLeer() {
        assertNull(Importe.leer(""))
        assertNull(Importe.leer("hola"))
        assertNull(Importe.leer("¥"))
    }
}
