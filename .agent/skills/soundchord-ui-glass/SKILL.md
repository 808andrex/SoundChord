---
name: soundchord-ui-glass
description: Estética Apple Music/iOS, modo Liquid Glass, menús esmerilados, alertas estilo iOS, reproductor NowPlayingScreen, cabeceras DetailScreen, contraste de controles, navegación inferior, paleta de Radio y badges NUEVO en Ajustes. Usar al editar pantallas Compose, modales, temas o SettingsSheet.
---

# Guía de Diseño UI/UX, Liquid Glass y Estética iOS/Apple Music en SoundChord

Reglas y patrones visuales obligatorios para el desarrollo y diseño de cualquier pantalla, modal, menú contextual, alerta o acción dentro de SoundChord.

---

## 1. Menús Flotantes Esmerilados (`FrostedMenus.kt` / `IosMenu.kt`)
Todo menú contextual desplegable (menús de ordenamiento, opciones de playlist/álbum `BrowseActionsMenu`, opciones de canción `SongActionsMenu`, emisoras `StationActionsMenu`, etc.) debe seguir el patrón de popover esmerilado flotante:
- **Contenedor y Superficie:** Usar `FrostedMenuPanel` o `IosMenuPopover` con efecto blur/haze (`HazeMaterials.thin` o `optimizedHazeEffect`).
- **Geometría y Bordes:** Esquinas fuertemente redondeadas continuas (`RoundedCornerShape(24.dp)` a `30.dp`), con borde sutil translúcido de `0.5.dp` (`Color.White.copy(alpha = 0.14f)` en oscuro o `colors.rim`).
- **Distribución de Filas:** Altura mínima generosa (`minHeight = 44.dp`), espaciado horizontal de `12.dp` a `20.dp` y tipografía legible (`bodyLarge`).
- **Alineación de Iconos:**
  - En menús de acciones: icono descriptivo a la izquierda alineado limpiamente con el texto.
  - En menús de ordenamiento/selección: tick de confirmación (`✓`, `Icons.Rounded.Check`) en el extremo derecho.
- **Acento Rojo de Marca:** Los elementos activos/seleccionados y las acciones destructivas (como *Eliminar playlist* o desvincular) usan el color rojo acento de la app (`MaterialTheme.colorScheme.primary` o tono destructivo).

---

## 2. Diálogos de Confirmación y Avisos Estilo iOS (`AccountAlerts.kt` / `GlobalSheetsHost.kt`)
Cualquier alerta informativa o de confirmación (ej. *Portada guardada localmente*, confirmaciones de eliminación, *Borrar historial* o avisos de sesión) debe prescindir de los cuadros de diálogo genéricos de Material 3 y utilizar el patrón `AlertScaffold` (`InfoAlert` / `ConfirmationAlert`):
- **Tarjeta Modal Centrada:** Tarjeta oscura flotante en el centro de la pantalla con fondo esmerilado/haze o superficie sólida oscura atenuada si el blur dinámico está reducido.
- **Bordes y Dimensiones:** Ancho fijo estilizado (`ALERT_WIDTH = 270.dp`) con esquinas redondeadas continuas (`ALERT_CORNER = 14.dp` a `20.dp`).
- **Jerarquía Tipográfica:**
  - **Título:** Centrado, negrita de alto contraste (17sp, `FontWeight.W600`, `TextAlign.Center`).
  - **Cuerpo:** Centrado, tamaño secundario legible (13sp, interlineado de 17sp, color atenuado).
- **Líneas Divisoras Hairline:** Separador horizontal fino (`AlertRule()`, grosor `0.5.dp`, color translúcido) entre el texto descriptivo y los botones de acción.
- **Botón de Acción Inferior a Ancho Completo:** Botón apilado a todo lo ancho (`AlertAction`), con tipografía centrada en rojo acento (`MaterialTheme.colorScheme.primary`) para acciones principales (*Aceptar*, *Eliminar*, *Guardar*), y botón secundario (*Cancelar*) debajo separado por otra línea hairline.

---

## 3. Paneles / Sheets con Desenfoque y Búsqueda Integrada (`AddSongsDialog.kt` / `PlaylistPickerSheet.kt`)
Los modales para añadir canciones a listas de reproducción o a la cola deben componerse como paneles flotantes glaseados:
- **Fondo Translúcido Teñido:** Superficie con efecto vidrio esmerilado que toma un tinte suave del fondo o carátula actual mediante `iosMenuPanel` o `Haze`.
- **Cabecera Funcional:**
  - Título principal en negrita a la izquierda (*Añadir canciones*).
  - Acción de cierre textual a la derecha en rojo acento (*Listo* / `TextButton` primario).
- **Buscador Tipo Píldora:** Campo de texto integrado tipo cápsula redondeada (`PillTextField` o `BasicTextField` con `CircleShape` o `RoundedCornerShape(16.dp)`):
  - Icono de lupa a la izquierda.
  - Placeholder estándar: *"Artistas, canciones, letras y más"*.
  - Botón de limpieza `X` a la derecha cuando hay texto.
- **Filas de Pistas con Botón Circular:** Cada pista muestra su carátula cuadrada redondeada, título, artista y un botón circular flotante con el icono `+` a la derecha para agregar inmediatamente sin abandonar la búsqueda.

---

## 4. Cabeceras de Detalle, Búsqueda Interna y Controles Cápsula (`DetailScreen.kt`)
En pantallas de colección (Playlists, Álbumes, Estaciones de Artista):
- **Botón Atrás Esmerilado:** Botón circular translúcido (`<`, `CircleIconButton`) ubicado en la esquina superior izquierda sobre la carátula o degradado.
- **Cápsula de Acciones Agrupada:** En la barra superior derecha, agrupar las acciones en una cápsula flotante o fila de botones discretos:
  - Búsqueda / Cerrar búsqueda (`Search` / `Close`).
  - Ordenar canciones (`Sort`).
  - Menú de más opciones (`•••`, `MoreHoriz`).
- **Buscador Interno de Lista:** Al activar la búsqueda, desplegar `DetailSearchField` justo bajo la cabecera: contenedor redondeado con borde tenue y texto *"Buscar en esta lista"*.
- **Fila Principal de Reproducción:**
  - **Botón Central Ancho:** Cápsula principal `ActionPill` (*▶ Reproducir*) en blanco/alto contraste.
  - **Botones Laterales Flanqueantes:** Dos botones circulares de `50.dp` (`RELEASE_SIDE_BUTTON`):
    - A la izquierda: Botón circular de *Aleatorio / Shuffle*.
    - A la derecha: Botón circular de *Añadir a biblioteca / Guardar* (`+` / `✓`).

---

## 5. Tipografía Editorial Apple Music y Separadores de Lista
- **Encabezados Editoriales:** Títulos de pantalla y secciones principales (*Estación de...*, *Canciones recomendadas*, *Recientes*, *Find Your Mood*) deben emplear tipografía grande en negrita con contraste marcado sobre fondo oscuro.
- **Separadores Hairline en Listas:**
  - Las listas verticales de pistas (como el bloque *Recientes* en Inicio o las listas de canciones en álbumes y playlists) se separan mediante líneas horizontales sutiles (`HorizontalDivider` de `0.5.dp`, color `outline` o `palette.divider`).
  - **Alineación con el texto:** El separador debe tener un `padding(start = 60.dp)` (o `97.dp` en miniaturas de video) para iniciar exactamente donde comienza el texto del título de la pista, dejando despejado el icono/arte de la carátula.
  - **Prohibido encajonar en tarjetas:** Nunca encerrar cada canción de una lista vertical en tarjetas individuales independientes con elevación o bordes completos.

---

## 6. Reproductor NowPlaying (`NowPlayingScreen.kt` / `PlayerFrost.kt`)
- **Contraste de Botones Circulares:** Los botones circulares de Favorito (`★`) y Más opciones (`•••`) deben mantener un fondo esmerilado con opacidad/tinte suficiente para destacar con nitidez sobre portadas claras o Canvas animados.
- **5 Controles de Transporte Opcionales:** Si `showPlayerShuffleRepeatButtons` está activo, mostrar 5 controles simétricos en la fila de transporte (Aleatorio a la izquierda, 3 controles centrales, Repetir a la derecha) respetando `controlsLocked` en sesiones de *Escuchar juntos*.

---

## 7. Navegación Inferior (`GlassNavBar.kt` / `FloatingBottomBar.kt` / `AppNavigationHost.kt`)
- **Cambio Inmediato de Pestaña:** Tocar una pestaña distinta a la activa cambia inmediatamente de destino sin quedar retenido por *scroll to top*.
- **Scroll to Top Condicional:** `animateScrollToItem(0)` solo debe dispararse al tocar la pestaña que **ya estaba activa**.
- **Superposiciones:** Nunca dejar pantallas superpuestas al cambiar de pestaña, tanto con Liquid Glass activado como desactivado.

---

## 8. Pestaña Radio (`:soundchord:features`)
- **Paleta de Colores:** Las tarjetas de Géneros y Países deben usar degradados suaves y desaturados armónicos con las tarjetas superiores de Mixes, evitando colores neón estridentes.

---

## 9. Badges Dinámicos `NUEVO` en Ajustes (`AppSettings.kt` / `SettingsSheet.kt`)
- **Ciclo de Vida:** Toda opción nueva en Ajustes registra su constante `FEATURE_<NOMBRE>` en `AppSettings.kt`, conecta `R.string.settings_badge_new` en su categoría y fila concreta, y llama a `AppSettings.markFeatureSeen(FEATURE_<NOMBRE>)` al interactuar con ella (nunca al abrir Ajustes).
- **Una Sola Línea:** Renderizar el badge siempre en una sola línea horizontal (`maxLines = 1`, `softWrap = false`).
