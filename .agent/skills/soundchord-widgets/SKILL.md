---
name: soundchord-widgets
description: Reglas de diseño y restricciones técnicas de RemoteViews para los widgets de pantalla de inicio 2x2 y 4x2 de SoundChord.
---

# Guía de Widgets de Pantalla de Inicio (`MediaWidget`)

1. **Estructura visual estilo Apple Music / iOS:**
   - Existen dos formatos: cuadrado (`2x2`) y rectangular ancho (`4x2`).
   - Ambos muestran carátula con esquinas redondeadas, logotipo de **SoundChord** solo en la esquina superior derecha (**sin botón de corazón/Me gusta**), título, artista y 3 controles de transporte (`Anterior`, `Reproducir/Pausar`, `Siguiente`).
   - En el widget `2x2`, la carátula debe tener un tamaño intermedio equilibrado (`~68dp–74dp`).

2. **Restricciones de `RemoteViews` y fondos:**
   - Usar exclusivamente vistas y atributos soportados por `RemoteViews` en XML para evitar el error *"No se puede agregar el widget"*.
   - Aplicar `android:clipToOutline="true"` en un contenedor raíz único redondeado, sin capas oscuras secundarias detrás que generen bordes o esquinas negras.
   - Si la carátula es monocromática o de baja saturación (blanco/negro), generar un degradado neutro (gris grafito / carbón suave) y nunca fallbacks azules o morados.