# Yenes

Conversor de yenes a euros para Android e iOS. Escribes el precio y te lo dice en euros.

- Tipo diario del BCE (`eurofxref-daily.xml`); si no responde, Frankfurter. Sin red, el último guardado.
- Lee lo que pone en las etiquetas: `¥3,980（税込）`, `3万5千`, `１５００`, `1500 + 300`.
- 税抜/税別 activa el +10 % de impuestos.
- En Android: menú de selección de texto («Yenes a euros») y «Compartir».
- También de euros a yenes.

## Cómo está hecho

Kotlin Multiplatform con Compose Multiplatform. Casi todo está en `app/src/commonMain`: la pantalla,
el lector de precios y las fuentes del tipo. Cada plataforma solo pone lo suyo (`Plataforma.kt`):

| | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| Red | `HttpURLConnection` | `NSString.stringWithContentsOfURL` |
| Último tipo | `SharedPreferences` | `NSUserDefaults` |
| Números y fechas | `NumberFormat`, `java.time` | `NSNumberFormatter`, `NSDateFormatter` |
| Colores | Los del sistema (Android 12+) | Rojo propio |
| Actualizaciones | `:actualizador` (DracApps) | — |

Tests comunes: `gradlew :app:testDebugUnitTest` (en Windows). La parte de iOS no se compila en
Windows: el workflow `.github/workflows/ios.yml` lo hace en un Mac de GitHub en cada push, pasa
los tests en el simulador de iPhone, abre la app y guarda una captura.

## Android

Se reparte por [DracApps](https://marcmayol.com/DracApps/) y se actualiza sola.

```
python scripts/publicar_release.py --notas "…"
```

La versión está en `gradle.properties`.

## iPhone

Hace falta un Mac con Xcode. Con un Apple ID gratuito vale, pero la app caduca a los 7 días y hay
que volver a instalarla desde Xcode (con la cuenta de desarrollador de pago, un año).

```
brew install xcodegen
cd iosApp && xcodegen && open Yenes.xcodeproj
```

En Xcode: Yenes → Signing & Capabilities → elige tu equipo, conecta el iPhone y dale a Run.
La versión de iOS está en `iosApp/project.yml`.
