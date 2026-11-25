---
name: soundchord-editorial-curation
description: Directrices técnicas y de diseño para la curaduría editorial y recomendaciones de Apple Music y YouTube Music en SoundChord (Inicio, Explorar, Búsqueda y Detalle de Playlists/Álbumes).
---

# Curaduría Editorial y Recomendaciones en SoundChord

Guía técnica y normativa obligatoria para integrar, sincronizar y renderizar contenido editorial de **Apple Music** y **YouTube Music** en SoundChord (`HomeScreen`, `DetailScreen`, `Explore`, `SearchScreen`).

---

## 1. Caché, Concurrencia y Cero Tarjetas Grises

El catálogo editorial de Apple Music se consume a través de `AppleMusicEditorialRepository` con tokens firmados dinámicamente (`AppleMusicTokenProvider`). Para garantizar fluidez de 60/120 FPS sin parpadeos ni tarjetas vacías:

- **Caché en Memoria y Disco:**
  - Todo fetch editorial (`explore`, `searchPlaylists`, secciones de inicio) debe almacenarse en `LruCache` en memoria y persistir en disco con un TTL estricto de **2 horas** (`EditorialCache`).
  - La clave de caché debe incluir storefront y tipo de consulta (`"editorial:$storefront:$key"`).
- **Control Estricto de Concurrencia:**
  - Las llamadas masivas hacia el endpoint editorial deben estar acotadas por un semáforo de concurrencia: `val gate = Semaphore(4)`.
  - Los reintentos por error HTTP 401 deben invalidar el token con `AppleMusicTokenProvider.invalidateToken(bearer)` y reintentar una única vez.
- **Cero Tarjetas Grises (Regla de Oro de Portadas):**
  - **Queda estrictamente prohibido** emitir o renderizar ítems de fallback con `artworkUrl` nula, vacía o con placeholders grises.
  - Toda URL de portada de Apple Music debe normalizarse sustituyendo `{w}x{h}` por `600x600` (`formatAppleArtworkUrl(url, 600, 600, "jpg")`).
  - Filtrar siempre con `isValidArtworkUrl(url)`: si no empieza por `https://` o contiene plantillas sin resolver, el ítem se descarta inmediatamente antes de llegar a la UI.

---

## 2. Estética Limpia Estilo Apple Music (Liquid Glass UI)

SoundChord sigue la estética visual minimalista y prémium de Apple Music:

- **Sin Badges Superpuestos:**
  - Nunca colocar insignias, etiquetas de texto ni banners semi-transparentes encima de las carátulas cuadradas redondeadas (`12.dp`). La portada debe verse limpia y despejada.
- **Sin Subtítulos Redundantes de Curador:**
  - En los encabezados de sección (`SectionHeading`) de `HomeScreen` o `DetailScreen`, nunca añadir subtítulos estáticos como *"Apple Music"* si la cabecera ya indica la temática (ej. *Playlists destacadas*, *También te puede gustar*).
- **Protección contra Recorte de Tipografía (Left Gutter):**
  - Todo bloque de texto debajo de una tarjeta de carrusel (`ShelfCard` o `RelatedPlaylistCard`) debe contar con un padding horizontal limpio (`padding(horizontal = 2.dp)`) para evitar que estilos tipográficos o letras con descendentes/serif se corten en los bordes.

---

## 3. Reglas de Curaduría y Diversidad en Recomendaciones (Tickets #49 / #27)

Al generar recomendaciones en carruseles horizontales (*También te puede gustar*, *Aparece en*, *Recomendaciones relacionadas*):

### Proporción 80/20
- En listas combinadas de **10 a 12 tarjetas en total**:
  - **80% (8 a 10 tarjetas):** Contenido editorial curado oficial de Apple Music (`curatorName` que contenga `"Apple Music"` o validado por editorial).
  - **20% (2 tarjetas):** Contenido complementario de YouTube Music (mezclas comunitarias o relacionadas), intercaladas en posiciones estratégicas (ej. índices 3 y 7).

### Diversidad Estricta por Artista
- **Límite por Artista:** Máximo **1 playlist por cada artista** (o máximo 2 si hay escasez extrema de resultados) para evitar saturar el carrusel con múltiples listas de un mismo artista (*Essentials*, *Setlist*, *Next Steps*).
- **Anti-Artistas Consecutivos:** Utilizar `ensureNoConsecutiveArtists` para asegurar que dos tarjetas del mismo artista nunca aparezcan consecutivas.
- **Balance entre Género y Artistas:**
  - 5–6 tarjetas de listas editoriales de género/vibe multi-artista (ej. *Dale Reggaetón*, *¡Dale Play!*, *Trap Kingz*, *Indie Anthems*).
  - 3–4 tarjetas dedicadas a los artistas más frecuentes de las canciones (1 lista *Essentials* por artista distinto).

### Pureza de Género y Filtros Anti-Ruido
- **Filtros Anti-Junk:**
  - **YouTube Music:** Descartar estrictamente resultados que contengan `karaoke`, `sing king`, `instrumental`, `8d audio`, `sped up`, `slowed`, `lyrics`, `letra`, `nightcore`, `bass boosted` o `tribute`, así como títulos genéricos de una sola palabra (ej. *"Feid"* o *"Drake"*).
  - **Apple Music:** Descartar playlists no musicales como `Track by Track`, `Video Essentials`, `Audio Commentary`, `Comentario de audio` y `Videoclips`.
- **Coherencia de Género:**
  - *Urbano latino / Reggaetón:* Excluir mariachi, banda, norteño, corridos, vallenato, salsa y k-pop.
  - *Vallenato / Tropical:* Excluir mariachi, banda, norteño, urbano, reggaetón y heavy metal, usando storefront regional (`"co"` para vallenato).
  - *Rock / Indie / Alternativa / Hip-Hop:* Excluir urbano latino, reggaetón, corridos y banda si no hay artistas latinos en la pista fuente, consultando storefront `"us"` para catálogo internacional.
- **Prohibición de Charts Genéricos:** Nunca rellenar playlists de género específico con listas tipo `Today's Hits`, `Éxitos de hoy`, `Top 100` o `Viral 50`.
- **Cero Búsquedas por Títulos de Canciones:** Nunca realizar búsquedas editoriales por nombres cortos o ambiguos de canciones (ej. *"Change"*, *"Dracula"*); priorizar curador editorial, top 3–4 artistas y palabras clave del género.

---

## 4. Persistencia de Scroll y Carga Progresiva

- **Restauración de Posición de Scroll:**
  - En `HomeScreen` y pantallas con múltiples carruseles, persistir los `LazyListState` (vertical y horizontal) en `MainViewModel` indexados por `sectionKey` (`rememberLazyListState` atado al estado persistido). Esto evita que al volver de un `DetailScreen` la pantalla salte al inicio.
- **Carga Progresiva y Sincronización:**
  - Al abrir playlists de Apple Music o Spotify en `DetailScreen`, la UI debe mostrar inmediatamente la primera página de canciones y cargar el resto en segundo plano.
  - Al guardar una playlist externa en la biblioteca del usuario con el botón `+`, preservar siempre el `curatorName` original (ej. *"Apple Music Alternativa"*).
  - Al concluir la carga progresiva de canciones, sincronizar las pistas completas en el almacenamiento local y remoto mediante `syncSavedApplePlaylistTracks(...)`.
