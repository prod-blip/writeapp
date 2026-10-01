package com.atulpandey.clearwrite.ui.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.WritingIssue
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueHighlightTransformationTest {
  @Test
  fun selectedIssue_appliesBackgroundToExactAnalyzerRange() {
    val text = "Short sentence. This sentence needs attention."
    val start = text.indexOf("This")
    val end = text.length
    val color = Color(0x553366FF)
    val issue =
      WritingIssue(
        category = AnalysisCategory.VERY_HARD_SENTENCE,
        startOffset = start,
        endOffset = end,
        explanation = "Test issue",
      )

    val transformed = issueHighlightTransformation(issue, color).filter(AnnotatedString(text))
    val highlight = transformed.text.spanStyles.single()

    assertEquals(text, transformed.text.text)
    assertEquals(start, highlight.start)
    assertEquals(end, highlight.end)
    assertEquals(color, highlight.item.background)
  }

  @Test
  fun appliedAiEdit_usesGreenBackgroundOnReplacementRange() {
    val text = "Clear writing helps every reader."
    val color = Color(0x554CAF50)
    val appliedHighlight =
      AppliedAiHighlight(
        startOffset = 6,
        endOffset = 19,
        documentRevision = 2,
      )

    val transformed =
      editorHighlightTransformation(
          issue = null,
          appliedAiHighlight = appliedHighlight,
          issueColor = Color.Blue,
          appliedAiColor = color,
        )
        .filter(AnnotatedString(text))
    val highlight = transformed.text.spanStyles.single()

    assertEquals(6, highlight.start)
    assertEquals(19, highlight.end)
    assertEquals(color, highlight.item.background)
  }
}
