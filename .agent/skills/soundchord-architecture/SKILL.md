---
name: soundchord-architecture
description: Arquitectura modular real de SoundChord (:app, :shared, :sharedUi, :soundchord:features), desacoplamiento de UI en Compose (límite JVM de 64 KB en MainActivity) y convenciones para crear features.
---

# Arquitectura Modular y Desacoplamiento de UI en SoundChord

Guía técnica obligatoria sobre la arquitectura modular del repositorio, el patrón de estado para evitar `MethodTooLargeException` en Jetpack Compose y las convenciones para incorporar nuevas funciones sin romper la estabilidad del código.

---

## 1. Mapa Exacto de Módulos y Responsabilidades

SoundChord utiliza una arquitectura desacoplada estructurada en cuatro módulos principales:

```
┌────────────────────────────────────────────────────────┐
│                          :app                          │
│ (Application, MainActivity, Services, Player, Widgets) │
└───────┬────────────────────────┬───────────────┬───────┘
        │                        │               │
        ▼                        ▼               │
┌───────────────────────┐ ┌─────────────┐        │
│ :soundchord:features  │ │  :sharedUi  │        │
│ (Radio, Recognition)  │ │ (Themes, UI)│        │
└───────┬───────────────┘ └──────┬──────┘        │
        │                        │               │
        ▼                        ▼               ▼
┌────────────────────────────────────────────────────────┐
│                        :shared                         │
│       (commonMain: Modelos | jvmSharedMain: APIs)      │
└────────────────────────────────────────────────────────┘
```

### `:shared` (Lógica de Negocio y Modelos Multiplataforma)
Módulo Kotlin Multiplatform (KMP) con las reglas de negocio, parsers y persistencia agnóstica:
- **`src/commonMain/kotlin`**: Código Kotlin puro sin APIs de Android ni dependencias JVM específicas.
  - Modelos globales de dominio: `Song`, `BrowseType`, `HomeShelf`, `SearchResult`, `ServerConnectionState`.
  - Interfaces de reproducción inteligente (`smart/`) y configuraciones de usuario (`AppSettings`).
- **`src/jvmSharedMain/kotlin`**: Implementaciones compartidas que requieren JVM/Ktor/Criptografía.
  - Parsers de YouTube/InnerTube (`InnertubeParser`, `poToken`).
  - Integraciones externas: Spotify API, Discord RPC (`Kizzy`), proveedores de letras (LRCLIB, Genius) y WebDAV.

### `:sharedUi` (Componentes Visuales y Recursos Multiplataforma)
Módulo Compose Multiplatform enfocado exclusivamente en la capa de presentación reutilizable:
- **Temas y Estilos**: `SoundChordTheme`, tokens de diseño, paletas dinámicas (`ArtworkPalette`).
- **Componentes base**: Sliders, barras de progreso, badges, haptics bridges y efectos visuales/festivos.
- **Recursos Localizados**: Cadenas (`composeResources/values*`), tipografías e iconografía común.
- *Regla*: No debe depender de `:app` ni de `:soundchord:features`.

### `:soundchord:features` (Módulo Aislado de Características Propias)
Librería Android (`com.android.library`) exclusiva de SoundChord para mantener el código base original de BitChord limpio y facilitar merges upstream:
- **Radio en Vivo (`com.soundchord.features.radio`)**:
  - API cliente RadioBrowser, `RadioRepository`.
  - Persistencia local: `PreferencesRadioFavoritesStore`, `RadioHistoryStore`.
  - UI: `RadioScreen.kt`, `RadioFavoritesScreen.kt`, `RadioHistoryScreen.kt`, `RadioViewModel.kt`.
- **Reconocimiento Musical / Shazam (`com.soundchord.features.recognition`)**:
  - Captura y buffer de micrófono: `AudioRecord`, `VibraState`.
  - Algoritmo de huella y API Shazam: `MusicRecognitionService`, `ShazamApiService`.
  - UI: `RecognitionScreen.kt`, `RecognitionFab.kt`, `RecognitionHistoryScreen.kt`, `RecognitionViewModel.kt`.
- **Salidas de Audio y Escucha Conjunta**: `RemoteAudioReceiver.kt`, `SoundChordAudioOutputSelector.kt`.
- **Recursos propios**: `features/src/main/res/values/strings.xml` y `values-es/strings.xml`, `ic_soundchord_logo.xml`.

### `:app` (Orquestador de Aplicación Android)
Módulo raíz de Android donde convergen todos los módulos:
- Android Manifest, ciclo de vida del sistema, Services (`PlaybackService`, MediaSession, Audio Engine Oboe/ExoPlayer, `CrossfadeController`).
- Widgets de pantalla de inicio (`widget/`).
- Integración con WearOS (`wear/`).
- ViewModels principales de pantalla (`MainViewModel`).
- Orquestación de UI y navegación en Compose (`MainActivity.kt` y sub-hosts en `ui/app/` y `ui/player/`).

---

## 2. Patrón de Estado y Separación de UI en Compose (Límite JVM de 64 KB)

### El Problema de `MethodTooLargeException`
En la JVM, el bytecode de un método compilado no puede exceder los 64 KB. En Jetpack Compose, las funciones `@Composable` con docenas de variables `remember`, `collectAsState`, lambdas anidadas o bloques gigantescos de UI generan métodos sintéticos (`$1`, etc.) que sobrepasan con facilidad este límite (`GenericSignatureFormatError` o `MethodTooLargeException`).

Por este motivo, **`MainActivity.kt` debe mantenerse por debajo de las 950 líneas** y no puede alojar árboles directos de UI ni estados sueltos.

### Arquitectura de Delegación en Hosts Especializados

```
MainActivity.kt
 ├── BitChordApp (Entry point, tema, insets)
 │    ├── BitChordAppEffects (Efectos LaunchedEffect y sincronización reactiva)
 │    └── BitChordAppContent (Estructura visual principal)
 │         ├── BitChordBackHandlers (Gestión de botón atrás para navegación y hojas)
 │         ├── AppNavigationHost (Routing de pantallas y tabs principales)
 │         ├── BitChordChrome (TopBar, FloatingTabBar, menús glaseados/frosted)
 │         ├── BitChordPlayerUi (Miniplayer inferior y Player expandido)
 │         └── BitChordGlobalSheetsUi (Modales globales, menús de canciones, diálogos)
```

### Holders `@Immutable` (`BitChordAppHolders.kt`)
Para evitar recomposiciones masivas y simplificar firmas kilométricas:
1. **`BitChordNavState`**: Estado inmutable de rutas, tabs (`TAB_HOME`, `TAB_RADIO`, etc.), historial y vistas detalladas.
2. **`BitChordPlayerUiState`**: Estado inmutable del reproductor, carátulas, letras, canvas y playback.
3. **`BitChordSheetsUiState`**: Flags y datos de diálogos y sheets abiertos.
4. **`BitChordAppState`**: Contenedor maestro que agrupa `nav`, `player` y `sheets`.
5. **`BitChordAppActions`**: Contenedor inmutable de callbacks y lambdas de acción.

### Flujo de Conexión de Nuevas Pantallas y Modales

#### A. Conectar una nueva pantalla (Ruta de Navegación):
1. **Declarar la ruta en `AppNavigator.kt`**:
   ```kotlin
   sealed interface AppRoute {
       // Si no recibe parámetros:
       data object MiNuevaFeature : AppRoute
       // Si recibe parámetros:
       data class MiNuevaFeature(val id: String) : AppRoute
   }
   ```
2. **Exponer el estado o acción en `AppNavigationHost.kt`**:
   - Si la pantalla requiere comunicarse con el host, añade las propiedades necesarias a `AppNavigationScreenStates` y `AppNavigationScreenActions` (construidas con `rememberAppNavigationScreenStates` y `rememberAppNavigationScreenActions`).
   - Si el estado es autocontenido de la pantalla, instáncialo dentro de la pantalla vía `viewModel()`.
3. **Mapear la ruta dentro de `AppNavigationHost`**:
   ```kotlin
   // En AppNavigationHost.kt dentro de AnimatedContent -> pageContent
   } else if (pageRoute == AppRoute.MiNuevaFeature) {
       MiNuevaFeatureScreen(
           // Pasar dependencias o lambdas desde actions
           onBack = { navigator.pop() }
       )
   }
   ```
4. **Disparar la navegación**:
   ```kotlin
   navigator.push(AppRoute.MiNuevaFeature)
   ```

#### B. Conectar un nuevo Modal o BottomSheet:
- No lo agregues en `MainActivity.kt`.
- Ubícalo en `GlobalSheetsHost.kt` bajo `BitChordGlobalSheetsUi` o en su propia función delegada.
- Agrega el flag de visibilidad al contenedor de estado en `BitChordAppHolders.kt` (`BitChordSheetsUiState` / `BitChordSheetStateHolder`).

---

## 3. Convenciones de Nombres, Capas y Checklists al Crear una Feature

### Dónde Ubicar los Archivos

| Tipo de Feature | Ubicación Obligatoria | Paquete Base |
| :--- | :--- | :--- |
| **Exclusiva de SoundChord** (Radio, IA/Audio, Reconocimiento, Herramientas) | `:soundchord:features` | `com.soundchord.features.<nombre>` |
| **Pantalla estándar o flujo core de BitChord** | `:app` | `com.music.bitchord.ui.screens.<nombre>` |
| **Modelo de datos o servicio de red KMP** | `:shared` (`commonMain` o `jvmSharedMain`) | `com.music.bitchord.data.<nombre>` |
| **Widget UI o tema reutilizable** | `:sharedUi` | `com.music.bitchord.ui.components` |

### Estructura Interna dentro de `:soundchord:features`
Para cada feature `<feature_name>`:
```
soundchord/features/src/main/java/com/soundchord/features/<feature_name>/
 ├── data/
 │    ├── <Feature>Item.kt            // Modelos de datos / entidades
 │    ├── <Feature>ApiClient.kt       // Clientes Ktor / APIs HTTP
 │    ├── <Feature>Repository.kt      // Fuente única de verdad
 │    └── Preferences<Feature>Store.kt // Persistencia DataStore / SharedPreferences
 ├── ui/
 │    ├── <Feature>Screen.kt          // Composable principal de la pantalla
 │    ├── <Feature>Components.kt      // Sub-componentes visuales reutilizables
 │    └── <Feature>ViewModel.kt       // ViewModel con StateFlow<UiState>
 └── model/ (Opcional si hay DTOs específicos)
```

### Convenciones de Nombres
- **Pantalla Composable:** `<Feature>Screen.kt` (ej. `RadioScreen.kt`, `RecognitionHistoryScreen.kt`).
- **Diálogos / Sheets:** `<Feature><Propósito>Dialog.kt` o `*Sheet.kt` (ej. `PersonalStationDialog.kt`).
- **ViewModel:** `<Feature>ViewModel.kt` (heredando de `androidx.lifecycle.ViewModel`).
- **Almacenamiento Local:** `<Prefijo><Feature>Store.kt` (ej. `PreferencesRadioFavoritesStore.kt`, `RecognitionHistoryStore.kt`).
- **Repositorio:** `<Feature>Repository.kt`.
- **Ruta:** `AppRoute.<Feature>` en PascalCase.

---

## Checklist de Verificación para Nuevas Features

Antes de dar por finalizada la creación de una feature, verificar cada punto:

- [ ] **Módulo correcto:** ¿Se colocó la lógica exclusiva en `:soundchord:features` para proteger el núcleo del proyecto y mantener limpios los merges upstream?
- [ ] **Localización doble obligatoria:** ¿Todas las cadenas visibles están añadidas a la vez en `strings.xml` (inglés) y `values-es/strings.xml` (español) en el módulo donde reside la UI?
- [ ] **Ruta en AppNavigator:** ¿La nueva pantalla hereda de `AppRoute` en `AppNavigator.kt`?
- [ ] **Desacoplamiento de UI en AppNavigationHost:** ¿La pantalla se ensambla dentro de `AppNavigationHost.kt` (o `GlobalSheetsHost.kt` para modales) y **NO** dentro de `MainActivity.kt`?
- [ ] **Protección del límite JVM de 64 KB:** ¿`MainActivity.kt` permanece por debajo de 950 líneas sin bloques extensos de UI ni estados sueltos añadidos a `BitChordApp`?
- [ ] **Inmutabilidad en Holders:** Si se transmiten estados entre hosts, ¿están empaquetados en clases `@Immutable` en `BitChordAppHolders.kt`?
- [ ] **Compilación:** Nunca ejecutar `./gradlew` desde scripts de terminal; el usuario compila directamente desde Android Studio.
