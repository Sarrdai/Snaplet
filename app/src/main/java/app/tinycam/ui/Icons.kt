package app.tinycam.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/** Small hand-drawn icon set (24x24 viewport), tinted at the call site. */
object Icons {
    val Camera = outline("M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", "M12 10a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z")
    val Video = outline("M4 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M16 10.5l5-3v9l-5-3z")
    val Grid = outline(
        "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
        "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
        "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
        "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
    )
    val Lock = outline("M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Close = outline("M6 6l12 12", "M18 6L6 18", width = 3f)
    val Check = outline("M5 12.5l4.5 4.5L19 7.5", width = 3f)
    val Play = filled("M7 4.5v15l12.5-7.5z")
    val Flip = outline("M19.5 10a8 8 0 0 0-14-3.5", "M5 3v4h4", "M4.5 14a8 8 0 0 0 14 3.5", "M19 21v-4h-4")
    val Timer = outline("M12 5a8 8 0 1 0 0 16a8 8 0 1 0 0-16z", "M12 9v4l2.5 2", "M10 2h4")
    val Wand = outline("M4 20L15 9", "M18 3v4", "M16 5h4", "M8 4v2", "M7 5h2", "M19 13v2", "M18 14h2")
    val Search = outline("M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13z", "M15.5 15.5L20 20", width = 2.6f)

    private fun outline(vararg paths: String, width: Float = 2.2f) = build { b ->
        paths.forEach {
            b.addPath(
                addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    private fun filled(path: String) = build { b -> b.addPath(addPathNodes(path), fill = SolidColor(Color.White)) }

    private fun build(block: (ImageVector.Builder) -> Unit) =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .also(block)
            .build()
}
