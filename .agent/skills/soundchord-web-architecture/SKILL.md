---
name: soundchord-web-architecture
description: Arquitectura completa de SoundChord Web (monorepo pnpm con app, extension, shared y scripts), bridge de comunicación sin CORS con YouTube/Spotify, empaquetado de extensión de navegador y diseño Liquid Glass para web.
---

# Arquitectura Web y Extensión de Navegador en SoundChord

Guía técnica integral sobre la arquitectura web de **SoundChord**, el monorepo `pnpm`, el protocolo de comunicación con la extensión de navegador para eludir restricciones CORS/DRM y las convenciones de diseño Liquid Glass inspiradas en Apple Music.

---

## 1. Estructura del Monorepo `pnpm`

El frontend de SoundChord se gestiona como un espacio de trabajo modular (`pnpm-workspace.yaml`) dentro del directorio `web/`:

```
web/
├── package.json                 # Orquestador del workspace (scripts globales)
├── pnpm-workspace.yaml          # Configuración de paquetes: app, extension, shared
├── tsconfig.base.json           # Configuración base de TypeScript (ES2023, Bundler)
├── README.md                    # Documentación general de SoundChord Web
│
├── app/                         # Aplicación Web SPA (React 19 + Vite + Tailwind v4)
│   ├── public/                  # Favicon SVG Liquid Glass, manifest.json, modelos ONNX
│   │   ├── downloads/           # Zips de la extensión y extension.json generados
│   │   └── models/              # Modelos ONNX de AutoMix (beat_this, vocals_umx)
│   ├── src/
│   │   ├── components/          # Componentes UI (Music, Lockups, TrackList, LogoMark)
│   │   ├── data/                # Consultas y adaptadores (InnerTube, Apple, Spotify)
│   │   ├── extension/           # Hooks y cliente del bridge (useExtension, useYtAuth)
│   │   ├── lyrics/              # Motor de sincronización TTML/LRC, reloj y providers
│   │   ├── pages/               # Vistas (Home, Browse, Search, Library, Settings, etc.)
│   │   ├── party/               # Sincronización en tiempo real "Escuchar juntos" (WebSocket)
│   │   ├── playback/            # Motor de audio, Shaka Player, resolver y AutoMix ONNX
│   │   ├── shell/               # AppShell, Sidebar, barra móvil, panel de letras
│   │   ├── state/               # Stores globales Zustand (player, ui, video)
│   │   └── styles/              # tokens.css (Apple Music Design Tokens) y global.css
│   ├── index.html               # Entrypoint HTML con metadatos PWA y OpenGraph
│   └── vite.config.ts           # Configuración Vite, Tailwind v4 y alias '@'
│
├── extension/                   # Extensión de Navegador Manifest V3 (WXT + TypeScript)
│   ├── dev-key/                 # Claves públicas para fijar el ID en Chromium (dev)
│   ├── entrypoints/
│   │   ├── background.ts        # Service worker MV3 (manejo de puente, cookies y reglas)
│   │   ├── popup/               # Popover de barra de herramientas (estado y desconexión)
│   │   ├── grant/               # Página de permisos para proveedores de letras
│   │   ├── discord/             # Flujo de conexión de Discord RPC
│   │   └── relay.content.ts     # Content script relay para Firefox (window.postMessage)
│   ├── lib/                     # Controladores (ytAuth, ytFetch, spotify, discord, rules)
│   ├── public/icon/             # Iconos PNG generados (16, 32, 48, 96, 128 px)
│   ├── scripts/                 # gen-icons.mjs, publish-to-site.mjs, gen-dev-key.mjs
│   ├── site.config.ts           # Configuración de orígenes permitidos (localhost, Vercel)
│   └── wxt.config.ts            # Manifiesto unificado WXT para Chromium y Firefox
│
├── shared/                      # Contrato y puente común (@soundchord/shared)
│   ├── package.json             # Paquete workspace interno consumido por app y extension
│   └── src/
│       ├── bridge/
│       │   ├── protocol.ts      # Canales (soundchord-bridge), tipos y envelopes
│       │   ├── client.ts        # Cliente Bridge en la página web
│       │   └── index.ts         # Exportaciones públicas de bridge
│       └── index.ts             # Punto de entrada de @soundchord/shared
│
├── docs/                        # Especificaciones de ingeniería inversa y DOM de Apple
└── scripts/                     # capture-fixtures.mjs para pruebas de integración
```

---

## 2. Comandos de Desarrollo, Compilación y Empaquetado

Todos los comandos se ejecutan desde el directorio `web/` con `pnpm`:

### Desarrollo Local
```bash
# Iniciar servidor web Vite (puerto fijo 5173 para coincidir con la extensión)
pnpm --dir web dev

# Iniciar extensión en modo desarrollo (Chromium)
pnpm --dir web dev:ext

# Iniciar extensión en modo desarrollo (Firefox)
pnpm --dir web dev:ext:firefox
```

### Verificación y Calidad de Código
```bash
# Comprobación de tipos estricta en todos los paquetes del workspace
pnpm --dir web typecheck

# Ejecutar suite de pruebas unitarias y de integración (Vitest)
pnpm --dir web test
```

### Empaquetado de la Extensión
```bash
# Compila Chromium MV3 y Firefox MV3, y genera los zips en web/app/public/downloads/
pnpm --dir web/extension package

# Regenerar iconos de la extensión desde el favicon Liquid Glass (web/app/public/favicon.svg)
pnpm --dir web/extension icons
```

### Compilación Completa de Producción
```bash
# Requiere VITE_PARTY_SERVER_URL definida (para el servidor de Escuchar juntos)
VITE_PARTY_SERVER_URL="https://party.soundchord.app" pnpm --dir web build:site
```
`build:site` ejecuta:
1. `pnpm --filter @soundchord/extension package`: Compila las dos versiones de la extensión (`chrome-mv3`, `firefox-mv3`), crea los zips versionados (`soundchord-extension-chrome.zip` y `soundchord-extension-firefox.zip`) y genera `extension.json`.
2. `pnpm --filter @soundchord/app build`: Compila TypeScript y genera los bundles optimizados con Vite en `web/app/dist/`.

---

## 3. Arquitectura de Comunicación Web ↔ Extensión (Anti-CORS)

Los navegadores prohíben por política CORS que una página web interactúe directamente con las APIs de YouTube Music (`/youtubei/v1`), descargue streams de `googlevideo.com` o lea cookies de sesión de Google/Spotify. SoundChord resuelve esto mediante una arquitectura en capas:

```
┌─────────────────────────────────────────────────────────────────┐
│               SoundChord Web Player (web/app)                   │
│   (Sin cookies, sin tokens confidenciales, requests simples)   │
└────────────────┬───────────────────────────────┬────────────────┘
                 │ (Chromium)                    │ (Firefox)
                 │ externally_connectable        │ window.postMessage
                 ▼                               ▼
┌────────────────────────────────┐ ┌──────────────────────────────┐
│  Service Worker (background.ts)│ │ Relay Script (relay.content) │
└────────────────┬───────────────┘ └─────────────┬────────────────┘
                 │                               │ Forward
                 ▼                               ▼
┌─────────────────────────────────────────────────────────────────┐
│              Motor de la Extensión (web/extension)              │
│  - Reglas declarativeNetRequest (DNR): inyección Origin/Referer │
│  - ytAuth.ts / ytFetch.ts: firma SAPISIDHASH con cookies reales │
│  - spotify.ts: lectura de sesión sp_dc                          │
│  - discord.ts: Gateway RPC sobre WebSockets                     │
└────────────────────────────────┬────────────────────────────────┘
                                 │ HTTP / Audio Byte-Ranges
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│          YouTube Music / googlevideo / Spotify / LRCLIB         │
└─────────────────────────────────────────────────────────────────┘
```

### A. Capa de Transporte y Aislamiento por Navegador
- **Chromium (Chrome, Edge, Brave):**
  - Utiliza `externally_connectable` con orígenes explícitos (`https://soundchord.vercel.app/*`, `http://localhost/*`, `http://127.0.0.1/*`).
  - El ID de la extensión se fija en desarrollo mediante `extension/dev-key/` (`pkddffeebcmhhanjcigilehaeonkkfih`), permitiendo que la web se conecte de inmediato sin configuración.
- **Firefox:**
  - Firefox no soporta `externally_connectable` hacia páginas web regulares.
  - Se utiliza un content script puente (`relay.content.ts`) inyectado en `document_start` que retransmite peticiones entre el contexto de la página y el fondo mediante `window.postMessage` y el canal `soundchord-bridge`.

### B. Evasión de Preflights CORS en InnerTube
- El backend de YouTube rechaza preflights `OPTIONS` (HTTP 400) incluso cuando provienen de YouTube.
- La aplicación web envía las llamadas a InnerTube como **peticiones simples**: cuerpo con `Content-Type: text/plain` y sin cabeceras personalizadas.
- La extensión aplica reglas de `declarativeNetRequest` (DNR) para inyectar en tránsito:
  - `Origin: https://music.youtube.com`
  - `Referer: https://music.youtube.com/`
  - `X-Origin: https://music.youtube.com`

### C. Sesión y Autenticación sin Filtración de Credenciales
- La página web **nunca** ve cookies ni tokens del usuario.
- En `ytAuth.ts`, la extensión lee la cookie `SAPISID` del almacén del navegador y genera la firma:
  $$\text{SAPISIDHASH } = \text{timestamp} + \text{"\_"} + \text{SHA1}(\text{timestamp} + \text{" "} + \text{SAPISID} + \text{" "} + \text{origin})$$
- Pasa `X-Goog-AuthUser` y `X-Goog-PageId` (para canales de marca) con `credentials: 'include'`, haciendo que el navegador adjunte las cookies en segundo plano de manera segura.

### D. Audio Streaming sin Bloqueos
- **Cliente VISIONOS (Principal):** Las URLs directas de audio obtenidas mediante el cliente `VISIONOS` se reproducen mediante rangos de bytes en Shaka Player. La regla 7 de la extensión elimina `Origin` y `Referer` en URLs con `c=VISIONOS` para evitar bloqueos HTTP 403 en `googlevideo.com`.
- **SABR (Fallback):** Para clientes web (`WEB_REMIX`), se utiliza un adaptador SABR con tokens WebPO generados dinámicamente.

---

## 4. Estética Liquid Glass y Convenciones de Marca

SoundChord Web replica minuciosamente la experiencia visual de **Apple Music Web**, adaptada a la identidad SoundChord:

### Principios de Diseño Liquid Glass
1. **Reproductor Flotante:** Un contenedor tipo cápsula esmerilada (`--player-pill-height: 56px`), flotando a `20px` del fondo en el área central de contenido, con `backdrop-filter: saturate(220%) blur(16px)` y borde tenue translúcido.
2. **Paleta de Acento:**
   - Color principal: `--keyColor: #d60017` (rojo Apple Music en fuentes DM Sans).
   - Acento brillante: `#ff375f` / `#fa233b`.
   - Superficies oscuras: `--systemStandardThinMaterialSover` (`#28282880`) y modo de contraste alto accesible.
3. **Tipografía:**
   - Tipografía principal de interfaz: **DM Sans** (Google Fonts).
   - Tipografía de letras sincronizadas: `'SoundChord SF Pro Display'` (`sf-pro-display-regular.woff2` y `sf-pro-display-bold.woff2`).
4. **Logotipo Oficial:**
   - Siempre renderizar la onda acústica SoundChord (`LogoMark.tsx` con viewBox `0 0 720 497`).
   - El favicon esmerilado (`favicon.svg`) presenta el squircle continuo iOS (`rx="224"`) con brillo especular radial superior y borde de cristal.

### Reglas Estrictas de Marca SoundChord
- **Prohibición de "BitChord":** Queda estrictamente prohibido introducir textos, comentarios públicos, nombres de paquetes o claves de almacenamiento con el término "BitChord".
- **Claves de Almacenamiento:** Todo estado persistente en `localStorage` o `IndexedDB` debe llevar el prefijo `soundchord:*` (ej. `soundchord:player`, `soundchord:lyrics`, `soundchord:automix`).
- **Rutas y Enlaces Externos:**
  - Repositorio y Releases: `https://github.com/808andrex/SoundChord`
  - Descargas de APK Android: `https://github.com/808andrex/SoundChord/releases/latest`
  - Origen de despliegue web: `https://soundchord.vercel.app`
  - Protocolo de deep link: `soundchord://party/<CODE>`
- **Canal de Comunicación:** Identificadores `soundchord-bridge` y puerto de eventos `soundchord-events`.
