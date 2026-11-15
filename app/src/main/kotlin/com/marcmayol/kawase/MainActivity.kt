package com.marcmayol.kawase

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.Modo
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    private val app get() = application as KawaseApp

    /** Sube en cada onResume: la pantalla vuelve a pedir el tipo al volver a la app. */
    private var vueltas by mutableIntStateOf(0)

    /** Texto que llega de "Compartir" o de "Kawase" en el menú de selección de texto. */
    private var entrante by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) entrante = textoDe(intent)
        val updates = if (BuildConfig.DEBUG) null else app.actualizador
        setContent {
            KawaseTheme {
                Pantalla(app.tipoCambio, updates, vueltas, entrante, onEntranteUsado = { entrante = null })
            }
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

private enum class Red { CARGANDO, AL_DIA, SIN_RED }

/** Impuesto al consumo de Japón: muchas etiquetas ponen el precio sin él (税抜). */
private const val IMPUESTO_JAPON = 1.10

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Pantalla(
    tipoCambio: TipoCambio,
    updates: Actualizador?,
    vueltas: Int,
    entrante: String?,
    onEntranteUsado: () -> Unit,
) {
    var campo by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var alReves by rememberSaveable { mutableStateOf(false) }
    var conImpuesto by rememberSaveable { mutableStateOf(false) }
    var tipo by remember { mutableStateOf(tipoCambio.guardado()) }
    var red by remember { mutableStateOf(Red.CARGANDO) }
    var refrescos by remember { mutableIntStateOf(0) }
    val foco = remember { FocusRequester() }

    LaunchedEffect(vueltas, refrescos) {
        red = Red.CARGANDO
        val nuevo = tipoCambio.actualizar()
        if (nuevo != null) tipo = nuevo
        red = if (nuevo != null) Red.AL_DIA else Red.SIN_RED
    }
    LaunchedEffect(Unit) {
        runCatching { foco.requestFocus() }
        updates?.comprobar(Modo.AUTOMATICO)
    }
    LaunchedEffect(entrante) {
        if (entrante != null) {
            alReves = false
            campo = TextFieldValue(entrante, TextRange(entrante.length))
            impuestoSegunEtiqueta(entrante)?.let { conImpuesto = it }
            onEntranteUsado()
        }
    }

    fun escribir(trozo: String) {
        val t = campo.text
        val sel = campo.selection
        val nuevo = t.substring(0, sel.min) + trozo + t.substring(sel.max)
        campo = TextFieldValue(nuevo, TextRange(sel.min + trozo.length))
    }

    val importe = Importe.leer(campo.text)
    val yenes = importe?.let { if (alReves) it * tipo.yenesPorEuro else if (conImpuesto) it * IMPUESTO_JAPON else it }
    val euros = importe?.let { if (alReves) it else yenes!! / tipo.yenesPorEuro }

    Scaffold { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (red == Red.CARGANDO) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp).height(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { refrescos++ }) { Text(stringResource(R.string.refresh)) }
                }
            }

            if (updates != null) {
                val estado by updates.estado.collectAsState()
                UpdateBanner(estado, onUpdate = { updates.actualizarAhora() })
            }

            OutlinedTextField(
                value = campo,
                onValueChange = {
                    campo = it
                    impuestoSegunEtiqueta(it.text)?.let { si -> conImpuesto = si }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
                label = { Text(stringResource(if (alReves) R.string.label_eur else R.string.label_jpy)) },
                prefix = { Text(if (alReves) "€ " else "¥ ") },
                textStyle = MaterialTheme.typography.headlineMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = if (campo.text.isNotEmpty()) {
                    { TextButton(onClick = { campo = TextFieldValue("") }) { Text("✕") } }
                } else null,
                supportingText = { Text(stringResource(if (alReves) R.string.hint_eur else R.string.hint_jpy)) },
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!alReves) {
                    AssistChip(onClick = { escribir("万") }, label = { Text(stringResource(R.string.man_chip)) })
                    AssistChip(onClick = { escribir("000") }, label = { Text("000") })
                    AssistChip(onClick = { escribir(" + ") }, label = { Text("+") })
                    FilterChip(
                        selected = conImpuesto,
                        onClick = { conImpuesto = !conImpuesto },
                        label = { Text(stringResource(R.string.tax)) },
                    )
                }
                AssistChip(
                    onClick = {
                        // Al dar la vuelta se lleva el resultado al campo para seguir desde ahí.
                        val llevar = if (alReves) yenes?.let { formatoNumero(it, 0) } else euros?.let { formatoNumero(it, 2) }
                        alReves = !alReves
                        val t = llevar ?: ""
                        campo = TextFieldValue(t, TextRange(t.length))
                    },
                    label = { Text(stringResource(if (alReves) R.string.swap_to_jpy else R.string.swap_to_eur)) },
                )
            }

            Spacer(Modifier.height(4.dp))

            val resultado = when {
                importe == null -> "—"
                alReves -> stringResource(R.string.fmt_jpy, formatoNumero(yenes!!, 0))
                else -> stringResource(R.string.fmt_eur, formatoNumero(euros!!, 2))
            }
            Text(
                resultado,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (importe != null && !alReves && conImpuesto) {
                Text(
                    stringResource(R.string.with_tax, stringResource(R.string.fmt_jpy, formatoNumero(yenes!!, 0))),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text(
                stringResource(R.string.rate_line, formatoNumero(tipo.yenesPorEuro, 2), tipo.fuente, fechaLegible(tipo.fecha)),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (red) {
                Red.SIN_RED -> Text(
                    stringResource(R.string.offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Red.AL_DIA -> Text(stringResource(R.string.updated_now), style = MaterialTheme.typography.bodySmall)
                Red.CARGANDO -> Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
            }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.quick), style = MaterialTheme.typography.titleSmall)
                    val filas = if (alReves) listOf(1.0, 10.0, 50.0, 100.0) else listOf(100.0, 1_000.0, 5_000.0, 10_000.0)
                    for (v in filas) {
                        Row {
                            if (alReves) {
                                Text(stringResource(R.string.fmt_eur, formatoNumero(v, 0)), Modifier.weight(1f))
                                Text(stringResource(R.string.fmt_jpy, formatoNumero(v * tipo.yenesPorEuro, 0)), fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(stringResource(R.string.fmt_jpy, formatoNumero(v, 0)), Modifier.weight(1f))
                                Text(stringResource(R.string.fmt_eur, formatoNumero(v / tipo.yenesPorEuro, 2)), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

        }
    }
}

/** 税抜 / 税別: precio sin impuestos; 税込: ya los lleva. null si la etiqueta no lo dice. */
internal fun impuestoSegunEtiqueta(texto: String): Boolean? = when {
    "税抜" in texto || "税別" in texto -> true
    "税込" in texto -> false
    else -> null
}

private fun formatoNumero(valor: Double, decimales: Int): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimales
        maximumFractionDigits = decimales
    }.format(valor)

private fun fechaLegible(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrDefault(iso)

private val rojoJapon = Color(0xFFBC002D)

private val claro: ColorScheme = lightColorScheme(primary = rojoJapon, secondary = Color(0xFF5B5F66))
private val oscuro: ColorScheme = darkColorScheme(primary = Color(0xFFFF8A9A), secondary = Color(0xFFC3C7CF))

/** Colores del sistema (Material You) donde los hay; si no, rojo japonés. Claro u oscuro según el móvil. */
@Composable
private fun KawaseTheme(content: @Composable () -> Unit) {
    val oscuroSistema = isSystemInDarkTheme()
    val contexto = LocalContext.current
    val esquema = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (oscuroSistema) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        oscuroSistema -> oscuro
        else -> claro
    }
    MaterialTheme(colorScheme = esquema, content = content)
}
