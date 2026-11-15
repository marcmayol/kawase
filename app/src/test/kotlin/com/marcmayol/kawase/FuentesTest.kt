package com.marcmayol.kawase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuentesTest {
    @Test fun bce() {
        val xml = """
            <Cube>
              <Cube time='2026-11-13'>
                <Cube currency='USD' rate='1.1612'/>
                <Cube currency='JPY' rate='176.83'/>
              </Cube>
            </Cube>
        """.trimIndent()
        assertEquals(Tipo(176.83, "2026-11-13", "BCE"), Fuentes.leerBce(xml))
    }

    @Test fun frankfurter() {
        val json = """{"amount":1.0,"base":"EUR","date":"2026-11-13","rates":{"JPY":176.83}}"""
        assertEquals(Tipo(176.83, "2026-11-13", "BCE"), Fuentes.leerFrankfurter(json))
    }

    @Test fun respuestasRaras() {
        assertNull(Fuentes.leerBce("<html>301 Moved Permanently</html>"))
        assertNull(Fuentes.leerFrankfurter("{}"))
    }
}
