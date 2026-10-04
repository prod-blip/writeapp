package com.atulpandey.clearwrite.ui.main

import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.WritingIssue
import com.atulpandey.clearwrite.analysis.WritingGoal
import com.atulpandey.clearwrite.ai.AiRewriteRequest
import com.atulpandey.clearwrite.ai.AiRewriteResponse
import com.atulpandey.clearwrite.ai.AiRewriteResult
import com.atulpandey.clearwrite.ai.AiRewriteService
import com.atulpandey.clearwrite.ai.AiSuggestion
import com.atulpandey.clearwrite.data.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private lateinit var mainDispatcher: TestDispatcher

  @Before
  fun setUpMainDispatcher() {
    mainDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(mainDispatcher)
  }

  @After
  fun resetMainDispatcher() {
    mainDispatcher.scheduler.advanceUntilIdle()
    Dispatchers.resetMain()
  }

  @Test
  fun wordCount_handlesWhitespace() {
    assertEquals(4, countWords("  One two\nthree\tfour  "))
  }

  @Test
  fun emptyDraft_cannotBeChecked() {
    assertFalse(MainScreenUiState().canCheck)
  }

  @Test
  fun populatedDraft_canBeChecked() {
    assertTrue(MainScreenUiState(text = "A short draft.").canCheck)
  }

  @Test
  fun freeWordLimit_disablesCheck() {
    val text = List(FREE_WORD_LIMIT + 1) { "word" }.joinToString(" ")
    val state = MainScreenUiState(text = text)

    assertTrue(state.wordLimitExceeded)
    assertFalse(state.canCheck)
  }

  @Test
  fun proPlan_usesHigherWordLimit() {
    val text = List(FREE_WORD_LIMIT + 1) { "word" }.joinToString(" ")
    val state = MainScreenUiState(text = text, isPro = true)

    assertFalse(state.wordLimitExceeded)
    assertEquals(PRO_WORD_LIMIT, state.wordLimit)
  }

  @Test
  fun phraseIssue_targetsItsWholeSentenceForAi() {
    val text = "The result is very useful. The next sentence stays as context."
    val start = text.indexOf("very")
    val issue =
      WritingIssue(
        category = AnalysisCategory.WEAKENER,
        startOffset = start,
        endOffset = start + "very".length,
        explanation = "Test issue",
      )

    val target = rewriteTargetForIssue(text, issue)

    assertEquals("The result is very useful.", target.originalText)
    assertEquals(0, target.startOffset)
  }

  @Test
  fun sentenceIssue_keepsAnalyzerRangeForAi() {
    val text = "First sentence. This sentence is deliberately long."
    val start = text.indexOf("This")
    val issue =
      WritingIssue(
        category = AnalysisCategory.LONG_SENTENCE,
        startOffset = start,
        endOffset = text.length,
        explanation = "Test issue",
      )

    assertEquals("This sentence is deliberately long.", rewriteTargetForIssue(text, issue).originalText)
  }

  @Test
  fun preferences_updateAndSurviveStartingNewDocument() {
    val viewModel = MainScreenViewModel(analysisDispatcher = mainDispatcher)

    viewModel.setCheckEnabled(AnalysisCategory.LONG_SENTENCE, false)
    viewModel.setShowWordCount(false)
    viewModel.setThemePreference(ThemePreference.DARK)
    viewModel.setWritingGoal(WritingGoal.PROFESSIONAL)
    viewModel.onTextChanged("A draft to replace.")
    viewModel.startNewDocument()

    assertFalse(AnalysisCategory.LONG_SENTENCE in viewModel.uiState.value.enabledChecks)
    assertFalse(viewModel.uiState.value.showWordCount)
    assertEquals(ThemePreference.DARK, viewModel.uiState.value.themePreference)
    assertEquals(WritingGoal.PROFESSIONAL, viewModel.uiState.value.writingGoal)
    assertEquals("", viewModel.uiState.value.text)
  }

  @Test
  fun textChange_generatesAnalysisAutomaticallyAfterDebounce() {
    val viewModel = MainScreenViewModel(analysisDispatcher = mainDispatcher)

    viewModel.onTextChanged("Clear writing helps readers understand an idea.")

    assertEquals(CheckState.CHECKING, viewModel.uiState.value.checkState)
    assertEquals(null, viewModel.uiState.value.analysis)
    mainDispatcher.scheduler.advanceTimeBy(301)
    mainDispatcher.scheduler.runCurrent()
    assertEquals(CheckState.IDLE, viewModel.uiState.value.checkState)
    assertTrue(viewModel.uiState.value.analysis != null)
    assertFalse(viewModel.uiState.value.showAnalysis)
  }

  @Test
  fun arbitrarySelectionRewrite_preservesRangeAndRequestedAction() {
    val service = RecordingAiRewriteService()
    val viewModel = MainScreenViewModel(aiRewriteService = service)
    val text = "Keep this. This section is unnecessarily wordy. Keep that."
    val start = text.indexOf("This section")
    val end = text.indexOf(" Keep that")
    viewModel.onTextChanged(text)
    viewModel.openSelectionRewrite(start, end)

    viewModel.requestSelectionRewrite(SelectionRewriteAction.SHORTEN)
    assertTrue(viewModel.uiState.value.aiRewriteState is AiRewriteState.Consent)
    viewModel.confirmAiConsent()

    val request = service.requests.single()
    assertEquals("This section is unnecessarily wordy.", request.selectedText)
    assertEquals("shorten", request.action)
    assertEquals("Keep this. ", request.contextBefore)
    assertEquals(" Keep that.", request.contextAfter)

    viewModel.applyAiSuggestion(AiSuggestion("This section is wordy.", "Shorter wording."))
    assertEquals("Keep this. This section is wordy. Keep that.", viewModel.uiState.value.text)
    assertTrue(viewModel.uiState.value.appliedAiHighlight != null)
    assertEquals(text, viewModel.uiState.value.undoText)
    assertEquals("AI suggestion is applied", viewModel.uiState.value.message)
    mainDispatcher.scheduler.advanceTimeBy(4_999)
    assertTrue(viewModel.uiState.value.appliedAiHighlight != null)
    mainDispatcher.scheduler.advanceTimeBy(1)
    mainDispatcher.scheduler.runCurrent()
    assertEquals(null, viewModel.uiState.value.appliedAiHighlight)
    mainDispatcher.scheduler.advanceUntilIdle()
  }

  @Test
  fun documentTitle_canBeSetByUser() {
    val viewModel = MainScreenViewModel(analysisDispatcher = mainDispatcher)
    viewModel.onTextChanged("The first line should not replace a custom title.")

    viewModel.renameCurrentDocument("  My essay title  ")

    assertEquals("My essay title", viewModel.uiState.value.activeDocumentTitle)
    assertEquals("My essay title", viewModel.uiState.value.savedDocuments.single().title)
  }

  @Test
  fun selectionTarget_normalizesDirectionAndTrimsOuterWhitespace() {
    val text = "Before   selected words   after"
    val selectedStart = text.indexOf("selected")
    val selectedEnd = text.indexOf("after")

    val target = rewriteTargetForSelection(text, selectedEnd, selectedStart - 2)

    assertEquals("selected words", target?.originalText)
    assertEquals(selectedStart, target?.startOffset)
    assertEquals(selectedEnd - 3, target?.endOffset)
  }

  @Test
  fun importingDocument_canRestorePreviousDraftAndFileName() {
    val viewModel = MainScreenViewModel()
    viewModel.onDocumentImported("Original text", "original.txt")
    viewModel.onDocumentImported("Replacement text", "replacement.docx")

    assertEquals("Replacement text", viewModel.uiState.value.text)
    assertEquals("Original text", viewModel.uiState.value.undoText)
    assertEquals("original.txt", viewModel.uiState.value.undoImportedFileName)

    viewModel.undoAiEdit()

    assertEquals("Original text", viewModel.uiState.value.text)
    assertEquals("original.txt", viewModel.uiState.value.importedFileName)
    assertEquals(null, viewModel.uiState.value.undoText)
  }
}

private class RecordingAiRewriteService : AiRewriteService {
  val requests = mutableListOf<AiRewriteRequest>()

  override suspend fun improve(request: AiRewriteRequest): AiRewriteResult {
    requests += request
    return AiRewriteResult.Success(
      AiRewriteResponse(
        requestId = request.requestId,
        documentRevision = request.documentRevision,
        suggestions = listOf(AiSuggestion("This section is wordy.", "Shorter wording.")),
        warnings = emptyList(),
      )
    )
  }
}
