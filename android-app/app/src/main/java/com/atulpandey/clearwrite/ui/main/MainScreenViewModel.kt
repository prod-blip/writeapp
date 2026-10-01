package com.atulpandey.clearwrite.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atulpandey.clearwrite.ai.AiRewriteRequest
import com.atulpandey.clearwrite.ai.AiRewriteResult
import com.atulpandey.clearwrite.ai.AiRewriteService
import com.atulpandey.clearwrite.ai.AiRewriteRepository
import com.atulpandey.clearwrite.ai.AiSuggestion
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.AnalysisResult
import com.atulpandey.clearwrite.analysis.WritingIssue
import com.atulpandey.clearwrite.analysis.WritingAnalyzer
import com.atulpandey.clearwrite.analysis.WritingGoal
import com.atulpandey.clearwrite.data.ClearWriteLocalStore
import com.atulpandey.clearwrite.data.ThemePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val FREE_WORD_LIMIT = 1_500
const val PRO_WORD_LIMIT = 10_000
const val MAX_AI_SELECTION_CHARACTERS = 3_000
const val MAX_AI_CUSTOM_INSTRUCTION_CHARACTERS = 240

enum class CheckState {
  IDLE,
  CHECKING,
}

enum class SaveState { IDLE, SAVING, SAVED }

enum class MessageTone { INFO, SUCCESS, ERROR }

data class AiRewriteTarget(
  val startOffset: Int,
  val endOffset: Int,
  val originalText: String,
)

data class AppliedAiHighlight(
  val startOffset: Int,
  val endOffset: Int,
  val documentRevision: Int,
)

enum class SelectionRewriteAction(val label: String, internal val apiValue: String) {
  IMPROVE_CLARITY("Improve clarity", "improve_clarity"),
  SHORTEN("Shorten", "shorten"),
  SIMPLIFY("Simplify", "simplify"),
  CHANGE_TONE("Change tone", "change_tone"),
  CUSTOM("Custom instruction", "custom_instruction"),
}

sealed interface AiRewriteState {
  data object Idle : AiRewriteState

  data class Consent(val target: AiRewriteTarget) : AiRewriteState

  data class Loading(val target: AiRewriteTarget) : AiRewriteState

  data class Ready(
    val target: AiRewriteTarget,
    val suggestions: List<AiSuggestion>,
    val warnings: List<String>,
  ) : AiRewriteState

  data class Error(val target: AiRewriteTarget, val message: String) : AiRewriteState
}

data class MainScreenUiState(
  val text: String = "",
  val importedFileName: String? = null,
  val checkState: CheckState = CheckState.IDLE,
  val analysis: AnalysisResult? = null,
  val showAnalysis: Boolean = false,
  val selectedIssueIndex: Int? = null,
  val message: String? = null,
  val messageTone: MessageTone = MessageTone.INFO,
  val documentRevision: Int = 0,
  val aiRewriteState: AiRewriteState = AiRewriteState.Idle,
  val selectionRewriteTarget: AiRewriteTarget? = null,
  val aiConsentGranted: Boolean = false,
  val appliedAiHighlight: AppliedAiHighlight? = null,
  val undoText: String? = null,
  val undoImportedFileName: String? = null,
  val saveState: SaveState = SaveState.IDLE,
  val enabledChecks: Set<AnalysisCategory> = AnalysisCategory.entries.toSet(),
  val showWordCount: Boolean = true,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val writingGoal: WritingGoal = WritingGoal.GENERAL,
  val selectionRewriteHintDismissed: Boolean = false,
  val isPro: Boolean = false,
  val ignoredIssueKeys: Set<String> = emptySet(),
) {
  val wordCount: Int = countWords(text)
  val wordLimit: Int = if (isPro) PRO_WORD_LIMIT else FREE_WORD_LIMIT
  val wordLimitExceeded: Boolean = wordCount > wordLimit
  val canCheck: Boolean = text.isNotBlank() && !wordLimitExceeded && checkState != CheckState.CHECKING
  val hasCurrentAnalysis: Boolean = analysis != null && checkState != CheckState.CHECKING
  val selectedIssue: WritingIssue? = selectedIssueIndex?.let { analysis?.issues?.getOrNull(it) }
  val selectedIssueNumber: Int
    get() {
      val current = selectedIssue ?: return 0
      return analysis?.issues?.filter { it.category == current.category }?.indexOf(current)?.plus(1) ?: 0
    }
  val selectedIssueCount: Int
    get() {
      val current = selectedIssue ?: return 0
      return analysis?.count(current.category) ?: 0
    }
  val hasPreviousIssue: Boolean = selectedIssueNumber > 1
  val hasNextIssue: Boolean = selectedIssueNumber in 1 until selectedIssueCount
}

class MainScreenViewModel(
  private val analyzer: WritingAnalyzer = WritingAnalyzer(),
  private val aiRewriteService: AiRewriteService = AiRewriteRepository(),
  private val localStore: ClearWriteLocalStore? = null,
  private val analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
  private val _uiState = MutableStateFlow(MainScreenUiState())
  val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()
  private var draftSaveJob: Job? = null
  private var analysisJob: Job? = null
  private var pendingAiRewrite: PendingAiRewrite? = null

  init {
    localStore?.let { store ->
      viewModelScope.launch {
        val stored = store.state.first()
        _uiState.update { current ->
          val canRestoreDocument = current.documentRevision == 0 && current.text.isEmpty()
          current.copy(
            text = if (canRestoreDocument) stored.documentText else current.text,
            importedFileName =
              if (canRestoreDocument) stored.importedFileName else current.importedFileName,
            enabledChecks = stored.enabledChecks,
            showWordCount = stored.showWordCount,
            aiConsentGranted = stored.aiConsentGranted,
            themePreference = stored.themePreference,
            writingGoal = stored.writingGoal,
            selectionRewriteHintDismissed = stored.selectionRewriteHintDismissed,
            saveState = if (canRestoreDocument && stored.documentText.isNotEmpty()) SaveState.SAVED else current.saveState,
          )
        }
        scheduleAnalysis(AUTO_ANALYSIS_PASTE_DELAY_MILLIS)
      }
    }
  }

  fun onTextChanged(text: String) {
    val previousText = _uiState.value.text
    _uiState.update {
      if (it.text == text) it
      else {
        it.copy(
          text = text,
          checkState = CheckState.IDLE,
          showAnalysis = false,
          selectedIssueIndex = null,
          documentRevision = it.documentRevision + 1,
          aiRewriteState = AiRewriteState.Idle,
          selectionRewriteTarget = null,
          appliedAiHighlight = null,
          undoText = null,
          undoImportedFileName = null,
          saveState = SaveState.SAVING,
          message = if (it.undoText != null) null else it.message,
          ignoredIssueKeys = emptySet(),
        )
      }
    }
    scheduleDocumentSave()
    val looksLikePaste = previousText.isBlank() || kotlin.math.abs(text.length - previousText.length) > 20
    scheduleAnalysis(
      if (looksLikePaste) AUTO_ANALYSIS_PASTE_DELAY_MILLIS else AUTO_ANALYSIS_TYPING_DELAY_MILLIS
    )
  }

  fun onDocumentImported(text: String, fileName: String) {
    _uiState.update {
      val replacingExistingDraft = it.text.isNotBlank()
      it.copy(
        text = text,
        importedFileName = fileName,
        checkState = CheckState.IDLE,
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
        message = if (replacingExistingDraft) "Imported $fileName. Your previous draft can be restored." else "Imported $fileName.",
        messageTone = MessageTone.SUCCESS,
        documentRevision = it.documentRevision + 1,
        aiRewriteState = AiRewriteState.Idle,
        selectionRewriteTarget = null,
        appliedAiHighlight = null,
        undoText = if (replacingExistingDraft) it.text else null,
        undoImportedFileName = if (replacingExistingDraft) it.importedFileName else null,
        saveState = SaveState.SAVED,
      )
    }
    persistDocumentNow(text, fileName)
    scheduleAnalysis(AUTO_ANALYSIS_PASTE_DELAY_MILLIS)
  }

  fun checkWriting() {
    scheduleAnalysis(delayMillis = 0L)
  }

  fun showAnalysis() {
    if (_uiState.value.analysis != null) _uiState.update { it.copy(showAnalysis = true) }
  }

  fun dismissAnalysis() {
    _uiState.update { it.copy(showAnalysis = false) }
  }

  fun selectFirstIssue(category: AnalysisCategory) {
    _uiState.update { current ->
      val index = current.analysis?.issues?.indexOfFirst { it.category == category } ?: -1
      if (index < 0) current else current.copy(showAnalysis = false, selectedIssueIndex = index)
    }
  }

  fun selectPreviousIssue() {
    selectAdjacentIssue(step = -1)
  }

  fun selectNextIssue() {
    selectAdjacentIssue(step = 1)
  }

  fun dismissSelectedIssue() {
    _uiState.update { it.copy(selectedIssueIndex = null, aiRewriteState = AiRewriteState.Idle) }
  }

  fun ignoreSelectedIssue() {
    _uiState.update { current ->
      val issue = current.selectedIssue ?: return@update current
      val analysis = current.analysis ?: return@update current
      val ignoredKey = issue.stableKey()
      val remainingIssues = analysis.issues.filterNot { it.stableKey() == ignoredKey }
      val nextInCategory =
        remainingIssues.firstOrNull {
          it.category == issue.category && it.startOffset >= issue.startOffset
        } ?: remainingIssues.lastOrNull { it.category == issue.category }
      current.copy(
        analysis = analysis.copy(issues = remainingIssues),
        selectedIssueIndex = nextInCategory?.let(remainingIssues::indexOf),
        ignoredIssueKeys = current.ignoredIssueKeys + ignoredKey,
        aiRewriteState = AiRewriteState.Idle,
        message = "Issue ignored for this review.",
        messageTone = MessageTone.INFO,
      )
    }
  }

  fun requestAiRewrite() {
    val current = _uiState.value
    val issue = current.selectedIssue ?: return
    val target = rewriteTargetForIssue(current.text, issue)
    if (target.originalText.isBlank()) return
    beginAiRewrite(
      target = target,
      issueType = issue.category.name,
      action = issue.category.aiAction(),
    )
  }

  fun openSelectionRewrite(startOffset: Int, endOffset: Int) {
    val current = _uiState.value
    val target = rewriteTargetForSelection(current.text, startOffset, endOffset)
    when {
      target == null ->
        _uiState.update { it.copy(message = "Select some text to rewrite.", messageTone = MessageTone.INFO) }
      target.originalText.length > MAX_AI_SELECTION_CHARACTERS ->
        _uiState.update {
          it.copy(message = "Select no more than $MAX_AI_SELECTION_CHARACTERS characters at a time.", messageTone = MessageTone.ERROR)
        }
      else ->
        _uiState.update {
          it.copy(
            selectionRewriteTarget = target,
            selectedIssueIndex = null,
            showAnalysis = false,
            message = null,
            selectionRewriteHintDismissed = true,
          )
        }
    }
    if (target != null && target.originalText.length <= MAX_AI_SELECTION_CHARACTERS) {
      viewModelScope.launch { localStore?.saveSelectionRewriteHintDismissed(true) }
    }
  }

  fun dismissSelectionRewrite() {
    _uiState.update { it.copy(selectionRewriteTarget = null) }
  }

  fun requestSelectionRewrite(
    action: SelectionRewriteAction,
    tone: String = "preserve",
    customInstruction: String = "",
  ) {
    val current = _uiState.value
    val target = current.selectionRewriteTarget ?: return
    val instruction = customInstruction.trim()
    if (action == SelectionRewriteAction.CUSTOM && instruction.isBlank()) {
      _uiState.update { it.copy(message = "Enter an instruction for the rewrite.", messageTone = MessageTone.ERROR) }
      return
    }
    if (instruction.length > MAX_AI_CUSTOM_INSTRUCTION_CHARACTERS) {
      _uiState.update {
        it.copy(message = "Keep the instruction under $MAX_AI_CUSTOM_INSTRUCTION_CHARACTERS characters.", messageTone = MessageTone.ERROR)
      }
      return
    }
    beginAiRewrite(
      target = target,
      issueType = "USER_SELECTION",
      action = action.apiValue,
      tone = if (action == SelectionRewriteAction.CHANGE_TONE) tone else "preserve",
      customInstruction = instruction,
    )
  }

  fun confirmAiConsent() {
    val waitingForConsent = _uiState.value.aiRewriteState is AiRewriteState.Consent
    if (!waitingForConsent) return
    _uiState.update { it.copy(aiConsentGranted = true, aiRewriteState = AiRewriteState.Idle) }
    viewModelScope.launch { localStore?.saveAiConsent(true) }
    submitPendingAiRewrite()
  }

  fun retryAiRewrite() {
    if (_uiState.value.aiRewriteState is AiRewriteState.Loading) return
    submitPendingAiRewrite()
  }

  fun applyAiSuggestion(suggestion: AiSuggestion) {
    var appliedHighlight: AppliedAiHighlight? = null
    _uiState.update { current ->
      val target = (current.aiRewriteState as? AiRewriteState.Ready)?.target ?: return@update current
      val existing = current.text.substringOrNull(target.startOffset, target.endOffset)
      if (existing != target.originalText) {
        return@update current.copy(
          aiRewriteState = AiRewriteState.Error(target, "The draft changed. Check it again before replacing text.")
        )
      }
      if (suggestion.text.isBlank() || suggestion.text == target.originalText) {
        return@update current.copy(
          aiRewriteState = AiRewriteState.Error(target, "Edit the suggestion before applying it.")
        )
      }
      val updatedText =
        current.text.replaceRange(target.startOffset, target.endOffset, suggestion.text)
      val updatedRevision = current.documentRevision + 1
      val highlight =
        AppliedAiHighlight(
          startOffset = target.startOffset,
          endOffset = target.startOffset + suggestion.text.length,
          documentRevision = updatedRevision,
        )
      appliedHighlight = highlight
      current.copy(
        text = updatedText,
        checkState = CheckState.IDLE,
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
        documentRevision = updatedRevision,
        aiRewriteState = AiRewriteState.Idle,
        selectionRewriteTarget = null,
        appliedAiHighlight = highlight,
        undoText = current.text,
        undoImportedFileName = current.importedFileName,
        saveState = SaveState.SAVED,
        message = "AI suggestion applied. The updated passage is marked in the draft.",
        messageTone = MessageTone.SUCCESS,
      )
    }
    appliedHighlight?.let { highlight ->
      viewModelScope.launch {
        delay(AI_APPLIED_HIGHLIGHT_MILLIS)
        _uiState.update { current ->
          if (current.appliedAiHighlight == highlight) current.copy(appliedAiHighlight = null)
          else current
        }
      }
    }
    val appliedState = _uiState.value
    if (appliedHighlight != null) {
      persistDocumentNow(appliedState.text, appliedState.importedFileName)
      scheduleAnalysis(AUTO_ANALYSIS_PASTE_DELAY_MILLIS)
    }
  }

  fun dismissAiRewrite() {
    _uiState.update { it.copy(aiRewriteState = AiRewriteState.Idle) }
    pendingAiRewrite = null
  }

  fun undoAiEdit() {
    _uiState.update { current ->
      val previous = current.undoText ?: return@update current
      current.copy(
        text = previous,
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
        documentRevision = current.documentRevision + 1,
        appliedAiHighlight = null,
        undoText = null,
        importedFileName = current.undoImportedFileName,
        undoImportedFileName = null,
        saveState = SaveState.SAVED,
        message = "Last change undone.",
        messageTone = MessageTone.SUCCESS,
      )
    }
    val restoredState = _uiState.value
    persistDocumentNow(restoredState.text, restoredState.importedFileName)
    scheduleAnalysis(AUTO_ANALYSIS_PASTE_DELAY_MILLIS)
  }

  fun startNewDocument() {
    draftSaveJob?.cancel()
    analysisJob?.cancel()
    val current = _uiState.value
    _uiState.value =
      MainScreenUiState(
        enabledChecks = current.enabledChecks,
        showWordCount = current.showWordCount,
        aiConsentGranted = current.aiConsentGranted,
        themePreference = current.themePreference,
        writingGoal = current.writingGoal,
        selectionRewriteHintDismissed = current.selectionRewriteHintDismissed,
        isPro = current.isPro,
      )
    viewModelScope.launch { localStore?.clearDocument() }
  }

  fun setCheckEnabled(category: AnalysisCategory, enabled: Boolean) {
    _uiState.update { current ->
      val updated =
        if (enabled) current.enabledChecks + category else current.enabledChecks - category
      current.copy(
        enabledChecks = updated,
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
      )
    }
    val enabledChecks = _uiState.value.enabledChecks
    viewModelScope.launch { localStore?.saveEnabledChecks(enabledChecks) }
    scheduleAnalysis(AUTO_ANALYSIS_SETTINGS_DELAY_MILLIS)
  }

  fun enableAllChecks() {
    _uiState.update {
      it.copy(
        enabledChecks = AnalysisCategory.entries.toSet(),
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
      )
    }
    viewModelScope.launch { localStore?.saveEnabledChecks(AnalysisCategory.entries.toSet()) }
    scheduleAnalysis(AUTO_ANALYSIS_SETTINGS_DELAY_MILLIS)
  }

  fun setShowWordCount(show: Boolean) {
    _uiState.update { it.copy(showWordCount = show) }
    viewModelScope.launch { localStore?.saveShowWordCount(show) }
  }

  fun setThemePreference(preference: ThemePreference) {
    _uiState.update { it.copy(themePreference = preference) }
    viewModelScope.launch { localStore?.saveThemePreference(preference) }
  }

  fun setWritingGoal(goal: WritingGoal) {
    _uiState.update {
      it.copy(
        writingGoal = goal,
        analysis = null,
        showAnalysis = false,
        selectedIssueIndex = null,
      )
    }
    viewModelScope.launch { localStore?.saveWritingGoal(goal) }
    scheduleAnalysis(AUTO_ANALYSIS_SETTINGS_DELAY_MILLIS)
  }

  fun setProEntitled(isPro: Boolean) {
    val changed = _uiState.value.isPro != isPro
    _uiState.update { it.copy(isPro = isPro) }
    if (changed) scheduleAnalysis(AUTO_ANALYSIS_SETTINGS_DELAY_MILLIS)
  }

  fun resetAiConsent() {
    _uiState.update { it.copy(aiConsentGranted = false, aiRewriteState = AiRewriteState.Idle) }
    viewModelScope.launch { localStore?.saveAiConsent(false) }
  }

  fun showMessage(message: String) {
    _uiState.update { it.copy(message = message, messageTone = MessageTone.ERROR) }
  }

  fun clearMessage() {
    _uiState.update { it.copy(message = null) }
  }

  fun dismissSelectionRewriteHint() {
    _uiState.update { it.copy(selectionRewriteHintDismissed = true) }
    viewModelScope.launch { localStore?.saveSelectionRewriteHintDismissed(true) }
  }

  private fun selectAdjacentIssue(step: Int) {
    _uiState.update { current ->
      val selected = current.selectedIssue ?: return@update current
      val categoryIndices =
        current.analysis?.issues?.indices?.filter {
          current.analysis.issues[it].category == selected.category
        }.orEmpty()
      val position = categoryIndices.indexOf(current.selectedIssueIndex)
      val targetIndex = categoryIndices.getOrNull(position + step) ?: return@update current
      current.copy(selectedIssueIndex = targetIndex)
    }
  }

  private fun beginAiRewrite(
    target: AiRewriteTarget,
    issueType: String,
    action: String,
    tone: String = "preserve",
    customInstruction: String = "",
  ) {
    val current = _uiState.value
    if (current.aiRewriteState is AiRewriteState.Loading) return
    val draft = current.text
    if (draft.substringOrNull(target.startOffset, target.endOffset) != target.originalText) {
      _uiState.update {
        it.copy(
          selectionRewriteTarget = null,
          message = "The draft changed. Select the text again.",
          messageTone = MessageTone.ERROR,
        )
      }
      return
    }
    val request =
      AiRewriteRequest(
        documentRevision = current.documentRevision,
        selectedText = target.originalText,
        contextBefore = draft.substring(0, target.startOffset).takeLast(CONTEXT_CHARACTER_LIMIT),
        contextAfter = draft.substring(target.endOffset).take(CONTEXT_CHARACTER_LIMIT),
        issueType = issueType,
        action = action,
        tone = tone,
        customInstruction = customInstruction,
      )
    pendingAiRewrite = PendingAiRewrite(target = target, request = request, draft = draft)
    _uiState.update { it.copy(selectionRewriteTarget = null) }
    if (!current.aiConsentGranted) {
      _uiState.update { it.copy(aiRewriteState = AiRewriteState.Consent(target)) }
    } else {
      submitPendingAiRewrite()
    }
  }

  private fun submitPendingAiRewrite() {
    val pending = pendingAiRewrite ?: return
    val current = _uiState.value
    if (
      current.documentRevision != pending.request.documentRevision ||
        current.text != pending.draft ||
        current.text.substringOrNull(pending.target.startOffset, pending.target.endOffset) !=
          pending.target.originalText
    ) {
      _uiState.update {
        it.copy(
          aiRewriteState =
            AiRewriteState.Error(
              pending.target,
              "The draft changed. Select the text again before rewriting it.",
            )
        )
      }
      return
    }

    _uiState.update { it.copy(aiRewriteState = AiRewriteState.Loading(pending.target)) }
    viewModelScope.launch {
      val result = aiRewriteService.improve(pending.request)
      _uiState.update { latest ->
        val stillCurrent =
          latest.documentRevision == pending.request.documentRevision &&
            latest.text == pending.draft &&
            latest.aiRewriteState is AiRewriteState.Loading
        if (!stillCurrent) return@update latest

        when (result) {
          is AiRewriteResult.Success ->
            latest.copy(
              aiRewriteState =
                AiRewriteState.Ready(
                  target = pending.target,
                  suggestions = result.response.suggestions,
                  warnings = result.response.warnings,
                )
            )
          is AiRewriteResult.Failure ->
            latest.copy(aiRewriteState = AiRewriteState.Error(pending.target, result.message))
        }
      }
    }
  }

  private fun scheduleDocumentSave() {
    val snapshot = _uiState.value
    draftSaveJob?.cancel()
    draftSaveJob =
      viewModelScope.launch {
        delay(DRAFT_SAVE_DEBOUNCE_MILLIS)
        localStore?.saveDocument(snapshot.text, snapshot.importedFileName)
        _uiState.update { current ->
          if (current.documentRevision == snapshot.documentRevision && current.text == snapshot.text) {
            current.copy(saveState = SaveState.SAVED)
          } else current
        }
      }
  }

  private fun scheduleAnalysis(delayMillis: Long) {
    analysisJob?.cancel()
    val snapshot = _uiState.value
    if (snapshot.text.isBlank() || snapshot.wordLimitExceeded) {
      _uiState.update {
        it.copy(
          checkState = CheckState.IDLE,
          analysis = null,
          showAnalysis = false,
          selectedIssueIndex = null,
        )
      }
      return
    }

    val draft = snapshot.text
    val revision = snapshot.documentRevision
    val enabledChecks = snapshot.enabledChecks
    val writingGoal = snapshot.writingGoal
    _uiState.update {
      it.copy(
        checkState = CheckState.CHECKING,
        showAnalysis = false,
        selectedIssueIndex = null,
      )
    }
    analysisJob =
      viewModelScope.launch {
        delay(delayMillis)
        val result = withContext(analysisDispatcher) {
          analyzer.analyze(draft, enabledChecks, writingGoal)
        }
        _uiState.update { current ->
          if (
            current.documentRevision != revision ||
              current.text != draft ||
              current.enabledChecks != enabledChecks ||
              current.writingGoal != writingGoal
          ) {
            current
          } else {
            val visibleResult = result.copy(
              issues = result.issues.filterNot { it.stableKey() in current.ignoredIssueKeys },
            )
            current.copy(
              checkState = CheckState.IDLE,
              analysis = visibleResult,
              showAnalysis = false,
              selectedIssueIndex = null,
            )
          }
        }
      }
  }

  private fun persistDocumentNow(text: String, importedFileName: String?) {
    val store = localStore ?: return
    draftSaveJob?.cancel()
    viewModelScope.launch { store.saveDocument(text, importedFileName) }
  }
}

private data class PendingAiRewrite(
  val target: AiRewriteTarget,
  val request: AiRewriteRequest,
  val draft: String,
)

internal fun rewriteTargetForIssue(text: String, issue: WritingIssue): AiRewriteTarget {
  val issueStart = issue.startOffset.coerceIn(0, text.length)
  val issueEnd = issue.endOffset.coerceIn(issueStart, text.length)
  val useIssueRange =
    issue.category == AnalysisCategory.LONG_SENTENCE ||
      issue.category == AnalysisCategory.HARD_SENTENCE ||
      issue.category == AnalysisCategory.VERY_HARD_SENTENCE ||
      issue.category == AnalysisCategory.LONG_PARAGRAPH ||
      issue.category == AnalysisCategory.FILLER_HEAVY_SENTENCE

  var start = if (useIssueRange) issueStart else sentenceStart(text, issueStart)
  var end = if (useIssueRange) issueEnd else sentenceEnd(text, issueEnd)
  while (start < end && text[start].isWhitespace()) start += 1
  while (end > start && text[end - 1].isWhitespace()) end -= 1
  return AiRewriteTarget(start, end, text.substring(start, end))
}

internal fun rewriteTargetForSelection(
  text: String,
  firstOffset: Int,
  secondOffset: Int,
): AiRewriteTarget? {
  var start = minOf(firstOffset, secondOffset).coerceIn(0, text.length)
  var end = maxOf(firstOffset, secondOffset).coerceIn(start, text.length)
  while (start < end && text[start].isWhitespace()) start += 1
  while (end > start && text[end - 1].isWhitespace()) end -= 1
  if (start >= end) return null
  return AiRewriteTarget(start, end, text.substring(start, end))
}

private fun sentenceStart(text: String, offset: Int): Int {
  var index = (offset - 1).coerceAtLeast(0)
  while (index > 0 && text[index - 1] !in sentenceTerminators) index -= 1
  return index
}

private fun sentenceEnd(text: String, offset: Int): Int {
  var index = offset.coerceIn(0, text.length)
  while (index < text.length && text[index] !in sentenceTerminators) index += 1
  return if (index < text.length) index + 1 else index
}

private fun AnalysisCategory.aiAction(): String =
  when (this) {
    AnalysisCategory.LONG_SENTENCE -> "split_and_simplify"
    AnalysisCategory.HARD_SENTENCE,
    AnalysisCategory.VERY_HARD_SENTENCE -> "simplify_readability"
    AnalysisCategory.LONG_PARAGRAPH -> "improve_paragraph_structure"
    AnalysisCategory.PASSIVE_VOICE -> "use_clear_active_voice"
    AnalysisCategory.WEAKENER -> "strengthen_wording"
    AnalysisCategory.WORDY_PHRASE,
    AnalysisCategory.FILLER_HEAVY_SENTENCE -> "make_concise"
    AnalysisCategory.REPEATED_START,
    AnalysisCategory.REPEATED_WORD -> "remove_repetition"
  }

private fun String.substringOrNull(start: Int, end: Int): String? =
  if (start in indices || (start == length && start == end)) {
    if (end in start..length) substring(start, end) else null
  } else null

private val sentenceTerminators = setOf('.', '!', '?')

private fun WritingIssue.stableKey(): String =
  "${category.name}:$startOffset:$endOffset"
private const val CONTEXT_CHARACTER_LIMIT = 500
private const val AI_APPLIED_HIGHLIGHT_MILLIS = 2_500L
private const val DRAFT_SAVE_DEBOUNCE_MILLIS = 350L
private const val AUTO_ANALYSIS_PASTE_DELAY_MILLIS = 300L
private const val AUTO_ANALYSIS_TYPING_DELAY_MILLIS = 1_000L
private const val AUTO_ANALYSIS_SETTINGS_DELAY_MILLIS = 150L

internal fun countWords(text: String): Int =
  text.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+"))?.size ?: 0
