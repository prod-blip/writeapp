package com.atulpandey.clearwrite.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EditorialSans = FontFamily.SansSerif
private val ReadingSerif = FontFamily.Serif

private fun editorialStyle(
  weight: FontWeight,
  size: androidx.compose.ui.unit.TextUnit,
  lineHeight: androidx.compose.ui.unit.TextUnit,
  letterSpacing: androidx.compose.ui.unit.TextUnit,
) = TextStyle(
  fontFamily = EditorialSans,
  fontWeight = weight,
  fontSize = size,
  lineHeight = lineHeight,
  letterSpacing = letterSpacing,
)

val Typography = Typography(
  headlineSmall = editorialStyle(FontWeight.SemiBold, 24.sp, 30.sp, (-0.2).sp),
  titleLarge = editorialStyle(FontWeight.SemiBold, 21.sp, 27.sp, (-0.1).sp),
  titleMedium = editorialStyle(FontWeight.SemiBold, 17.sp, 23.sp, 0.sp),
  titleSmall = editorialStyle(FontWeight.SemiBold, 15.sp, 20.sp, 0.sp),
  bodyLarge = editorialStyle(FontWeight.Normal, 17.sp, 27.sp, 0.sp),
  bodyMedium = editorialStyle(FontWeight.Normal, 15.sp, 22.sp, 0.1.sp),
  bodySmall = editorialStyle(FontWeight.Normal, 13.sp, 18.sp, 0.1.sp),
  labelLarge = editorialStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
  labelMedium = editorialStyle(FontWeight.Medium, 12.sp, 17.sp, 0.2.sp),
  labelSmall = editorialStyle(FontWeight.Medium, 11.sp, 16.sp, 0.3.sp),
)

object ClearWriteType {
  val editor = TextStyle(
    fontFamily = ReadingSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.sp,
  )

  val score = editorialStyle(FontWeight.SemiBold, 18.sp, 22.sp, (-0.2).sp)
}
