package com.atulpandey.clearwrite.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorScrollTargetTest {
  @Test
  fun highlightedText_isPlacedBelowTopWithReadingContext() {
    assertEquals(760, editorScrollTarget(highlightedTopPx = 1_000f, viewportHeightPx = 1_000f, maxScroll = 2_000))
  }

  @Test
  fun highlightedText_nearDocumentEdgesClampsToScrollRange() {
    assertEquals(0, editorScrollTarget(highlightedTopPx = 100f, viewportHeightPx = 1_000f, maxScroll = 2_000))
    assertEquals(2_000, editorScrollTarget(highlightedTopPx = 3_000f, viewportHeightPx = 1_000f, maxScroll = 2_000))
  }
}
