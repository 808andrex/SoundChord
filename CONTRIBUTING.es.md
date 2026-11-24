# Cómo Contribuir a SoundChord

[**English**](CONTRIBUTING.md) · [**Español**](CONTRIBUTING.es.md)

Gracias por contribuir a SoundChord. Sigue estas directrices para asegurar un proceso de desarrollo y revisión eficiente.

## Proponer Contribuciones
- Las características sustanciales, refactorizaciones o rediseños de interfaz normalmente deben debatirse antes de su implementación. Abre un issue en GitHub para describir tu propuesta.
- Las correcciones sencillas de errores (bug fixes), mejoras de documentación, traducciones o correcciones menores pueden enviarse directamente como un Pull Request.

## Flujo de Ramas y Pull Requests
Comprueba siempre el repositorio para conocer la versión activa o la rama de desarrollo antes de empezar a trabajar.

El nuevo trabajo normalmente debe basarse en la versión activa o rama de desarrollo más reciente (por ejemplo, `release/v1.3` durante el ciclo v1.3.x) en lugar de `main`, a menos que los mantenedores indiquen explícitamente lo contrario.

Al contribuir:
- Haz un fork del repositorio y crea una rama descriptiva (ej. `fix/radio-stream`, `feat/new-station-provider`).
- Mantén los pull requests enfocados en un único cambio o corrección de error. Evita mezclar refactorizaciones no relacionadas, retoques estéticos o reformateos masivos con cambios funcionales.
- Mantén tu rama razonablemente sincronizada con la rama base de destino.
- Proporciona una descripción clara en el PR explicando qué se modificó y por qué.
- Incluye detalles pertinentes sobre las pruebas y validación en tu PR.
- Responde con prontitud a los comentarios de revisión y mantén las conversaciones centradas en los aspectos técnicos.

## Entorno de Desarrollo
El proyecto requiere las siguientes herramientas:
- **JDK**: Java Development Kit 17 (se recomienda Eclipse Temurin 17).
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- **C/C++ NDK & CMake**: CMake 3.22.1+ y Android NDK (para componentes DSP de audio nativo configurados bajo `app/src/main/cpp`).
- **Backend de Escuchar Juntos (Opcional)**: Go 1.22+ si vas a desarrollar o probar el servidor de sala compartida (`backend/`).

## Comandos de Compilación y Pruebas
Ejecuta los comandos de Gradle desde la raíz del repositorio.

Compilación:
```bash
./gradlew assembleDevDebug
```

Windows:
```powershell
.\gradlew.bat assembleDevDebug
```

Pruebas unitarias:
```bash
./gradlew testDevDebugUnitTest
```

Windows:
```powershell
.\gradlew.bat testDevDebugUnitTest
```

Ejecutar una prueba específica:
```bash
./gradlew testDevDebugUnitTest --tests "com.music.bitchord.playback.audio.DirectAudioStreamingRegressionTest"
```

## Calidad del Código y Arquitectura Modular
- Sigue las convenciones idiomáticas de Kotlin y Jetpack Compose.
- Mantén la separación modular de responsabilidades de SoundChord (`:app`, `:shared`, `:sharedUi`, y `:soundchord:features` para características exclusivas de SoundChord como Radio Browser, reconocimiento con Shazam y componentes de interfaz Liquid Glass).
- Escribe pruebas unitarias para nueva lógica, correcciones y casos límite.
- Los colaboradores deben ejecutar las pruebas pertinentes, no deben introducir nuevos fallos y deben documentar fallos preexistentes conocidos cuando proceda.
- Mantén los diffs enfocados y mínimos. Evita dependencias de terceros innecesarias.

## Cambios de Audio y Telemetría
Para cambios relacionados con el motor de audio: la telemetría de la aplicación no debe presentarse como prueba del comportamiento físico del hardware a menos que dicho comportamiento se haya verificado realmente.

Distingue claramente entre los metadatos de origen, el formato del decodificador, el formato DSP interno, la salida de AudioTrack, el estado de AudioFlinger/HAL, las capacidades anunciadas por el dispositivo y el comportamiento del endpoint físico externo.

## Proceso de Revisión
Todos los pull requests requieren la revisión y aprobación de los mantenedores del repositorio antes de fusionarse. Los mantenedores revisan los cambios en busca de corrección técnica, encaje arquitectónico y mantenibilidad.

## Dependencias y Licencia
SoundChord está licenciado bajo la **GNU General Public License v3.0 (GPLv3)**, heredada del proyecto upstream BitChord.

Todo el código aportado y sus dependencias deben ser estrictamente compatibles con la licencia GPLv3. Evita introducir dependencias externas a menos que sea estrictamente necesario; cualquier nueva dependencia debe ser evaluada en cuanto a necesidad, tamaño de binario y cumplimiento de licencia.
