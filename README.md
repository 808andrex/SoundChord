<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="SoundChord app icon" width="200" />

# SoundChord

### Aesthetic & Modular YouTube Music Client

<br/>

[![Latest release](https://img.shields.io/github/v/release/808andrex/SoundChord?style=for-the-badge&labelColor=0d1117)](https://github.com/808andrex/SoundChord/releases)
[![License](https://img.shields.io/github/license/808andrex/SoundChord?style=for-the-badge&labelColor=0d1117)](https://github.com/808andrex/SoundChord/blob/main/LICENSE)
[![Downloads](https://img.shields.io/github/downloads/808andrex/SoundChord/total?style=for-the-badge&labelColor=0d1117)](https://github.com/808andrex/SoundChord/releases)
[![Platform](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://github.com/808andrex/SoundChord/releases)

<br/>

[**Descargar APK**](#download) · [**Características**](#features) · [**Novedades v1.3.0**](#whats-new) · [**Arquitectura**](#architecture) · [**Créditos**](#contributing) · [**Privacidad**](#privacy) · [**Aviso Legal**](#disclaimer)

</div>

> [!IMPORTANT]
> SoundChord es una aplicación independiente de código abierto y no está afiliada, respaldada ni vinculada oficialmente con YouTube o Google.

---

<div align="center">

<h1><a id="features"></a>Características Destacadas</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### Reproducción y Audio
- **Streaming de alta fidelidad** — Búsqueda y reproducción desde YouTube Music con soporte para formatos sin pérdidas (FLAC / Lossless / DSD) en módulos compatibles.
- **Radio Global & Estaciones Personalizadas** — Emisoras organizadas por género y país con degradados vibrantes estilo Apple Music, rotación automática de servidores espejo, estaciones personalizadas, menús contextuales *Liquid Glass* e historial reciente.
- **AutoMix & Reproducción sin pausas (*Gapless*)** — Mezclas inteligentes emparejadas por tempo (BPM) y tonalidad con barra de progreso animada, fundido cruzado en siguiente/anterior y pausa automática en álbumes para conservar transiciones originales.
- **Reconocimiento musical (Shazam) + Historial** — Identificación de canciones en tiempo real mediante el micrófono con historial local persistente, buscador instantáneo y reproducción directa.
- **Sesiones Escuchar Juntos & Copias en Google Drive** — Control remoto en tiempo real entre dispositivos y respaldo opcional de tu historial *Replay*, listas y ajustes en Google Drive.
- **Reproducción local y descargas** — Guarda pistas con metadatos embebidos e integra la música almacenada en tu dispositivo.

    </td>
    <td width="50%" valign="top">

#### Experiencia e Interfaz
- **Onboarding Interactivo & Color de Acento Global** — Flujo inicial de 4 pasos con selección inteligente de artistas, vista previa en vivo de tema/cristal, paleta de colores de acento dinámicos e iconos alternativos (*Monocromo*, *Rojo clásico*, *Verde Spotify*).
- **Diseño "Liquid Glass" & Animaciones estilo iOS** — Tarjetas flotantes translúcidas adaptativas a Tema Claro y Oscuro, transiciones compartidas fluidas en carátulas (AGSL), física de resorte (*spring*), portadas animadas y Spotify Canvas.
- **Widgets de Pantalla de Inicio & Video Fullscreen** — Widgets rediseñados al estilo Apple Music / iOS y controles minimalistas para reproducción de video a pantalla completa.
- **Gestión Rápida de Cola y Playlists** — Creación y edición de playlists con selector de portada personalizado y sincronización con YouTube Music, selección múltiple y gestos al deslizar en la cola, e insignia circular de Aleatorio/Autoplay.
- **Letras sincronizadas palabra por palabra** — Animación tipo Apple Music con soporte multi-proveedor (Musixmatch, LRCLIB, BiniLyrics, Genius).
- **Actualizador OTA & Novedades Integradas** — Detección automática desde GitHub Releases con tarjeta flotante de novedades, historial por categorías e instalación directa del APK.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="whats-new"></a>Novedades en v1.3.0</h1>

</div>

- **Nueva Pestaña Radio con Degradados Vibrantes y Menús *Liquid Glass*:** Exploración de emisoras por género y país con tarjetas inspiradas en Apple Music, conservación de desplazamiento, estaciones personalizadas con separadores limpios y menú contextual flotante con respuesta háptica al mantener presionado.
- **AutoMix Inteligente & Reproducción sin Pausas en Álbumes:** Motor de mezcla automática con análisis de tono/tempo y barra animada (*Mezclando*), modo *gapless* para escuchar álbumes completos sin cortes ni fundidos artificiales, y restauración del fundido cruzado manual al saltar pistas o elegir canciones en listas.
- **Nuevos Widgets estilo Apple Music / iOS & Video a Pantalla Completa:** Widgets de inicio completamente rediseñados y nuevos controles de video en pantalla completa con estética minimalista.
- **Creación y Edición Avanzada de Playlists & Controles Rápidos:** Nueva vista *Nueva lista* con selector de portada y sincronización con YouTube Music, opción *Editar playlist* (título y carátula), botones opcionales de Aleatorio y Repetir en la barra del reproductor e insignia circular de estado en el botón de Cola.
- **Rediseño de Novedades, Actualizaciones OTA & Transiciones Fluidas:** Nuevo modal flotante inferior con tarjeta Hero, onda sonora animada y acordeones desplegables para *Actualización disponible* y *Novedades*, además de mejoras de fluidez, color de acento en tiempo real y respaldo en Google Drive listo para producción.

---

<div align="center">

<h1><a id="architecture"></a>Arquitectura y Stack Tecnológico</h1>

</div>

SoundChord está construido bajo una arquitectura **Kotlin Multiplatform / Android Modular** diseñada para mantener las funcionalidades propias desacopladas del núcleo de reproducción:

- **Lenguaje & UI:** Kotlin, Jetpack Compose, Material 3, Haze (Glassmorphism) y shaders AGSL nativos.
- **Motor de Audio:** Media3 / ExoPlayer con soporte para procesamiento de audio de alta resolución, AutoMix, crossfade dinámico, reproducción *gapless* y sesiones remotas.
- **Estructura Modular:**
  - `:app` — Punto de entrada Android, navegación principal, widgets, copias de seguridad en Google Drive, actualizador OTA y vistas específicas de plataforma.
  - `:shared` & `:sharedUi` — Lógica compartida, componentes de interfaz del reproductor, animaciones, recursos multiplataforma y modelos de dominio.
  - `:soundchord:features` — Módulo dedicado para las extensiones exclusivas de SoundChord (*Radio Browser & Estaciones*, *Shazam & Historial*, diálogos *Liquid Glass* y selector de *Salida de audio*).

---

<div align="center">

<h1><a id="download"></a>Descargas (Versión 1.3.0)</h1>

<a href="https://github.com/808andrex/SoundChord/releases/latest" target="_blank" rel="noopener noreferrer"><img src="https://cdn.jsdelivr.net/gh/JMcrafter26/badges@main/src/assets/supported/android/cozy.svg" alt="Descargar SoundChord para Android" height="55"/></a>

Obtén el APK oficial firmado desde la sección de [Releases de GitHub](https://github.com/808andrex/SoundChord/releases/latest) (elige **`arm64-v8a`** para la mayoría de teléfonos Android modernos, o **`universal`** si desconoces tu arquitectura).

### ⚠️ Nota sobre la instalación (Play Protect)

Al instalar el APK por primera vez fuera de Google Play, Android puede mostrar la ventana *"Se recomienda analizar la app"* debido a que cada versión compilada genera una firma digital única:

1. Pulsa **Analizar app** para completar la verificación automática de seguridad de Google (recomendado).
2. Si prefieres omitir el análisis, toca en **Más detalles** y selecciona **Instalar de todos modos**.

*SoundChord es 100% seguro, de código abierto y sin rastreadores comerciales.*

</div>

---

<div align="center">

<h1><a id="contributing"></a>Créditos y Origen</h1>

SoundChord es un fork modular desarrollado por [808andrex](https://github.com/808andrex), basado originalmente en el proyecto de código abierto **BitChord** de [kushagrasinghx](https://github.com/kushagrasinghx).

Gracias a la comunidad de desarrolladores y proyectos open-source que hicieron posible este reproductor.

</div>

---

<div align="center">

<h1><a id="privacy"></a>Privacidad</h1>

SoundChord almacena tu historial *Replay*, descargas y preferencias de forma local en tu dispositivo. Las copias de seguridad opcionales en Google Drive utilizan únicamente el permiso limitado `drive.file` para gestionar los archivos generados por la propia aplicación.

Consulta la [Política de Privacidad completa (`PRIVACY.md`)](https://github.com/808andrex/SoundChord/blob/main/PRIVACY.md) para más detalles.

</div>

---

<div align="center">

<h1><a id="disclaimer"></a>Aviso Legal</h1>

SoundChord es un cliente de audio de código abierto desarrollado estrictamente con fines educativos y de investigación personal.

- **Sin Alojamiento de Archivos:** SoundChord no aloja ni distribuye archivos de audio con derechos de autor. Funciona exclusivamente como un cliente para indexar la biblioteca local del teléfono o reproducir contenido mediante fuentes y APIs públicas.
- **Licencia Copyleft:** Publicado bajo la licencia **GNU General Public License v3.0 (GPLv3)**.

</div>
