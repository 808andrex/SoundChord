package com.soundchord.features.radio.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object SoundChordIcons {
    // Alias para consumirlo de forma limpia
    val TabRadio: ImageVector get() = Radio

    // Definición del vector de Radio en código
    val Radio: ImageVector by lazy {
        ImageVector.Builder(
            name = "bc_radio",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.247 7.761a6 6 0 0 1 0 8.478 " +
                            "M19.075 4.933a10 10 0 0 1 0 14.134 " +
                            "M4.925 19.067a10 10 0 0 1 0-14.134 " +
                            "M7.753 16.239a6 6 0 0 1 0-8.478 " +
                            "M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4"
                ),
                stroke = SolidColor(Color.Unspecified), // Toma el tinte por defecto del componente Icon
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }
}