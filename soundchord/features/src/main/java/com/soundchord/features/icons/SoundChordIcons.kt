package com.soundchord.features.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object SoundChordIcons {
    val TabRadio: ImageVector get() = Radio

    val Radio: ImageVector by lazy {
        ImageVector.Builder(
            name = "bc_radio",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // 1. Rutas de las ondas de radio
            addPath(
                pathData = addPathNodes(
                    "M16.247 7.761a6 6 0 0 1 0 8.478" +
                            "M19.075 4.933a10 10 0 0 1 0 14.134" +
                            "M4.925 19.067a10 10 0 0 1 0-14.134" +
                            "M7.753 16.239a6 6 0 0 1 0-8.478"
                ),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            // 2. Círculo central (reemplaza a <circle cx="12" cy="12" r="2"/>)
            addPath(
                pathData = addPathNodes(
                    "M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"
                ),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }
}