---
name: soundchord-audio-features
description: Arquitectura de motor de audio, PlaybackService, CrossfadeController, AutoMix ONNX bajo demanda, Radio en vivo y Reconocimiento de audio.
---

# Guía de Audio, Crossfade, AutoMix y `:soundchord:features`

1. **Aislamiento modular:**
   - Toda lógica y UI de Radio en vivo, favoritos de emisoras (`PreferencesRadioFavoritesStore`) y reconocimiento de canciones (`RecognitionHistoryStore`) reside en `:soundchord:features`.
2. **Fundido cruzado (*Crossfade*) y AutoMix:**
   - El fundido cruzado manual (`seekToNext` / `seekToPrevious`) y la mezcla automática (`AutoMix`) en `CrossfadeController.kt` / `PlaybackService.kt` deben coordinarse sin bloquearse mutuamente ni romper la animación de transición en `NowPlayingScreen.kt`.
   - El modelo ONNX de AutoMix se descarga bajo demanda en almacenamiento interno y `PlaybackService` debe detectarlo dinámicamente sin exigir reiniciar la app.