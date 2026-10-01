package com.atulpandey.clearwrite.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal enum class ClearWriteIcon {
  MORE,
  IMPORT,
  CHEVRON_RIGHT,
  CHECK,
  DOCUMENT,
  WRITING,
  SETTINGS,
  HELP,
  SPARK,
}

@Composable
internal fun ClearWriteIconGraphic(
  icon: ClearWriteIcon,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val w = size.width
    val h = size.height
    when (icon) {
      ClearWriteIcon.MORE -> {
        val radius = 1.7.dp.toPx()
        listOf(.28f, .5f, .72f).forEach { y -> drawCircle(tint, radius, Offset(w / 2, h * y)) }
      }
      ClearWriteIcon.IMPORT -> {
        drawLine(tint, Offset(w * .5f, h * .16f), Offset(w * .5f, h * .64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .31f, h * .46f), Offset(w * .5f, h * .65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .69f, h * .46f), Offset(w * .5f, h * .65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        val tray = Path().apply { moveTo(w * .2f, h * .68f); lineTo(w * .2f, h * .84f); lineTo(w * .8f, h * .84f); lineTo(w * .8f, h * .68f) }
        drawPath(tray, tint, style = stroke)
      }
      ClearWriteIcon.CHEVRON_RIGHT -> {
        drawLine(tint, Offset(w * .38f, h * .28f), Offset(w * .62f, h * .5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .62f, h * .5f), Offset(w * .38f, h * .72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      ClearWriteIcon.CHECK -> {
        drawLine(tint, Offset(w * .2f, h * .52f), Offset(w * .42f, h * .72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .42f, h * .72f), Offset(w * .8f, h * .3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      ClearWriteIcon.DOCUMENT -> {
        val page = Path().apply { moveTo(w * .25f, h * .12f); lineTo(w * .63f, h * .12f); lineTo(w * .78f, h * .28f); lineTo(w * .78f, h * .88f); lineTo(w * .25f, h * .88f); close() }
        drawPath(page, tint, style = stroke)
        drawLine(tint, Offset(w * .36f, h * .46f), Offset(w * .66f, h * .46f), strokeWidth = stroke.width)
        drawLine(tint, Offset(w * .36f, h * .61f), Offset(w * .66f, h * .61f), strokeWidth = stroke.width)
      }
      ClearWriteIcon.WRITING -> {
        drawLine(tint, Offset(w * .2f, h * .79f), Offset(w * .71f, h * .28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .65f, h * .22f), Offset(w * .78f, h * .35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .2f, h * .79f), Offset(w * .35f, h * .75f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      ClearWriteIcon.SETTINGS -> {
        drawCircle(tint, w * .29f, Offset(w / 2, h / 2), style = stroke)
        drawCircle(tint, w * .09f, Offset(w / 2, h / 2), style = stroke)
      }
      ClearWriteIcon.HELP -> {
        drawCircle(tint, w * .36f, Offset(w / 2, h / 2), style = stroke)
        drawCircle(tint, 1.3.dp.toPx(), Offset(w / 2, h * .72f))
        drawLine(tint, Offset(w * .42f, h * .37f), Offset(w * .5f, h * .31f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .5f, h * .31f), Offset(w * .59f, h * .38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * .59f, h * .38f), Offset(w * .5f, h * .53f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      ClearWriteIcon.SPARK -> {
        val p = Path().apply { moveTo(w * .5f, h * .12f); lineTo(w * .58f, h * .42f); lineTo(w * .88f, h * .5f); lineTo(w * .58f, h * .58f); lineTo(w * .5f, h * .88f); lineTo(w * .42f, h * .58f); lineTo(w * .12f, h * .5f); lineTo(w * .42f, h * .42f); close() }
        drawPath(p, tint)
      }
    }
  }
}
