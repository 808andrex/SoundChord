---
name: soundchord-animations
description: Presets exactos de física (spring damping/stiffness, duraciones tween, TransformOrigin de menús/popovers y uso estricto de graphicsLayer) inspirados en iOS/Apple Music y probados en Zenith para mantener 60/120 FPS sin recomposición.
---

# Guía de Animaciones y Física Fluida (Estilo iOS/Apple Music) en SoundChord

Esta guía documenta los presets técnicos exactos de curvas de animación, física de resortes (`spring`), transformaciones de origen (`TransformOrigin`) y el patrón obligatorio de `Modifier.graphicsLayer { ... }` para lograr una experiencia táctil ultra fluida (60/120 FPS estables) sin provocar recomposiciones ni relayouts innecesarios en Jetpack Compose.

---

## 1. Principio Fundamental: Rendimiento y Aislamiento de Capa (GraphicsLayer)

En Jetpack Compose, animar propiedades en la fase de composición (`Composition`) o diseño (`Layout`) fuerza a recomponer subárboles enteros o recalcular medidas de texto en cada frame:
- **Prohibido:** Animar `Modifier.offset(x, y)` o `Modifier.padding(...)` o `fontSize` cuadro a cuadro para transiciones de movimiento continuo.
- **Obligatorio:** Utilizar siempre lambdas de `Modifier.graphicsLayer { ... }` o `placeWithLayer`. Al leer los valores de animación (`Animatable.value`, `animateFloatAsState`, o lambdas `() -> Float`) dentro del bloque `graphicsLayer`, la modificación se ejecuta directamente en el `RenderNode` (hilo de renderizado de la GPU) sin recomponer el Composable.

```kotlin
// Correcto (GPU RenderNode - 60/120 FPS estables):
Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
    alpha = fade
    translationX = tx
    translationY = ty
    transformOrigin = TransformOrigin(originX, originY)
}
```

---

## 2. Presets Exactos de Física (`spring`)

| Componente / Escenario | Damping Ratio | Stiffness | Comportamiento / Sensación |
| :--- | :--- | :--- | :--- |
| **PlayerSheet (Expansión/Contracción Reproductor)** | `Spring.DampingRatioNoBouncy` (1.0f) | `150f` | Críticamente amortiguado, sin rebote. Movimiento suave y largo que permite al ojo seguir la carátula hasta su destino. |
| **Menús Flotantes (`FrostedMenuPanel`, Perfil)** | `0.72f` | `380f` | Pop-in rápido con micro-rebote orgánico característico de menús emergentes en iOS. |
| **Aparición Escalonada de Elementos Internos (`reveal`)** | `0.62f` | `420f` | Rebote sutil desfasado (110 ms tras la apertura del contenedor) para avatares, ticks o checks. |
| **Micro-Escala Táctil en Botones (`TransportGlyph`)** | `0.45f` | `Spring.StiffnessMedium` (1500f) | Respuesta instantánea al presionar (escala a `0.72f`) y descompresión con rebote reactivo al soltar. |
| **Kick Táctil de Saltos (Skip Next / Previous)** | `0.40f` | `Spring.StiffnessMediumLow` (800f) | Retorno elástico tras empujar el icono en la dirección del salto (`direction * 12.dp`). |
| **Popover de Salida de Audio (`SoundChordAudioOutputSheet`)** | `0.82f` (enter) / `1.0f` (exit) | `450f` (enter) / `550f` (exit) | Expansión enérgica desde la parte inferior y cierre firme y directo sin oscilaciones residuales. |
| **Diálogos Modales iOS (`AlertScaffold`, Confirmación)** | `0.76f` | `420f` | Expansión elástica limpia de escala `0.90f → 1.0f` al aparecer y contracción suave al cerrar. |
| **Cápsulas de Transporte / Pill (`Pill`)** | `0.82f` | `Spring.StiffnessMediumLow` | Cambio elástico de ancho al colapsar o expandir segmentos sin saltos visuales. |

---

## 3. Presets de Duración y Curvas (`tween` / Easing)

| Transición | Duración | Easing | Propósito |
| :--- | :--- | :--- | :--- |
| **Fade de Apertura Contenedor** | `160 ms` | `FastOutSlowInEasing` | Entrada rápida de la tarjeta/panel antes de que termine el resorte. |
| **Fade de Cierre Contenedor** | `160 ms` a `180 ms` | `FastOutLinearInEasing` | Salida limpia que previene artefactos visuales al desmontar. |
| **Escala de Cierre Contenedor** | `160 ms` a `200 ms` | `FastOutSlowInEasing` | Contracción fluida de `1.0f` a `0.90f` o `0.6f` hacia el origen. |
| **Transición Directa Cola ↔ Letras (`paneSwap`)** | `340 ms` | `FastOutSlowInEasing` | Crossfade con micro-elevación vertical (18dp) sin parpadeo de pantalla. |
| **Navegación Pila iOS (Settings Push)** | Entrada: `340 ms`<br>Salida: `340 ms` | `FastOutSlowInEasing` | Deslizamiento horizontal de ancho completo (`{ it }`) con fade suave. |
| **Aparición Escalonada de Metadatos (`enterReveal`)** | `420 ms` (retraso variable) | `FastOutSlowInEasing` | Título (delay: 150ms), Artista (200ms), Meta (240ms), Acciones cápsula (290ms), Pistas (340ms). |
| **Desplazamiento de Texto de Pista (`TrackTextSwitch`)** | `340 ms` (desplazamiento)<br>`260 ms` (fade in)<br>`160 ms` (fade out) | `FastOutSlowInEasing` | Deslizamiento direccional (izquierda en Siguiente, derecha en Anterior) con desvanecimiento simultáneo. |
| **Barrido Shimmer (`ShimmerBox`)** | `1400 ms` | `LinearEasing` (infinito) | Barrido horizontal limpio con gradiente continuo sin relayouts. |

---

## 4. Coordenadas de Origen de Transformación (`TransformOrigin`)

Para que las animaciones de escala parezcan nacer o guardarse físicamente en el botón que las activó:

- **Menús de Cabecera Superior Derecha (`FrostedMenuPanel`, Menús `•••` de página):**
  `TransformOrigin(1f, 0f)` — El panel emerge desde la esquina superior derecha hacia abajo y hacia la izquierda.
- **Selector de Perfil / Cuentas (`AccountProfileSelector`):**
  `TransformOrigin(0.92f, 0f)` — Coincide exactamente con el centro del avatar circular en la barra superior.
- **Popover Flotante Inferior de Salida de Audio (`SoundChordAudioOutputSheet`):**
  `TransformOrigin(0.5f, 1f)` — Brota desde el centro del borde inferior de la pantalla hacia arriba.
- **Textos de Cabecera en Transición Reproductor / Cola / Letras:**
  `TransformOrigin(0f, 0.5f)` — Escala del texto anclada al inicio horizontal para que el título no cambie de alineación al encogerse (`scale = 1f - 0.2f * p()`).

---

## 5. Implementación de Morfismo y Vuelo de Carátula (Mini-Player ↔ Reproductor)

Al abrir o cerrar la cola o las letras en `NowPlayingScreen`, la miniatura en cabecera debe converger de forma lineal y recta hacia el mini player sin trayectorias curvas ni sobrepasos:
```kotlin
fun towardsDock(rect: Rect, mini: Rect?): Rect {
    if (mini == null) return rect
    val unridden = dockFrame.offset * (1f - (dockFrame.ride * (1f - p())))
    val open = dockT()
    val ease = open + (dockEaseThumb(open) - open) * p()
    return lerp(mini, rect.translate(0f, -unridden), ease)
}
```
Con la función de suavizado `dockEaseThumb`:
```kotlin
private const val DOCK_THUMB_FULL_AT = 0.35f

private fun dockEaseThumb(t: Float): Float {
    val x = (t / DOCK_THUMB_FULL_AT).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
```

---

## 6. Transiciones Especiales (Cola↔Letras, Detalle Opaco, Subpantallas Ajustes, Diálogos iOS)

### A. Alternancia Directa Cola ↔ Letras (`paneSwap`):
Cuando el usuario cambia directamente entre Cola y Letras sin colapsar el reproductor, se evita el parpadeo abrupto animando `paneSwap` de 0f a 1f (`340 ms`):
```kotlin
// El panel que sale se desvanece y asciende sutilmente (-travel * 0.5f)
alpha = panelFade.value * (1f - swap)
translationY = -swap * travel * 0.5f

// El panel que entra asciende desde +travel hasta su posición
alpha = panelFade.value * swap
translationY = (1f - swap) * travel
```

### B. Mini-Cabecera y Textos con `basicMarquee()`:
En la mini-cabecera con Cola o Letras abiertas, el título conserva `Modifier.weight(1f)` y utiliza `Modifier.basicMarquee(iterations = Int.MAX_VALUE)` activo con `scrolls = { !queueDragging && !collapsePastSettling }`, evitando truncamiento por elipsis (`...`).

### C. Navegación Opaca de Detalles (`DetailScreen`):
Al abrir una playlist o álbum, la nueva pantalla entra de forma 100% opaca con su fondo teñido (`palette.background`):
```kotlin
(EnterTransition.None togetherWith fadeOut(tween(160))).apply {
    targetContentZIndex = 1f
}
```
Esto elimina el efecto fantasma translúcido de la pantalla anterior (*Find Your Mood*), mientras los elementos internos aterrizan en cascada fluida con `Modifier.enterReveal(...)`.

### D. Pila de Navegación Horizontal en Ajustes:
Las subpantallas de Ajustes entran con deslizamiento lateral iOS:
```kotlin
// Al avanzar (Push):
(slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(260))) togetherWith
    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200)))

// Al retroceder (Pop):
(slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(200))) togetherWith
    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(260)))
```

### E. Diálogos Modales Estilo iOS (`AlertScaffold`):
Todos los diálogos de alerta iOS (`AlertScaffold`, historial de radio, historial de reconocimiento) implementan escala `0.90f → 1.0f` con `spring(dampingRatio = 0.76f, stiffness = 420f)` y fundido del scrim de fondo, cerrándose con animación de contracción previa al callback `onDismiss()`.

---

## 7. Reglas de Arquitectura Modular y Prevención de Errores

Para garantizar compilaciones limpias en el entorno Kotlin Multiplatform de SoundChord:

### Acceso a `reduceAnimation` según el módulo:
- **En módulo `:app`:** Utilizar `com.music.bitchord.data.settings.AppSettings.reduceAnimation.collectAsStateWithLifecycle()`.
- **En módulo `:sharedUi` (KMP):** `:sharedUi` no depende de `:app` y no tiene visibilidad de `AppSettings`. Debe usarse exclusivamente:
  ```kotlin
  import com.music.bitchord.ui.player.PlayerSettings
  
  val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
  ```
  *(Definido internamente en `PlayerHost.kt` a través de `PlayerPlatform.host.settings.reduceAnimation`).*
  **Nunca** importar `com.music.bitchord.ui.theme.PlayerSettings` ni `AppSettings` en `:sharedUi`.

### Prevención de Declaraciones Duplicadas (`Conflicting declarations`):
- Los tokens compartidos de métricas y espaciado definidos a nivel de paquete en `Common.kt` (por ejemplo `val LIBRARY_GRID_SPACING = 12.dp` en el paquete `com.music.bitchord.ui.components`) tienen visibilidad en todos los archivos del mismo paquete.
- **Nunca** re-declarar propiedades con el mismo nombre a nivel superior en otros archivos del mismo paquete (`Skeletons.kt`, etc.), incluso si se marcan como `private`, ya que el compilador de Kotlin genera un error de conflicto de declaración en el mismo paquete.

### Prevención de Conflictos de Funciones Helper en Paquetes (`tween`):
- En el paquete `com.music.bitchord.ui.app`, existe una función helper interna `internal fun tween(durationMillis: Int)` en `FrostedMenus.kt` que toma únicamente un argumento.
- Cualquier archivo dentro de `com.music.bitchord.ui.app` (como `AppNavigationHost.kt`) que requiera parámetros adicionales como `easing` o `delayMillis` **debe importar explícitamente**:
  ```kotlin
  import androidx.compose.animation.core.tween
  import androidx.compose.animation.core.LinearOutSlowInEasing
  ```
  De lo contrario, Kotlin resuelve la llamada a la función helper del mismo paquete y falla con `No parameter with name 'easing' found`.

---

## 8. Checklist de Implementación en Nuevas Pantallas

- [ ] ¿Las animaciones táctiles usan `Modifier.graphicsLayer` en lugar de recomposición de layouts?
- [ ] ¿El origen `TransformOrigin` coincide con el punto focal o botón activador?
- [ ] ¿Se respeta la preferencia del usuario `reduceAnimation` usando `PlayerSettings` en `:sharedUi` o `AppSettings` en `:app`?
- [ ] ¿Las listas o tarjetas esqueléticas (`ShimmerBox`) ejecutan su gradiente dentro de `drawWithCache` sin recomponer en cada frame?
- [ ] ¿Se mantuvieron intactas las funciones y estilos propios de SoundChord (Liquid Glass, filas de 4 botones, badges, opacidades)?
- [ ] ¿No se duplicaron constantes de layout o espaciado ya provistas por `Common.kt` u otros archivos del paquete?
- [ ] ¿Se importó explícitamente `androidx.compose.animation.core.tween` en archivos del paquete `com.music.bitchord.ui.app` al usar parámetros de curva/easing?
- [ ] ¿Los diálogos modales estilo iOS escalan de `0.90f → 1.0f` con resorte y fundido atenuado?
- [ ] ¿Las transiciones entre páginas de detalle son opacas sin superposiciones fantasma de la pantalla de inicio?
- [ ] ¿Se utiliza `Modifier.coverSharedElement` con clave unificada `"artwork_${id}"` y `rememberCoverTap` para vuelo fluido de carátulas?
- [ ] ¿Las listas de canciones y cuadrículas 'Ver todo' implementan `Modifier.staggeredEntrance` para cascada fluida en la carga?

---

## 9. Elementos Compartidos (`SharedTransitionLayout`) y Entrada en Cascada (*Staggered Cascade*)

### A. Arquitectura de Transición Compartida Desacoplada:
Para evitar inflar `MainActivity.kt` (manteniendo el límite de 64 KB JVM) y sin romper firmas entre módulos, `AppNavigationHost.kt` envuelve su navegación en `SharedTransitionLayout` y expone los scopes de Compose 1.7+ mediante `CompositionLocal`:
- `LocalCoverTransitionScope`: `SharedTransitionScope?` ambient provisto por el host de navegación.
- `LocalCoverVisibilityScope`: `AnimatedVisibilityScope?` ambient provisto por `AnimatedContent`.

```kotlin
// AppNavigationHost.kt
SharedTransitionLayout {
    CompositionLocalProvider(
        LocalCoverTransitionScope provides this@SharedTransitionLayout,
    ) {
        AnimatedContent(...) {
            CompositionLocalProvider(
                LocalCoverVisibilityScope provides this@AnimatedContent,
            ) {
                // Pantalla de destino (DetailScreen, Home, Library, etc.)
            }
        }
    }
}
```

### B. Claves Unificadas `"artwork_${id}"` y Prevención de Colisiones (`token`):
Cuando una misma carátula aparece en varios carruseles o cuadrículas a la vez, se previene que colisionen o vuelen desde la tarjeta equivocada asociando un token único por tarjeta:
- **En la tarjeta de origen (`ShelfCard`, `VideoShelfCard`, `ExploreShelfCard`, `TopReleaseCard`, `SectionCard`, `BrowseRow`):**
  ```kotlin
  val cardToken = remember { Any() }
  val openCard = rememberCoverTap(item.browseId, cardToken, onClick)

  // En el contenedor de la imagen:
  Box(
      modifier = Modifier
          .clip(shape)
          .coverSharedElement(item.browseId, shape, cardToken)
          .thumbnailBorder(shape)
  )
  ```
- **En la cabecera de destino (`DetailScreen` / `ArtistHero`):**
  No pasa token (`token = null`), activándose siempre sobre la carátula o héroe de la pantalla abierta:
  ```kotlin
  Box(
      modifier = Modifier
          .coverSharedElement(page.browseId, SLEEVE_SHAPE)
  )
  ```
- **Física de vuelo de carátula:** Curva `CoverEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)` de 560 ms: despegue rápido con aterrizaje largo y suave sin rebote.

### C. Física de Cascada Escalonada (*Staggered Entrance*):
Tanto las filas de canciones en `DetailScreen` como los elementos de cuadrícula en "Ver todo" (`LibraryGridPage` / `LibraryShowAll`) usan `Modifier.staggeredEntrance(index)`:
```kotlin
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 40,
    stepMs: Int = 28,
    rise: Dp = 14.dp,
): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayTime = (baseDelayMs + (index.coerceAtMost(24) * stepMs)).toLong()
        if (delayTime > 0) delay(delayTime)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 380f,
            ),
        )
    }
    return this.graphicsLayer {
        val p = progress.value
        if (p >= 1f) return@graphicsLayer
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * rise.toPx()
    }
}
```
- **Física:** `dampingRatio = 0.82f`, `stiffness = 380f`.
- **Desfase:** `40ms + index * 28ms` (limitado a los primeros 24 elementos para evitar demoras innecesarias al scrollear).
- **Rendimiento:** Al llegar `progress` a `1f`, `graphicsLayer` retorna de inmediato, garantizando cero overhead de recomposición o GPU una vez asentado.

---

## 10. Transición de Banners Anchos, Navegación Inversa (Pop) y Radio / Mixes

### A. Banners Anchos de Artistas y Suscripciones (Full-Bleed):
Al abrir un artista o suscripción desde una tarjeta cuadrada en `LibraryScreen`, `HomeScreen` o `ExploreScreen` hacia el banner panorámico en `DetailScreen`:
- **Interpolación Libre sin Recorte Forzado:** Se omite `clipInOverlayDuringTransition = OverlayClip(...)` en `CoverTransition.kt`, permitiendo que Compose interpole suavemente los límites entre el cuadrado redondeado y el rectángulo ancho *full-bleed* sin saltos bruscos ni cortes angulares a mitad de trayectoria.
- **Fondo Limpio sin Tintes Grises:** El fondo del `AsyncImage` en `PageBackground` se establece en `Color.Transparent` (en lugar de `palette.elevated`), garantizando que la imagen fluya limpia sin parpadeos opacos o tonos grises pesados antes de renderizarse.

### B. Animación Inversa al Regresar Atrás (Pop en Navigation Host) y Desvanecimiento Rápido Secundario:
Al salir de una página de detalle (`AppDestination.Release`) hacia la pantalla anterior o regresar de un Álbum a su Artista:
```kotlin
val poppingRelease = initial is AppDestination.Release &&
    (target !is AppDestination.Release || !states.detailStack.any { it.browseId == initial.browseId })
val pushingRelease = target is AppDestination.Release && !poppingRelease

if (pushingRelease) {
    (EnterTransition.None togetherWith fadeOut(tween(160))).apply {
        targetContentZIndex = 1f
    }
} else if (poppingRelease) {
    (fadeIn(tween(280)) togetherWith fadeOut(tween(560, easing = FastOutLinearInEasing))).apply {
        targetContentZIndex = 0f
    }
}
```
- **Prioridad de Capa (`targetContentZIndex = 0f`):** Mantiene la página que sale en el nivel visual superior durante la transición de retorno, evitando que la pestaña que regresa la tape abruptamente.
- **Duración Sincronizada (560 ms):** El `fadeOut` de la página contenedora coincide exactamente con la duración de `CoverEasing` (560 ms), logrando que la carátula o banner vuele de regreso limpiamente hacia su tarjeta de origen antes de que la pantalla se desmonte.
- **Desvanecimiento Rápido del Contenido Secundario (~100 ms con `secondaryExitFade`):**
  Para evitar que títulos, botones cápsula ("Play", "Aleatorio"), descripción y listas de canciones queden flotando sobre la pantalla anterior mientras la carátula vuela, todo el contenido secundario implementa `Modifier.secondaryExitFade(100)` (o `enterReveal` / `staggeredEntrance` con salida de 100ms). Esto desvanece de inmediato el texto y controles secundarios al iniciar el pop, dejando la vista anterior limpia y despejada mientras la portada aterriza con precisión.

### C. Transiciones en Radio y Mixes Personalizados con Carátula Inicial Precargada:
- **Paso de Carátula Inicial Inmediata:** Al abrir mixes desde `RadioScreen` (`QuickMixCard` / `MixCarouselCard`), se pasa la URL de imagen ya cargada (`mix.thumbnailUrl`) directamente a `viewModel.openDetail(browseId, title, "YouTube Music mix", thumbnailUrl, BrowseType.PLAYLIST)`. Esto garantiza que `DetailPage` tenga la portada precargada en memoria desde el primer fotograma, eliminando el placeholder o cuadro gris vacío durante el vuelo.
- **Tus Mixes y Mixes Personalizados:** Usan la clave `item.browseId ?: item.title` conectada a `coverSharedElement` y disparada mediante `rememberCoverTap`, volando directamente a la cabecera de la playlist generada en `DetailScreen`.
- **Estación Personalizada (`PersonalStationCard` ↔ `PersonalStationHero`):** Comparten la clave `"station:${seed.videoId}"` con `RoundedCornerShape(28.dp)` en la tarjeta y `RoundedCornerShape(18.dp)` en el héroe de la estación.
- **Micro-Escala Táctil en Géneros y Países (`CategoryGridRow`):** Escala a `0.96f` al presionar con `spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)`.
- **Reordenamiento y Carga de Estaciones (`StationRow`):** Integran `Modifier.animateItem()` dentro de `itemsIndexed` en `LazyColumn` para una animación elástica al buscar o filtrar emisoras.


