# Yenes

Conversor de yenes a euros para Android. Escribes el precio y te lo dice en euros.

- Tipo diario del BCE (`eurofxref-daily.xml`); si no responde, Frankfurter. Sin red, el último guardado.
- Lee lo que pone en las etiquetas: `¥3,980（税込）`, `3万5千`, `１５００`, `1500 + 300`.
- 税抜/税別 activa el +10 % de impuestos.
- Menú de selección de texto («Yenes a euros») y «Compartir».
- También de euros a yenes.

Se reparte por [DracApps](https://marcmayol.com/DracApps/) y se actualiza sola.

## Publicar

```
python scripts/publicar_release.py --notas "…"
```

La versión está en `gradle.properties`.
