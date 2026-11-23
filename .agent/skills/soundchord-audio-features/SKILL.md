---
name: soundchord-audio-features
description: Arquitectura de motor de audio, PlaybackService, CrossfadeController, AutoMix ONNX bajo demanda, Radio en vivo y Reconocimiento de audio.
---

# Guía de Audio, Crossfade, AutoMix y `:soundchord:features`

Guía técnica integral sobre la arquitectura de reproducción, el controlador de fundido (`CrossfadeController`), el motor inteligente `AutoMix`, la sincronización de análisis ONNX con fallback automático y los efectos visuales del reproductor.

---

## 1. Aislamiento Modular y Servicios de Audio
- **Features independientes:** Toda lógica y UI de Radio en vivo (`PreferencesRadioFavoritesStore`, `RadioRepository`) y reconocimiento musical (`RecognitionHistoryStore`, `MusicRecognitionService`) residen de forma aislada en `:soundchord:features`.
- **Motor central de audio:** `PlaybackService.kt` y `CrossfadeController.kt` residen en `:app` coordinando las instancias duales de ExoPlayer para transiciones continuas.

---

## 2. Motor de AutoMix y Transiciones Inteligentes (Ticket #22)

### A. Modelos ONNX Bajo Demanda y Detección Dinámica
- Los modelos ONNX de seguimiento de beats (`BeatTracker`) y vocales (`VocalTracker`) se descargan bajo demanda en almacenamiento interno mediante `AutomixModels`.
- `PlaybackService` y `TrackAnalyzer` detectan dinámicamente la presencia de los pesos sin requerir reinicio de la aplicación.

### B. Ciclo de Análisis y Estados de Transición
1. **Petición en segundo plano:** Al reproducir una pista, `requestAnalysisAround` solicita el análisis predictivo tanto de la canción activa como de la siguiente en la cola.
2. **Evaluación de tempo y compás:** Si el análisis de ambos temas concluye a tiempo (`TrackAnalysisState.ANALYSED` y `MEASURED_ENOUGH_TO_ENTER_ON`), `TransitionPlanner` genera un plan de mezcla inteligente por tempo/compás (beatmatching, fraseo de compases, filtro sweep/bass swap).
3. **Indicador en la barra de progreso:** El marcador visual de la ventana de transición (`AppSettings.smartTransitionWindow`) solo se muestra en la barra de búsqueda una vez que ambas canciones han sido medidas con precisión para evitar saltos bruscos.

### C. Fallback Automático de Crossfade al Tramo Final (Protección Anti-Corte en Seco)
- **Problema resuelto:** Cuando el usuario adelanta la reproducción (`seek`) cerca del final de una canción o el análisis de la siguiente pista está en estado `analysing...` / `waiting` y no concluye antes del margen final:
  - El sistema **no aborta ni permite que la pista termine abruptamente**.
  - Si el tiempo restante alcanza la ventana final (`remaining <= ARM_LEAD_MS + rampMs`), el motor planifica y **dispara automáticamente un crossfade de respaldo** (`smart-analysis-fallback`) en los últimos segundos.
  - **Re-planificación en vuelo:** Si el análisis ONNX concluye mientras el crossfade de respaldo está armado (`ARMING`) antes del inicio auditivo del fundido, `CrossfadeController` cancela el armado provisional y re-planifica inmediatamente con la mezcla inteligente por beats.

### D. Prioridades y Coexistencia de Modos
- **Reproducción sin pausas en álbumes (`albumGaplessPlayback` - Ticket #31):** Tiene máxima prioridad; si dos canciones consecutivas pertenecen al mismo álbum y el ajuste está activo, omiten AutoMix y crossfade para reproducirse en modo gapless nativo.
- **Fundido manual en Siguiente / Anterior (`crossfadeOnSkip` - Ticket #34):** Opera de forma independiente con `manualFade = true` sin alterar los indicadores ni el estado visual de AutoMix.

---

## 3. Barra Animada de Progreso y Efectos Visuales (`NowPlayingScreen.kt` / `ThinSlider.kt` / `PlayerControls.kt`)

### A. Barrido de Carga y Preparación (`MixSheen`)
- Durante estados de carga, búfer o preparación de cambio de pista (`loading = versionSwitching`), `ThinSlider` activa `MixSheen`, dibujando un halo continuo que se desplaza de lado a lado con animación suavizada en bucle inverso (*ping-pong*).

### B. Efecto Shine Dinámico en la Barra y Etiqueta "Mixing"
- **Barra de reproducción (`ThinSlider.kt`):** Mientras corre la mezcla de AutoMix, `mixPulse.shine` y `mixPulse.shineStrength` dibujan un resplandor luminoso suave que recorre toda la barra de lado a lado al ritmo del compás.
- **Etiqueta "Mixing" (`PlayerControls.kt`):** El texto descriptivo "Mixing" / "Mezclando" incorpora un barrido de gradiente dinámico con `BlendMode.SrcIn` sincronizado exactamente con el reloj de pulso de la barra.

### C. Integridad Visual de Carátulas
- La animación visual de disolución y desplazamiento de carátula (`rememberArtworkDissolve`) en SoundChord permanece completamente intacta, garantizando una transición estética estilo Apple Music fluida y libre de parpadeos.
