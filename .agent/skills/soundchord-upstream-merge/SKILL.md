---
name: soundchord-upstream-merge
description: Protocolo obligatorio para portar commits o funciones desde el remoto upstream (BitChord) sin sobrescribir ni borrar características exclusivas de SoundChord.
---

# Protocolo de Fusión Segura con `upstream` (BitChord)

Guía técnica y protocolo obligatorio para portar mejoras, correcciones o commits desde el repositorio remoto `upstream` (BitChord) hacia la rama activa de desarrollo de **SoundChord** (`release/v1.3`).

---

## 1. Flujo Obligatorio Antes de Portar Cualquier Commit

Cualquier integración desde `upstream` debe seguir rigurosamente este procedimiento:

```
[ upstream commit ]
        │
        ▼
1. git show <hash> --stat (Identificar archivos modificados)
        │
        ▼
2. git diff <hash>^! -- <archivo> (Analizar diferencias lógicas)
        │
        ▼
3. Integración quirúrgica por bloques (NUNCA checkout completo)
        │
        ▼
4. Verificación de Zonas Protegidas y Branding
```

### Reglas Críticas de Fusión:
- **Inspección Previa:** Ejecutar siempre `git show <hash> --stat` y examinar el diff detallado antes de tocar cualquier archivo del proyecto.
- **Prohibido `git checkout upstream/...`:** Jamás sobrescribir archivos completos que contengan código propio de SoundChord. La integración se hace manualmente por bloques de código (hunk por hunk) combinando ambos comportamientos.
- **Protección de Identidad y Branding:**
  - Nunca reintroducir el nombre `"BitChord"` en cadenas de UI destinadas a SoundChord.
  - El `applicationId` en `app/build.gradle.kts` debe permanecer como `com.dev.bitchord` (en `devDebug`) o `com.andrex808.soundchord` (en `prodRelease`), nunca el genérico `com.music.bitchord`.
  - No arrastrar iconos, metadatos, drawables ni enlaces de donación/repositorio propios de BitChord sobre los de SoundChord.

---

## 2. Inventario de Zonas Protegidas de SoundChord

Los siguientes archivos y módulos contienen lógica exclusiva de SoundChord que **debe conservarse intacta** durante cualquier fusión:

### A. Cola de Reproducción y Reproductor (`PlayerQueue.kt`, `NowPlayingScreen.kt`, `PlayerFrost.kt`)
- **Fila Superior de 4 Botones Rápidos en la Cola:**
  - Botones de acceso directo simétricos: **Aleatorio**, **Repetir**, **Reproducción automática** y **AutoMix** (`Icons.Rounded.GraphicEq`).
- **Gestión Avanzada de Cola:**
  - Botón `+ Añadir canciones a la cola` conectado con `AddSongsRequest`.
  - Botón `X` (`Icons.Rounded.Close`) en cada fila de pista para eliminación rápida inmediata.
  - Modo de selección múltiple con casillas de verificación (`Checkbox`), botón `Seleccionar` y gestos de deslizamiento con deshacer (`Swipe + Undo`).
- **Estética Esmerilada y Controles (`NowPlayingScreen.kt` / `PlayerFrost.kt`):**
  - Fondo translúcido con tinte y opacidad calculada para los botones circulares de **Favorito** (`★`) y **Más opciones** (`•••`), garantizando legibilidad sobre Canvas animados y portadas claras.
  - Fila de transporte de 5 controles simétricos cuando `showPlayerShuffleRepeatButtons` está activo, respetando el bloqueo de controles (`controlsLocked`) en modo fiesta (*Listen Together*).

### B. Motor de Audio y Servicios (`PlaybackService.kt`, `CrossfadeController.kt`)
- **AutoMix ONNX Bajo Demanda:**
  - Detección reactiva del modelo ONNX en almacenamiento interno, extracción de tempo/beats/energía y mezcla automática sin exigir el reinicio de la aplicación.
- **Fundido Cruzado (*Crossfade*):**
  - Coordinación de fade-in / fade-out en saltos de pista manuales (`seekToNext` / `seekToPrevious`) y transiciones automáticas en `CrossfadeController.kt`.
- **Widgets de Pantalla de Inicio:**
  - Notificación de cambios de estado y sincronización reactiva con los proveedores de widgets `2x2` (`Widget2x2Provider`) y `4x2` (`Widget4x2Provider`).
- **Interoperabilidad con `:soundchord:features`:**
  - Enlaces de reproducción con Radio en vivo (`RadioBrowser`, metadatos de emisoras) y recepción de audio para reconocimiento musical (`MusicRecognitionService`).

### C. Ajustes, Localización y Respaldos (`AppSettings.kt`, `SettingsSheet.kt`, `ReplaySync.kt`, `Backup.kt`)
- **Secciones Exclusivas en Ajustes:**
  - Categorías y opciones dedicadas a Radio en vivo, Reconocimiento musical (Shazam), AutoMix ONNX y selección de salidas de audio.
- **Badges Dinámicos `NUEVO`:**
  - Declaración en `AppSettings.kt` (`FEATURE_<NOMBRE>`), conexión de `R.string.settings_badge_new` y llamada a `AppSettings.markFeatureSeen(...)` al interactuar. Renderizado estricto en una sola línea (`maxLines = 1`, `softWrap = false`).
- **Carátulas Personalizadas en Playlists Locales:**
  - Almacenamiento y persistencia local de portadas personalizadas (`customCoverUri` / `playlist_cover_*`).
- **Respaldo en Google Drive y JSON:**
  - Serialización y restauración de emisoras favoritas (`PreferencesRadioFavoritesStore`), historial de reconocimiento (`RecognitionHistoryStore`) y portadas personalizadas, garantizando retrocompatibilidad con esquemas de respaldo anteriores.

### D. Navegación Inferior (`GlassNavBar.kt`, `FloatingBottomBar.kt`, `AppNavigationHost.kt`)
- **Separación de Toque en Pestañas:**
  - Tocar una pestaña no seleccionada cambia de pantalla al instante sin quedar retenido por animaciones de scroll.
  - `animateScrollToItem(0)` solo debe ejecutarse si la pestaña tocada ya era la pestaña activa.
- **Prevención de Pantallas Superpuestas:**
  - Mantener las transiciones limpias en `AnimatedContent` de `AppNavigationHost.kt`, asegurando que no queden fragmentos visuales fantasma tanto con Liquid Glass activado como desactivado.

### E. Manifiesto y Permisos (`AndroidManifest.xml`)
- Permisos requeridos por funciones exclusivas: `RECORD_AUDIO` (reconocimiento musical), exclusión de permisos no deseados (`READ_PHONE_STATE` con `tools:node="remove"`).
- Declaración de receptores de widgets (`Widget2x2Provider`, `Widget4x2Provider`).

---

## 3. Checklist de Verificación Post-Fusión

Tras portar cualquier commit o conjunto de cambios desde `upstream`, verificar obligatoriamente la siguiente lista antes de entregar el trabajo:

- [ ] **Diff limpio contra zonas protegidas:** Revisar con `git diff` que ninguna funcionalidad exclusiva de SoundChord (AutoMix, 4 botones de cola, eliminación rápida, Radio, Reconocimiento, widgets) haya sido eliminada o revertida.
- [ ] **Localización doble sincronizada:** Si el commit introdujo cadenas nuevas visibles, verificar que estén añadidas tanto en `app/src/main/res/values/strings.xml` como en `app/src/main/res/values-es/strings.xml` (y en `:soundchord:features` si corresponde).
- [ ] **Identidad preservada:** Confirmar que no se hayan introducido referencias a `"BitChord"` en textos visibles para el usuario ni alterado el `applicationId` en `build.gradle.kts`.
- [ ] **Límite JVM de 64 KB en `MainActivity.kt`:** Asegurar que `MainActivity.kt` no supere las 950 líneas y que no se hayan inyectado bloques extensos de UI o estados sueltos en `BitChordApp`.
- [ ] **Ausencia de regresiones visuales:** Verificar que los botones esmerilados, divisores hairline y modales conserven la estética Apple Music/Liquid Glass documentada en `soundchord-ui-glass`.
- [ ] **Sin comandos de compilación:** Recordar no ejecutar `./gradlew` en la consola; el usuario compilará directamente desde Android Studio.
