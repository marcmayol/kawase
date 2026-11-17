@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.marcmayol.kawase

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.stringWithContentsOfURL

class AlmacenUserDefaults : Almacen {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun leer(clave: String): String? = defaults.stringForKey(clave)
    override fun guardar(clave: String, valor: String) {
        defaults.setObject(valor, forKey = clave)
    }
}

actual suspend fun descargarTexto(url: String): String = withContext(Dispatchers.IO) {
    val direccion = NSURL.URLWithString(url) ?: error("URL no válida: $url")
    NSString.stringWithContentsOfURL(direccion, NSUTF8StringEncoding, null) ?: error("Sin respuesta de $url")
}

actual fun formatoNumero(valor: Double, decimales: Int): String {
    val formato = NSNumberFormatter()
    formato.numberStyle = NSNumberFormatterDecimalStyle
    formato.locale = NSLocale.currentLocale
    formato.minimumFractionDigits = decimales.toULong()
    formato.maximumFractionDigits = decimales.toULong()
    return formato.stringFromNumber(NSNumber(double = valor)) ?: valor.toString()
}

actual fun fechaLegible(iso: String): String {
    val entrada = NSDateFormatter()
    entrada.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    entrada.dateFormat = "yyyy-MM-dd"
    val fecha = entrada.dateFromString(iso) ?: return iso
    val salida = NSDateFormatter()
    salida.locale = NSLocale.currentLocale
    salida.dateStyle = NSDateFormatterMediumStyle
    salida.timeStyle = NSDateFormatterNoStyle
    return salida.stringFromDate(fecha)
}

@Composable
actual fun esquemaColores(oscuro: Boolean): ColorScheme = if (oscuro) esquemaOscuro else esquemaClaro
