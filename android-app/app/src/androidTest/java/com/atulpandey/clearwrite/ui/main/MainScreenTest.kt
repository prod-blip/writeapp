package com.atulpandey.clearwrite.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.WritingAnalyzer
import com.atulpandey.clearwrite.analysis.WritingGoal
import com.atulpandey.clearwrite.ai.AiSuggestion
import com.atulpandey.clearwrite.data.ThemePreference
import com.atulpandey.clearwrite.theme.ClearWriteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun emptyEditor_showsPasteFirstState() {
    composeTestRule.setContent { ClearWriteTheme { TestEditor(MainScreenUiState()) } }

    composeTestRule.onNodeWithText("Paste or type your text here…").assertExists()
    composeTestRule.onNodeWithText("Paste").assertExists()
    composeTestRule.onNodeWithText("Check writing").assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription("Analysis panel").assertDoesNotExist()
  }

  @Test
  fun pasteButton_readsTextFromAndroidClipboard() {
    val viewModel = MainScreenViewModel()
    composeTestRule.setContent { ClearWriteTheme { MainScreen(viewModel = viewModel) } }
    composeTestRule.runOnIdle {
      val clipboard = composeTestRule.activity.getSystemService(ClipboardManager::class.java)
      clipboard.setPrimaryClip(ClipData.newPlainText("ClearWrite test", "Pasted text from clipboard."))
    }

    composeTestRule.onNodeWithText("Paste").performClick()

    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      viewModel.uiState.value.text == "Pasted text from clipboard."
    }
    composeTestRule.onNodeWithContentDescription("Writing editor").assertExists()
    composeTestRule.onNodeWithText("Paste").assertDoesNotExist()
  }

  @Test
  fun enteringText_generatesCollapsedAnalysisAutomatically() {
    val viewModel = MainScreenViewModel()
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    composeTestRule.onNodeWithContentDescription("Writing editor").performTextInput("A short draft")
    composeTestRule.waitUntil(timeoutMillis = 3_000) { viewModel.uiState.value.analysis != null }
    composeTestRule.onNodeWithText("Check writing").assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription("Analysis panel").assertExists()
    composeTestRule.onNodeWithContentDescription("Analysis summary").assertExists()
    composeTestRule.onNodeWithText("3 words · 0 issues · General goal").assertExists()
    composeTestRule.onNodeWithText("Pull up").assertDoesNotExist()
  }

  @Test
  fun collapsedAnalysis_canBeOpenedByTappingItsPeek() {
    val text = (1..26).joinToString(" ") { "word$it" } + "."
    val analysis = WritingAnalyzer().analyze(text)
    composeTestRule.setContent {
      ClearWriteTheme { TestEditor(MainScreenUiState(text = text, analysis = analysis)) }
    }

    composeTestRule.onNodeWithContentDescription("Analysis summary").performClick()

    composeTestRule.onNodeWithContentDescription("Long sentences, 1 issue").assertExists()
    composeTestRule.onNodeWithText("Pull down").assertDoesNotExist()
  }

  @Test
  fun collapsedAnalysis_doesNotLeaveAnExtraGapBelowEditor() {
    val text = "A short draft."
    val analysis = WritingAnalyzer().analyze(text)
    composeTestRule.setContent {
      ClearWriteTheme { TestEditor(MainScreenUiState(text = text, analysis = analysis)) }
    }

    val editorBottom =
      composeTestRule
        .onNodeWithContentDescription("Writing editor")
        .fetchSemanticsNode()
        .boundsInRoot
        .bottom
    val analysisTop =
      composeTestRule
        .onNodeWithContentDescription("Analysis panel")
        .fetchSemanticsNode()
        .boundsInRoot
        .top

    assertTrue("Editor should meet the analysis sheet without a blank band", analysisTop - editorBottom < 2f)
  }

  @Test
  fun importSheet_cancelReturnsToEditor() {
    val viewModel = MainScreenViewModel()
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    composeTestRule.onNodeWithContentDescription("Import document").performClick()
    composeTestRule.onNodeWithText("Import document").assertExists()
    composeTestRule.onNodeWithText("Cancel").performClick()

    composeTestRule.onNodeWithText("Import document").assertDoesNotExist()
    composeTestRule.onNodeWithContentDescription("Writing editor").assertExists()
  }

  @Test
  fun overflowMenu_opensGroupedDestinations() {
    val viewModel = MainScreenViewModel()
    val destinations =
      listOf(
        "Document" to "Free plan · one saved document",
        "Writing" to "Local checks",
        "Settings & help" to "Privacy & data",
      )
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    destinations.forEach { (menuItem, expectedContent) ->
      composeTestRule.onNodeWithContentDescription("More options").performClick()
      composeTestRule.onNodeWithText(menuItem).performScrollTo().performClick()
      composeTestRule.onNodeWithText(expectedContent).assertExists()
      composeTestRule.onNodeWithText("Close").performClick()
    }
  }

  @Test
  fun startNewDocument_requiresConfirmation() {
    val viewModel = MainScreenViewModel()
    viewModel.onTextChanged("Keep this draft.")
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    composeTestRule.onNodeWithContentDescription("More options").performClick()
    composeTestRule.onNodeWithText("Document").performClick()
    composeTestRule.onNodeWithText("Start new document").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Start a new document?").assertExists()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.runOnIdle { assertEquals("Keep this draft.", viewModel.uiState.value.text) }

    composeTestRule.onNodeWithContentDescription("More options").performClick()
    composeTestRule.onNodeWithText("Document").performClick()
    composeTestRule.onNodeWithText("Start new document").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Start new").performClick()
    composeTestRule.runOnIdle { assertEquals("", viewModel.uiState.value.text) }
  }

  @Test
  fun settings_selectingDarkTheme_updatesPreference() {
    val viewModel = MainScreenViewModel()
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    composeTestRule.onNodeWithContentDescription("More options").performClick()
    composeTestRule.onNodeWithText("Settings & help").performClick()
    composeTestRule.onNodeWithText("Dark").performClick()

    composeTestRule.runOnIdle {
      assertEquals(ThemePreference.DARK, viewModel.uiState.value.themePreference)
    }
  }

  @Test
  fun settings_selectingWritingGoal_updatesScoringTarget() {
    val viewModel = MainScreenViewModel()
    composeTestRule.setContent {
      ClearWriteTheme { MainScreen(viewModel = viewModel) }
    }

    composeTestRule.onNodeWithContentDescription("More options").performClick()
    composeTestRule.onNodeWithText("Writing").performClick()
    composeTestRule.onNodeWithText("Academic / technical").performScrollTo().performClick()

    composeTestRule.runOnIdle {
      assertEquals(WritingGoal.ACADEMIC_TECHNICAL, viewModel.uiState.value.writingGoal)
    }
  }

  @Test
  fun selectingLongSentences_opensFirstIssueInEditor() {
    val text = (1..26).joinToString(" ") { "word$it" } + "."
    val analysis = WritingAnalyzer().analyze(text)

    composeTestRule.setContent {
      var state by remember {
        mutableStateOf(MainScreenUiState(text = text, analysis = analysis, showAnalysis = true))
      }
      ClearWriteTheme {
        TestEditor(
          uiState = state,
          onIssueCategorySelected = { category ->
            val issueIndex = analysis.issues.indexOfFirst { it.category == category }
            state = state.copy(showAnalysis = false, selectedIssueIndex = issueIndex)
          },
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Long sentences, 1 issue").performClick()

    composeTestRule.onNodeWithText("Long sentence").assertExists()
    composeTestRule.onNodeWithText("1 of 1").assertExists()
    composeTestRule.onNodeWithText("Edit in draft").assertExists()
    composeTestRule.onNodeWithText("Ignore").assertExists()
    composeTestRule.onNodeWithText("This sentence has 26 words. Consider splitting it.").assertExists()
    composeTestRule.onNodeWithText("Improve with AI · Pro").assertExists()
  }

  @Test
  fun readyAiSuggestion_requiresExplicitUse() {
    val target = AiRewriteTarget(0, 24, "This is very hard to read.")
    val suggestion = AiSuggestion("This is hard to read.", "Removes an unnecessary intensifier.")
    val state =
      MainScreenUiState(
        text = target.originalText,
        aiRewriteState = AiRewriteState.Ready(target, listOf(suggestion), emptyList()),
      )

    composeTestRule.setContent { ClearWriteTheme { TestEditor(state) } }

    composeTestRule.onNodeWithText("AI improvement").assertExists()
    composeTestRule.onNodeWithText("This is hard to read.").assertExists()
    composeTestRule.onNodeWithText("Use suggestion").assertExists()
    composeTestRule.onNodeWithText("Keep my original").assertExists()
  }

  @Test
  fun firstAiUse_explainsDataSharingBeforeContinue() {
    val text = "This is very hard to read."
    val state =
      MainScreenUiState(
        text = text,
        aiRewriteState = AiRewriteState.Consent(AiRewriteTarget(0, text.length, text)),
      )

    composeTestRule.setContent { ClearWriteTheme { TestEditor(state) } }

    composeTestRule.onNodeWithText("Continue to AI").assertExists()
    composeTestRule
      .onNodeWithText(
        "To create a suggestion, ClearWrite sends this selected passage and nearby context to the AI provider. The ClearWrite backend does not store it."
      )
      .assertExists()
  }

  @Test
  fun selectedTextRewrite_showsFocusedRewriteChoices() {
    val text = "This sentence needs a clearer ending."
    val state =
      MainScreenUiState(
        text = text,
        selectionRewriteTarget = AiRewriteTarget(5, 13, "sentence"),
      )

    composeTestRule.setContent { ClearWriteTheme { TestEditor(state) } }

    composeTestRule.onNodeWithText("Rewrite selection").assertExists()
    composeTestRule.onNodeWithText("Improve clarity").assertExists()
    composeTestRule.onNodeWithText("Shorten").assertExists()
    composeTestRule.onNodeWithText("Simplify").assertExists()
    composeTestRule.onNodeWithText("Change tone").assertExists()
    composeTestRule.onNodeWithText("Custom instruction").assertExists()
  }
}

@androidx.compose.runtime.Composable
private fun TestEditor(
  uiState: MainScreenUiState,
  onTextChanged: (String) -> Unit = {},
  onIssueCategorySelected: (AnalysisCategory) -> Unit = {},
) {
  EditorScreen(
    uiState = uiState,
    onTextChanged = onTextChanged,
    onPasteUnavailable = {},
    onImportClick = {},
    onShowAnalysis = {},
    onDismissAnalysis = {},
    onIssueCategorySelected = onIssueCategorySelected,
    onPreviousIssue = {},
    onNextIssue = {},
    onDismissIssue = {},
    onImproveWithAi = {},
    onSelectionRewriteRequested = { _, _ -> },
    onSelectionRewriteAction = { _, _, _ -> },
    onDismissSelectionRewrite = {},
    onApplyAiSuggestion = {},
    onConfirmAiConsent = {},
    onRetryAi = {},
    onDismissAi = {},
    onUndoAiEdit = {},
    onMenuDestinationSelected = {},
    onMessageShown = {},
  )
}
