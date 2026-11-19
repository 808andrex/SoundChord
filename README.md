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

[**Descargar APK**](#download) · [**Características**](#features) · [**Novedades v1.2.0**](#whats-new) · [**Arquitectura**](#architecture) · [**Créditos**](#contributing) · [**Aviso Legal**](#disclaimer)

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
- **Radio Global & Historial** — Búsqueda global en Radio Browser con rotación automática de servidores espejo, estaciones personalizadas con recomendaciones, menús contextuales e historial reciente.
- **Reconocimiento musical (Shazam) + Historial** — Identificación de canciones en tiempo real mediante el micrófono con historial local persistente, buscador instantáneo y reproducción directa.
- **Crossfade y AutoMix inteligente** — Transiciones suaves de 0 a 12 segundos y fundido cruzado al saltar pistas sincronizado con el punto de mezcla DJ.
- **Sesiones Escuchar Juntos con Control Remoto** — Modo de silenciado local para actuar únicamente como controlador remoto sin interrumpir la reproducción activa.
- **Reproducción local y descargas** — Guarda pistas con metadatos embebidos e integra la música almacenada en tu dispositivo.

    </td>
    <td width="50%" valign="top">

#### Experiencia e Interfaz
- **Onboarding Interactivo & Color de Acento Global** — Flujo inicial de 4 pasos con selección inteligente de artistas, vista previa en vivo de tema/cristal, paleta de colores de acento dinámicos e iconos alternativos (*Monocromo*, *Rojo clásico*, *Verde Spotify*).
- **Diseño "Liquid Glass" (Glassmorphism)** — Tarjetas flotantes translúcidas adaptativas a Tema Claro y Oscuro para *Salida de audio*, controles del reproductor, diálogos de calidad y menús contextuales.
- **Transiciones Fluidas de Carátula (AGSL)** — Shader de disolución líquida en Android 13+, precarga inteligente de portadas vecinas, portadas animadas y Spotify Canvas.
- **Gestión Rápida de Cola y Playlists** — Buscador modal con sugerencias inteligentes para añadir canciones directamente a la cola, a tus playlists propias o a *Música que me gusta*.
- **Letras sincronizadas palabra por palabra** — Animación tipo Apple Music con soporte multi-proveedor (Musixmatch, LRCLIB, BiniLyrics, Genius).
- **Actualizador OTA & Pantalla *Acerca de*** — Detección automática desde GitHub Releases con notas de versión integradas, historial de cambios e instalación directa.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="whats-new"></a>Novedades en v1.2.0</h1>

</div>

- **Nuevo Flujo de Bienvenida (Onboarding de 4 Pasos):** Configuración inicial guiada con identidad para *Escuchar juntos*, selección interactiva de artistas favoritos (con sugerencias dinámicas *«Similares a...»* que alimentan tus recomendaciones de Inicio), vista previa en vivo (*Hazlo tuyo*) para probar **Tema**, **Cristal** y **Color de acento**, y conexión directa con YouTube Music.
- **Color de Acento Global e Iconos Alternativos:** Nuevo selector en *Apariencia → Tema e icono* con paleta de colores dinámicos en toda la interfaz, además de 3 nuevos iconos de aplicación (**Monocromo**, **Rojo clásico** y **Verde Spotify**).
- **Historial de Radio y Búsqueda Global Mejorada:** Acceso rápido a emisoras escuchadas recientemente desde *Radio* y solución de búsqueda internacional en Radio Browser mediante rotación automática de mirrors (`de1`, `de2`, `fi1`, `nl1`) y filtros globales.
- **Pulido Integral de Tema Claro (*Light Mode*) y Cabeceras:** Adaptación completa de contraste en *Radio*, búsqueda, estaciones personalizadas (*Canciones recomendadas*), historiales, botón flotante de Shazam y diálogos *Liquid Glass*, junto con cabeceras alineadas entre *Explorar*, *Radio* y *Biblioteca*.
- **Ajustes Reorganizados, Badges «NUEVO» y Pantalla *Acerca de*:** Categorías más claras con distintivos interactivos que desaparecen al explorar cada novedad, y nueva vista *Información de SoundChord* con actualizador integrado y notas de versión.

---

<div align="center">

<h1><a id="architecture"></a>Arquitectura y Stack Tecnológico</h1>

</div>

SoundChord está construido bajo una arquitectura **Kotlin Multiplatform / Android Modular** diseñada para mantener las funcionalidades propias desacopladas del núcleo de reproducción:

- **Lenguaje & UI:** Kotlin, Jetpack Compose, Material 3, Haze (Glassmorphism) y shaders AGSL nativos.
- **Motor de Audio:** Media3 / ExoPlayer con soporte para procesamiento de audio de alta resolución, crossfade dinámico y sesiones remotas.
- **Estructura Modular:**
  - `:app` — Punto de entrada Android, navegación principal, integración de servicios y vistas específicas de plataforma.
  - `:shared` & `:sharedUi` — Lógica compartida, componentes de interfaz del reproductor, recursos multiplataforma y modelos de dominio.
  - `:soundchord:features` — Módulo dedicado para las extensiones exclusivas de SoundChord (*Radio Browser & Historial*, *Shazam & Historial*, diálogos *Liquid Glass* y selector de *Salida de audio*).

---

<div align="center">

<h1><a id="download"></a>Descargas (Versión 1.2.0)</h1>

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

<h1><a id="disclaimer"></a>Aviso Legal</h1>

SoundChord es un cliente de audio de código abierto desarrollado estrictamente con fines educativos y de investigación personal.

- **Sin Alojamiento de Archivos:** SoundChord no aloja ni distribuye archivos de audio con derechos de autor. Funciona exclusivamente como un cliente para indexar la biblioteca local del teléfono o reproducir contenido mediante fuentes y APIs públicas.
- **Licencia Copyleft:** Publicado bajo la licencia **GNU General Public License v3.0 (GPLv3)**.

</div>
