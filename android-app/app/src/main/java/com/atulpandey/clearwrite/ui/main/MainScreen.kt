package com.atulpandey.clearwrite.ui.main

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atulpandey.clearwrite.BuildConfig
import com.atulpandey.clearwrite.R
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.AnalysisResult
import com.atulpandey.clearwrite.analysis.WritingIssue
import com.atulpandey.clearwrite.ai.AiSuggestion
import com.atulpandey.clearwrite.billing.PlayBillingManager
import com.atulpandey.clearwrite.data.DocumentImporter
import com.atulpandey.clearwrite.data.ImportResult
import com.atulpandey.clearwrite.theme.ClearWriteTheme
import com.atulpandey.clearwrite.theme.ClearWriteThemeTokens
import com.atulpandey.clearwrite.theme.ClearWriteElevation
import com.atulpandey.clearwrite.theme.ClearWriteSpacing
import com.atulpandey.clearwrite.theme.ClearWriteType
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

private val supportedDocumentTypes =
  arrayOf(
    "text/plain",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  )

private fun readClipboardText(context: android.content.Context, clipboard: ClipboardManager): String? =
  runCatching {
    val clip = clipboard.primaryClip ?: return@runCatching null
    if (clip.itemCount == 0) return@runCatching null
    clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
  }.getOrNull()

private val ANALYSIS_PEEK_HEIGHT = 72.dp

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() },
  billingManager: PlayBillingManager? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val billingState by
    (billingManager?.state ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.atulpandey.clearwrite.billing.BillingUiState()) })
      .collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showImportSheet by remember { mutableStateOf(false) }
  var menuDestination by remember { mutableStateOf<MenuDestination?>(null) }
  var clearDocumentReason by remember { mutableStateOf<ClearDocumentReason?>(null) }
  var pendingImport by remember { mutableStateOf<ImportResult.Success?>(null) }
  var renameDocument by remember { mutableStateOf(false) }
  var pendingDocumentTitle by remember { mutableStateOf("") }

  LaunchedEffect(billingState.isPro) {
    viewModel.setProEntitled(billingState.isPro)
  }

  val documentPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        scope.launch {
          when (val result = DocumentImporter.import(context, uri)) {
            is ImportResult.Success -> {
              if (uiState.text.isNotBlank()) pendingImport = result
              else viewModel.onDocumentImported(result.text, result.fileName)
            }
            is ImportResult.Failure -> viewModel.showMessage(result.message)
          }
        }
      }
    }

  EditorScreen(
    uiState = uiState,
    onTextChanged = viewModel::onTextChanged,
    onPasteUnavailable = viewModel::showMessage,
    onImportClick = { showImportSheet = true },
    onShowAnalysis = viewModel::showAnalysis,
    onDismissAnalysis = viewModel::dismissAnalysis,
    onIssueCategorySelected = viewModel::selectFirstIssue,
    onPreviousIssue = viewModel::selectPreviousIssue,
    onNextIssue = viewModel::selectNextIssue,
    onDismissIssue = viewModel::dismissSelectedIssue,
    onIgnoreIssue = viewModel::ignoreSelectedIssue,
    onImproveWithAi = {
      if (BuildConfig.AI_DEVELOPMENT_ACCESS || billingState.isPro) viewModel.requestAiRewrite()
      else menuDestination = MenuDestination.UPGRADE
    },
    onSelectionRewriteRequested = viewModel::openSelectionRewrite,
    onSelectionRewriteAction = { action, tone, instruction ->
      if (BuildConfig.AI_DEVELOPMENT_ACCESS || billingState.isPro) {
        viewModel.requestSelectionRewrite(action, tone, instruction)
      } else {
        viewModel.dismissSelectionRewrite()
        menuDestination = MenuDestination.UPGRADE
      }
    },
    onDismissSelectionRewrite = viewModel::dismissSelectionRewrite,
    onApplyAiSuggestion = viewModel::applyAiSuggestion,
    onConfirmAiConsent = viewModel::confirmAiConsent,
    onRetryAi = viewModel::retryAiRewrite,
    onDismissAi = viewModel::dismissAiRewrite,
    onUndoAiEdit = viewModel::undoAiEdit,
    onRenameDocument = {
      pendingDocumentTitle = uiState.activeDocumentTitle
      renameDocument = true
    },
    onMenuDestinationSelected = { menuDestination = it },
    onMessageShown = viewModel::clearMessage,
    modifier = modifier,
  )

  if (showImportSheet) {
    ImportDocumentSheet(
      onDismiss = { showImportSheet = false },
      onChooseFile = {
        showImportSheet = false
        documentPicker.launch(supportedDocumentTypes)
      },
    )
  }

  menuDestination?.let { destination ->
    AppMenuSheet(
      destination = destination,
      uiState = uiState,
      onDismiss = { menuDestination = null },
      onDeleteDocument = { clearDocumentReason = ClearDocumentReason.DELETE_SAVED },
      onCheckEnabledChanged = viewModel::setCheckEnabled,
      onEnableAllChecks = viewModel::enableAllChecks,
      onShowWordCountChanged = viewModel::setShowWordCount,
      onThemePreferenceChanged = viewModel::setThemePreference,
      onWritingGoalChanged = viewModel::setWritingGoal,
      onResetAiConsent = viewModel::resetAiConsent,
      onImportDocument = {
        menuDestination = null
        showImportSheet = true
      },
      onStartNewDocument = {
        menuDestination = null
        if (uiState.isPro) viewModel.startNewDocument()
        else clearDocumentReason = ClearDocumentReason.START_NEW
      },
      onOpenDocument = { documentId ->
        viewModel.openDocument(documentId)
        menuDestination = null
      },
      onOpenUpgrade = { menuDestination = MenuDestination.UPGRADE },
      billingState = billingState,
      onPurchase = { offerToken ->
        val activity = context.findActivity()
        if (activity == null) viewModel.showMessage("Could not open Google Play Billing.")
        else billingManager?.launchPurchase(activity, offerToken)
      },
      onRestorePurchase = { billingManager?.restorePurchases() },
      onReviewerAccess = { code -> billingManager?.activateReviewerAccess(code) },
    )
  }

  pendingImport?.let { imported ->
    AlertDialog(
      onDismissRequest = { pendingImport = null },
      title = { Text("Replace the current document?") },
      text = {
        Text("Importing ${imported.fileName} will replace the draft in the editor. You can undo this immediately afterward.")
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onDocumentImported(imported.text, imported.fileName)
          pendingImport = null
        }) { Text("Replace and import") }
      },
      dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
    )
  }

  clearDocumentReason?.let { reason ->
    AlertDialog(
      onDismissRequest = { clearDocumentReason = null },
      title = {
        Text(if (reason == ClearDocumentReason.START_NEW) "Start a new document?" else "Delete saved document?")
      },
      text = {
        Text(
          if (reason == ClearDocumentReason.START_NEW) {
            "This replaces your current saved document. This action cannot be undone."
          } else {
            "This permanently removes the document stored on this device."
          }
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (reason == ClearDocumentReason.DELETE_SAVED && uiState.isPro) {
              viewModel.deleteCurrentDocument()
            } else {
              viewModel.startNewDocument()
            }
            clearDocumentReason = null
            menuDestination = null
          }
        ) {
          Text(if (reason == ClearDocumentReason.START_NEW) "Start new" else "Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { clearDocumentReason = null }) { Text("Cancel") }
      },
    )
  }

  if (renameDocument) {
    AlertDialog(
      onDismissRequest = { renameDocument = false },
      title = { Text("Document title") },
      text = {
        OutlinedTextField(
          value = pendingDocumentTitle,
          onValueChange = { pendingDocumentTitle = it.take(MAX_DOCUMENT_TITLE_CHARACTERS) },
          label = { Text("Title") },
          singleLine = true,
          supportingText = { Text("${pendingDocumentTitle.length}/$MAX_DOCUMENT_TITLE_CHARACTERS") },
        )
      },
      confirmButton = {
        TextButton(
          enabled = pendingDocumentTitle.isNotBlank(),
          onClick = {
            viewModel.renameCurrentDocument(pendingDocumentTitle)
            renameDocument = false
          },
        ) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { renameDocument = false }) { Text("Cancel") } },
    )
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private enum class ClearDocumentReason {
  START_NEW,
  DELETE_SAVED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
  uiState: MainScreenUiState,
  onTextChanged: (String) -> Unit,
  onPasteUnavailable: (String) -> Unit,
  onImportClick: () -> Unit,
  onShowAnalysis: () -> Unit,
  onDismissAnalysis: () -> Unit,
  onIssueCategorySelected: (AnalysisCategory) -> Unit,
  onPreviousIssue: () -> Unit,
  onNextIssue: () -> Unit,
  onDismissIssue: () -> Unit,
  onIgnoreIssue: () -> Unit = {},
  onImproveWithAi: () -> Unit,
  onSelectionRewriteRequested: (Int, Int) -> Unit,
  onSelectionRewriteAction: (SelectionRewriteAction, String, String) -> Unit,
  onDismissSelectionRewrite: () -> Unit,
  onApplyAiSuggestion: (AiSuggestion) -> Unit,
  onConfirmAiConsent: () -> Unit,
  onRetryAi: () -> Unit,
  onDismissAi: () -> Unit,
  onUndoAiEdit: () -> Unit,
  onRenameDocument: () -> Unit,
  onMenuDestinationSelected: (MenuDestination) -> Unit,
  onMessageShown: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val editorFocusRequester = remember { FocusRequester() }
  val editorScrollState = rememberScrollState()
  val analysisPeekHeight = remember(density.fontScale) {
    (ANALYSIS_PEEK_HEIGHT.value + ((density.fontScale - 1f).coerceAtLeast(0f) * 36f)).dp
  }
  var editorValue by
    remember(uiState.appliedAiHighlight?.documentRevision) {
      mutableStateOf(TextFieldValue(uiState.text))
    }
  var overflowOpen by remember { mutableStateOf(false) }
  var emphasizeIssueHighlight by remember { mutableStateOf(false) }
  var saveStatusVisible by remember { mutableStateOf(false) }
  var lastRewriteSelection by remember { mutableStateOf<TextRange?>(null) }
  var editorLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  var pendingEditorScrollOffset by remember { mutableStateOf<Int?>(null) }
  val analysisSheetState =
    rememberStandardBottomSheetState(
      initialValue = if (uiState.showAnalysis) SheetValue.Expanded else SheetValue.PartiallyExpanded,
      skipHiddenState = true,
    )
  val analysisScaffoldState = rememberBottomSheetScaffoldState(analysisSheetState)
  val issueHighlightAlpha by animateFloatAsState(
    targetValue = if (emphasizeIssueHighlight) 0.30f else 0.18f,
    animationSpec = tween(durationMillis = 260),
    label = "issue highlight emphasis",
  )
  val issueHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = issueHighlightAlpha)
  val appliedAiHighlightColor = ClearWriteThemeTokens.colors.success.copy(alpha = 0.28f)
  val editorHighlight =
    remember(
      uiState.selectedIssue,
      uiState.appliedAiHighlight,
      issueHighlightColor,
      appliedAiHighlightColor,
    ) {
      editorHighlightTransformation(
        issue = uiState.selectedIssue,
        appliedAiHighlight = uiState.appliedAiHighlight,
        issueColor = issueHighlightColor,
        appliedAiColor = appliedAiHighlightColor,
      )
    }
  val dismissKeyboard = {
    keyboardController?.hide()
    focusManager.clearFocus()
  }
  val dismissKeyboardOnScroll =
    remember(focusManager, keyboardController, density) {
      var accumulatedDistance = 0f
      var keyboardDismissed = false
      val dismissThreshold = with(density) { 18.dp.toPx() }
      object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          if (source == NestedScrollSource.UserInput && !keyboardDismissed) {
            accumulatedDistance += abs(available.y)
          }
          if (accumulatedDistance >= dismissThreshold && !keyboardDismissed) {
            keyboardController?.hide()
            focusManager.clearFocus()
            keyboardDismissed = true
          }
          return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
          accumulatedDistance = 0f
          keyboardDismissed = false
          return Velocity.Zero
        }
      }
    }

  LaunchedEffect(uiState.selectedIssue?.category, uiState.selectedIssue?.startOffset) {
    if (uiState.selectedIssue != null) {
      emphasizeIssueHighlight = true
      delay(220)
      emphasizeIssueHighlight = false
    }
  }

  LaunchedEffect(uiState.saveState) {
    when (uiState.saveState) {
      SaveState.IDLE -> saveStatusVisible = false
      SaveState.SAVING -> saveStatusVisible = true
      SaveState.SAVED -> {
        saveStatusVisible = true
        delay(1_400)
        saveStatusVisible = false
      }
    }
  }

  LaunchedEffect(uiState.message, uiState.messageTone) {
    if (uiState.message != null && uiState.messageTone != MessageTone.ERROR) {
      delay(5_000)
      onMessageShown()
    }
  }

  LaunchedEffect(editorValue.selection, uiState.documentRevision, uiState.selectionRewriteTarget) {
    if (!editorValue.selection.collapsed) {
      lastRewriteSelection = editorValue.selection
    } else if (uiState.selectionRewriteTarget == null) {
      delay(300)
      if (editorValue.selection.collapsed) lastRewriteSelection = null
    }
  }

  LaunchedEffect(uiState.text, uiState.selectedIssue, uiState.appliedAiHighlight) {
    val highlightedStart =
      uiState.appliedAiHighlight?.startOffset ?: uiState.selectedIssue?.startOffset
    if (highlightedStart != null) {
      val start = highlightedStart.coerceIn(0, uiState.text.length)
      // Keep the selection collapsed so Android does not draw its selection color over
      // the custom background. The cursor position still brings the highlight into view.
      editorValue = TextFieldValue(text = uiState.text, selection = TextRange(start))
      pendingEditorScrollOffset = start
      keyboardController?.hide()
      focusManager.clearFocus()
    } else if (editorValue.text != uiState.text) {
      editorValue = TextFieldValue(text = uiState.text, selection = TextRange(uiState.text.length))
    }
  }

  LaunchedEffect(uiState.showAnalysis, uiState.analysis, uiState.wordLimitExceeded) {
    if (uiState.showAnalysis && uiState.analysis != null && !uiState.wordLimitExceeded) {
      analysisSheetState.expand()
    } else {
      analysisSheetState.partialExpand()
    }
  }

  LaunchedEffect(analysisSheetState) {
    snapshotFlow { analysisSheetState.currentValue }
      .distinctUntilChanged()
      .collect { value ->
        if (value == SheetValue.Expanded) {
          keyboardController?.hide()
          focusManager.clearFocus()
          onShowAnalysis()
        } else {
          onDismissAnalysis()
        }
      }
  }

  BottomSheetScaffold(
    scaffoldState = analysisScaffoldState,
    modifier = modifier.fillMaxSize().imePadding().navigationBarsPadding(),
    containerColor = MaterialTheme.colorScheme.background,
    sheetPeekHeight = if (uiState.text.isBlank()) 0.dp else analysisPeekHeight,
    sheetDragHandle = null,
    sheetShape = MaterialTheme.shapes.extraLarge,
    // A barely tinted peek makes the collapsed analysis affordance feel separate from
    // the editor. Expanded analysis returns to the normal surface for calmer reading.
    sheetContainerColor =
      if (uiState.showAnalysis) MaterialTheme.colorScheme.surface
      else MaterialTheme.colorScheme.surfaceContainerLow,
    sheetShadowElevation = ClearWriteElevation.overlay,
    sheetContent = {
      if (uiState.text.isNotBlank()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
          AnalysisPanel(
            uiState = uiState,
            peekHeight = analysisPeekHeight,
            expanded = analysisSheetState.currentValue == SheetValue.Expanded,
            modifier = Modifier.widthIn(max = 760.dp),
            onToggle = {
              dismissKeyboard()
              if (analysisSheetState.currentValue == SheetValue.Expanded) {
                onDismissAnalysis()
                scope.launch { analysisSheetState.partialExpand() }
              } else if (uiState.analysis != null && !uiState.wordLimitExceeded) {
                onShowAnalysis()
                scope.launch { analysisSheetState.expand() }
              }
            },
            onIssueCategorySelected = { category ->
              scope.launch { analysisSheetState.partialExpand() }
              onIssueCategorySelected(category)
            },
          )
        }
      }
    },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(R.drawable.ic_clearwrite_brand),
              contentDescription = null,
              modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)),
            )
            Spacer(Modifier.width(ClearWriteSpacing.small))
            Text(text = "ClearWrite", style = MaterialTheme.typography.titleLarge)
            AnimatedVisibility(
              visible = saveStatusVisible,
              enter = fadeIn(tween(160)),
              exit = fadeOut(tween(180)),
            ) {
              Row(
                modifier = Modifier.padding(start = ClearWriteSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (uiState.saveState == SaveState.SAVED) {
                  ClearWriteIconGraphic(
                    ClearWriteIcon.CHECK,
                    ClearWriteThemeTokens.colors.success,
                    Modifier.size(14.dp),
                  )
                  Spacer(Modifier.width(ClearWriteSpacing.xSmall))
                }
                Text(
                  text = if (uiState.saveState == SaveState.SAVING) "Saving…" else "Saved",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        },
        actions = {
          IconButton(
            onClick = {
              dismissKeyboard()
              onImportClick()
            },
            modifier = Modifier.semantics { contentDescription = "Import document" },
          ) {
            ClearWriteIconGraphic(ClearWriteIcon.IMPORT, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
          }
          Box {
            IconButton(
              onClick = {
                dismissKeyboard()
                overflowOpen = true
              },
              modifier = Modifier.semantics { contentDescription = "More options" },
            ) {
              ClearWriteIconGraphic(ClearWriteIcon.MORE, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
            }
            OverflowMenu(
              expanded = overflowOpen,
              onDismiss = { overflowOpen = false },
              onDestinationSelected = { destination ->
                overflowOpen = false
                onMenuDestinationSelected(destination)
              },
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
  ) { contentPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(contentPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(horizontal = ClearWriteSpacing.xLarge),
      ) {
      if (uiState.isPro) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.padding(bottom = ClearWriteSpacing.small).clickable(onClick = onRenameDocument),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ClearWriteSpacing.medium, vertical = ClearWriteSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ClearWriteIconGraphic(ClearWriteIcon.DOCUMENT, MaterialTheme.colorScheme.primary, Modifier.size(16.dp))
            Text(
              text = uiState.activeDocumentTitle,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(start = ClearWriteSpacing.small).weight(1f),
            )
            Text("Rename", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      } else if (uiState.importedFileName != null) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.padding(bottom = ClearWriteSpacing.small),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = ClearWriteSpacing.medium, vertical = ClearWriteSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ClearWriteIconGraphic(
              ClearWriteIcon.DOCUMENT,
              MaterialTheme.colorScheme.primary,
              Modifier.size(16.dp),
            )
            Text(
              text = uiState.importedFileName,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(start = ClearWriteSpacing.small),
            )
          }
        }
      }

      BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
        val editorViewportHeight = maxHeight
        LaunchedEffect(pendingEditorScrollOffset, editorViewportHeight) {
          val requestedOffset = pendingEditorScrollOffset ?: return@LaunchedEffect
          if (uiState.selectedIssue != null) delay(240)
          val viewportHeightPx = with(density) { editorViewportHeight.toPx() }
          val (layoutResult, maxScroll) =
            snapshotFlow { editorLayoutResult to editorScrollState.maxValue }
              .first { (layout, maxValue) ->
                layout != null && (maxValue > 0 || layout.size.height <= viewportHeightPx)
              }
          val layout = layoutResult ?: return@LaunchedEffect
          val safeOffset = requestedOffset.coerceIn(0, layout.layoutInput.text.length)
          val cursorTop = layout.getCursorRect(safeOffset).top
          editorScrollState.animateScrollTo(
            editorScrollTarget(cursorTop, viewportHeightPx, maxScroll),
          )
          if (pendingEditorScrollOffset == requestedOffset) pendingEditorScrollOffset = null
        }

        BasicTextField(
          value = editorValue,
          onValueChange = { updatedValue ->
            editorValue = updatedValue
            if (updatedValue.text != uiState.text) onTextChanged(updatedValue.text)
          },
          textStyle = ClearWriteType.editor.copy(color = MaterialTheme.colorScheme.onBackground),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          visualTransformation = editorHighlight,
          onTextLayout = { editorLayoutResult = it },
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = editorViewportHeight)
              .nestedScroll(dismissKeyboardOnScroll)
              .verticalScroll(editorScrollState)
              .padding(end = 8.dp)
              .focusRequester(editorFocusRequester)
              .semantics { contentDescription = "Writing editor" },
          decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {
              if (uiState.text.isEmpty()) {
                Column {
                  Text(
                    text = "Paste or type your text here…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = ClearWriteType.editor,
                  )
                  Text(
                    text = "Your clarity check appears automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = ClearWriteSpacing.small),
                  )
                }
              }
              innerTextField()
            }
          },
        )

        if (uiState.text.isEmpty()) {
          FilledTonalButton(
            onClick = {
              val pastedText = readClipboardText(context, clipboard)
              if (pastedText != null) {
                editorValue = TextFieldValue(
                  text = pastedText,
                  selection = TextRange(pastedText.length),
                )
                onTextChanged(pastedText)
                dismissKeyboard()
              } else {
                onPasteUnavailable("There is no text on your clipboard to paste.")
              }
            },
            modifier =
              Modifier.align(Alignment.TopStart)
                .padding(top = 68.dp),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Paste")
          }
        }

        val rewriteSelection = lastRewriteSelection
        if (
          rewriteSelection != null &&
            uiState.selectionRewriteTarget == null &&
            uiState.aiRewriteState is AiRewriteState.Idle
        ) {
          Button(
            onClick = {
              val selectionStart = minOf(rewriteSelection.start, rewriteSelection.end)
              val selectionEnd = maxOf(rewriteSelection.start, rewriteSelection.end)
              onSelectionRewriteRequested(selectionStart, selectionEnd)
              lastRewriteSelection = null
              keyboardController?.hide()
              focusManager.clearFocus(force = true)
            },
            modifier =
              Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .semantics { contentDescription = "Rewrite selected text with AI" },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClearWriteThemeTokens.colors.aiAccent,
              contentColor = MaterialTheme.colorScheme.surface,
            ),
          ) {
            Text("Rewrite with AI · Pro")
          }
        }

        EditorScrollbar(
          scrollState = editorScrollState,
          modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(3.dp),
        )
      }

      AnimatedVisibility(
        visible = uiState.selectedIssue != null,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 4 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 5 },
      ) {
        SelectedIssueCard(
          uiState = uiState,
          onPrevious = {
            dismissKeyboard()
            onPreviousIssue()
          },
          onNext = {
            dismissKeyboard()
            onNextIssue()
          },
          onDismiss = {
            dismissKeyboard()
            onDismissIssue()
          },
          onEditInDraft = {
            val issue = uiState.selectedIssue ?: return@SelectedIssueCard
            val offset = issue.startOffset.coerceIn(0, uiState.text.length)
            editorValue = TextFieldValue(uiState.text, TextRange(offset))
            editorFocusRequester.requestFocus()
            keyboardController?.show()
          },
          onIgnore = {
            dismissKeyboard()
            onIgnoreIssue()
          },
          onImproveWithAi = {
            dismissKeyboard()
            onImproveWithAi()
          },
        )
      }

      AnimatedVisibility(
        visible = uiState.message != null,
        enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it / 3 },
        exit = fadeOut(tween(140)),
      ) {
        InlineMessage(
          message = uiState.message.orEmpty(),
          actionLabel = if (uiState.undoText != null) "Undo" else null,
          tone = uiState.messageTone,
          onAction = onUndoAiEdit,
          onDismiss = onMessageShown,
        )
      }
      }
    }
  }

  uiState.selectionRewriteTarget?.let { target ->
    SelectionRewriteSheet(
      target = target,
      onRewrite = onSelectionRewriteAction,
      onDismiss = onDismissSelectionRewrite,
    )
  }

  if (uiState.aiRewriteState !is AiRewriteState.Idle) {
    AiRewriteSheet(
      state = uiState.aiRewriteState,
      onApplySuggestion = { suggestion ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onApplyAiSuggestion(suggestion)
      },
      onConfirmConsent = onConfirmAiConsent,
      onRetry = onRetryAi,
      onDismiss = onDismissAi,
    )
  }
}

internal fun issueHighlightTransformation(
  issue: WritingIssue?,
  highlightColor: Color,
): VisualTransformation =
  editorHighlightTransformation(
    issue = issue,
    appliedAiHighlight = null,
    issueColor = highlightColor,
    appliedAiColor = highlightColor,
  )

internal fun editorScrollTarget(
  highlightedTopPx: Float,
  viewportHeightPx: Float,
  maxScroll: Int,
): Int {
  if (viewportHeightPx <= 0f || maxScroll <= 0) return 0
  val contextAboveHighlight = viewportHeightPx * 0.24f
  return (highlightedTopPx - contextAboveHighlight).roundToInt().coerceIn(0, maxScroll)
}

internal fun editorHighlightTransformation(
  issue: WritingIssue?,
  appliedAiHighlight: AppliedAiHighlight?,
  issueColor: Color,
  appliedAiColor: Color,
): VisualTransformation = VisualTransformation { source ->
  val startOffset = appliedAiHighlight?.startOffset ?: issue?.startOffset
  val endOffset = appliedAiHighlight?.endOffset ?: issue?.endOffset
  if (startOffset == null || endOffset == null) {
    TransformedText(source, OffsetMapping.Identity)
  } else {
    val start = startOffset.coerceIn(0, source.length)
    val end = endOffset.coerceIn(start, source.length)
    val highlightColor = if (appliedAiHighlight != null) appliedAiColor else issueColor
    val highlighted =
      buildAnnotatedString {
        append(source)
        if (start < end) {
          addStyle(SpanStyle(background = highlightColor), start, end)
        }
      }
    TransformedText(highlighted, OffsetMapping.Identity)
  }
}

@Composable
private fun OverflowMenu(
  expanded: Boolean,
  onDismiss: () -> Unit,
  onDestinationSelected: (MenuDestination) -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    DropdownMenuItem(
      text = { MenuItemLabel(ClearWriteIcon.DOCUMENT, "Document") },
      onClick = { onDestinationSelected(MenuDestination.DOCUMENT) },
    )
    DropdownMenuItem(
      text = { MenuItemLabel(ClearWriteIcon.WRITING, "Writing") },
      onClick = { onDestinationSelected(MenuDestination.WRITING) },
    )
    DropdownMenuItem(
      text = { MenuItemLabel(ClearWriteIcon.SETTINGS, "Settings & help") },
      onClick = { onDestinationSelected(MenuDestination.APP) },
    )
  }
}

@Composable
private fun MenuItemLabel(icon: ClearWriteIcon, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    ClearWriteIconGraphic(icon, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
    Text(label, modifier = Modifier.padding(start = 12.dp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDocumentSheet(onDismiss: () -> Unit, onChooseFile: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
      Text("Import document", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(8.dp))
      Text(
        "Choose a TXT or DOCX file. Complex document formatting will not be preserved.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
      Button(
        onClick = onChooseFile,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.small,
      ) {
        Text("Choose file")
      }
      TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
  }
}

@Composable
private fun AnalysisPanel(
  uiState: MainScreenUiState,
  peekHeight: androidx.compose.ui.unit.Dp,
  expanded: Boolean,
  modifier: Modifier = Modifier,
  onToggle: () -> Unit,
  onIssueCategorySelected: (AnalysisCategory) -> Unit,
) {
  val analysis = uiState.analysis
  val issueLabel = if (analysis?.totalIssues == 1) "issue" else "issues"

  Column(
    modifier =
      modifier.fillMaxWidth()
        .semantics { contentDescription = "Analysis panel" },
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .height(peekHeight)
          .clickable(
            enabled = analysis != null && !uiState.wordLimitExceeded,
            role = Role.Button,
            onClickLabel = "Expand or collapse writing analysis",
            onClick = onToggle,
          )
          .semantics {
            contentDescription = "Analysis summary"
            stateDescription = if (expanded) "Expanded" else "Collapsed"
            if (uiState.checkState == CheckState.CHECKING) liveRegion = LiveRegionMode.Polite
          },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(8.dp))
      Box(
        modifier =
          Modifier.width(36.dp)
            .height(4.dp)
            .background(
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(50),
            )
      )
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (uiState.checkState == CheckState.CHECKING && analysis == null) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(Modifier.size(12.dp))
        }
        AnimatedContent(
          targetState = Triple(uiState.checkState, uiState.wordLimitExceeded, analysis),
          label = "analysis status",
          modifier = Modifier.weight(1f),
        ) { (checkState, wordLimitExceeded, currentAnalysis) ->
          Column {
          val title =
            when {
              wordLimitExceeded -> if (uiState.isPro) "Document limit exceeded" else "Free limit exceeded"
              checkState == CheckState.CHECKING && currentAnalysis != null -> "Updating analysis…"
              checkState == CheckState.CHECKING -> "Checking writing…"
              currentAnalysis != null -> clarityVerdict(currentAnalysis.score)
              else -> "Preparing analysis…"
            }
          val subtitle =
            when {
              wordLimitExceeded -> "${uiState.wordCount} words · Limit ${uiState.wordLimit}"
              checkState == CheckState.CHECKING && currentAnalysis == null -> "Analysis updates automatically"
              currentAnalysis != null ->
                buildList {
                    if (uiState.showWordCount) add("${currentAnalysis.wordCount} words")
                    add("${currentAnalysis.totalIssues} $issueLabel")
                    add("${currentAnalysis.writingGoal.title} goal")
                  }
                  .joinToString(" · ")
              else -> "Analysis updates automatically"
            }
          Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          }
        }
        if (analysis != null && uiState.checkState != CheckState.CHECKING) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              text = analysis.score.toString(),
              style = ClearWriteType.score,
              modifier = Modifier.padding(horizontal = ClearWriteSpacing.medium, vertical = ClearWriteSpacing.small),
            )
          }
        }
      }
    }

    if (analysis != null) {
      HorizontalDivider()
      AnalysisDetails(
        analysis = analysis,
        onIssueCategorySelected = onIssueCategorySelected,
      )
    }
  }
}

@Composable
private fun AnalysisDetails(
  analysis: AnalysisResult,
  onIssueCategorySelected: (AnalysisCategory) -> Unit,
) {
  var showScoreHelp by remember { mutableStateOf(false) }
  val groups =
    listOf(
      "Readability" to listOf(AnalysisCategory.VERY_HARD_SENTENCE, AnalysisCategory.HARD_SENTENCE, AnalysisCategory.LONG_SENTENCE),
      "Structure" to listOf(AnalysisCategory.LONG_PARAGRAPH, AnalysisCategory.REPEATED_START),
      "Style" to listOf(AnalysisCategory.PASSIVE_VOICE, AnalysisCategory.WORDY_PHRASE, AnalysisCategory.FILLER_HEAVY_SENTENCE, AnalysisCategory.WEAKENER, AnalysisCategory.REPEATED_WORD),
    ).map { (name, categories) -> name to categories.filter { analysis.count(it) > 0 } }
      .filter { it.second.isNotEmpty() }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(top = 16.dp, bottom = 32.dp),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = MaterialTheme.shapes.medium,
      modifier =
        Modifier.fillMaxWidth()
          .animateContentSize(tween(200))
          .clickable(role = Role.Button) { showScoreHelp = !showScoreHelp }
          .semantics {
            stateDescription = if (showScoreHelp) "Expanded" else "Collapsed"
          },
    ) {
      Column(Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("About your clarity score", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
          ClearWriteIconGraphic(ClearWriteIcon.HELP, MaterialTheme.colorScheme.primary, Modifier.size(20.dp))
        }
        AnimatedVisibility(visible = showScoreHelp, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
          Column {
          Spacer(Modifier.height(6.dp))
          Text(
            "The score estimates sentence clarity, concision, readability for your ${analysis.writingGoal.title.lowercase()} goal, and paragraph flow. More issues do not always reduce the score equally.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          }
        }
      }
    }
    Spacer(Modifier.height(18.dp))

    groups.forEachIndexed { groupIndex, (name, categories) ->
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { heading() }) {
        Box(
          Modifier.size(7.dp)
            .background(analysisGroupColor(name), RoundedCornerShape(50)),
        )
        Text(
          name,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = ClearWriteSpacing.small),
        )
      }
      Spacer(Modifier.height(4.dp))
      Column(Modifier.semantics { collectionInfo = CollectionInfo(categories.size, 1) }) {
        categories.forEachIndexed { index, category ->
          AnalysisRow(category, analysis.count(category), index) { onIssueCategorySelected(category) }
          if (index != categories.lastIndex) HorizontalDivider()
        }
      }
      if (groupIndex != groups.lastIndex) Spacer(Modifier.height(18.dp))
    }

    if (analysis.totalIssues == 0) {
      Text("No clarity issues found across these checks.")
    }
  }
}

@Composable
private fun AnalysisRow(category: AnalysisCategory, count: Int, rowIndex: Int, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(
          enabled = count > 0,
          role = Role.Button,
          onClickLabel = "View first ${category.issueLabel.lowercase()}",
          onClick = onClick,
        )
        .semantics {
          contentDescription = "${category.title}, $count ${if (count == 1) "issue" else "issues"}"
          collectionItemInfo = CollectionItemInfo(rowIndex, 1, 0, 1)
        }
        .padding(vertical = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(category.title, fontWeight = FontWeight.Medium)
      Text(
        category.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
      shape = MaterialTheme.shapes.extraSmall,
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      )
    }
    Spacer(Modifier.width(8.dp))
    ClearWriteIconGraphic(ClearWriteIcon.CHEVRON_RIGHT, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
  }
}

@Composable
private fun SelectedIssueCard(
  uiState: MainScreenUiState,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onDismiss: () -> Unit,
  onEditInDraft: () -> Unit,
  onIgnore: () -> Unit,
  onImproveWithAi: () -> Unit,
) {
  val issue = uiState.selectedIssue ?: return
  Surface(
    color = ClearWriteThemeTokens.colors.issueContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
  ) {
    Column(
      modifier =
        Modifier.animateContentSize(tween(200))
          .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(
            text = issue.category.issueLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${uiState.selectedIssueNumber} of ${uiState.selectedIssueCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(onClick = onDismiss) { Text("Close review") }
      }
      Spacer(Modifier.height(ClearWriteSpacing.small))
      Text(text = issue.explanation, style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.height(ClearWriteSpacing.medium))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ClearWriteSpacing.small)) {
        OutlinedButton(
          onClick = onEditInDraft,
          modifier = Modifier.weight(1f),
          shape = MaterialTheme.shapes.small,
        ) { Text("Edit in draft") }
        TextButton(onClick = onIgnore) { Text("Ignore") }
      }
      if (uiState.selectedIssueCount > 1) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val navigationColors = ButtonDefaults.outlinedButtonColors(
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
          )
          TextButton(onClick = onPrevious, enabled = uiState.hasPreviousIssue, colors = navigationColors) { Text("Previous") }
          Text(
            "${uiState.selectedIssueNumber} / ${uiState.selectedIssueCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TextButton(onClick = onNext, enabled = uiState.hasNextIssue, colors = navigationColors) { Text("Next") }
        }
      }
      FilledTonalButton(
        onClick = onImproveWithAi,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = ClearWriteThemeTokens.colors.aiContainer,
          contentColor = ClearWriteThemeTokens.colors.onAiContainer,
        ),
      ) {
        ClearWriteIconGraphic(ClearWriteIcon.SPARK, ClearWriteThemeTokens.colors.aiAccent, Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Improve with AI · Pro")
      }
    }
  }
}

@Composable
private fun analysisGroupColor(name: String): Color =
  when (name) {
    "Readability" -> ClearWriteThemeTokens.colors.warning
    "Style" -> ClearWriteThemeTokens.colors.aiAccent
    else -> MaterialTheme.colorScheme.primary
  }

private fun clarityVerdict(score: Int): String =
  when {
    score >= 90 -> "Very clear"
    score >= 75 -> "Clear overall"
    score >= 60 -> "A little work needed"
    else -> "Needs attention"
  }

@Composable
private fun InlineMessage(
  message: String,
  tone: MessageTone,
  actionLabel: String?,
  onAction: () -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = ClearWriteThemeTokens.colors
  val containerColor = when (tone) {
    MessageTone.SUCCESS -> colors.successContainer
    MessageTone.INFO -> MaterialTheme.colorScheme.secondaryContainer
    MessageTone.ERROR -> MaterialTheme.colorScheme.errorContainer
  }
  val contentColor = when (tone) {
    MessageTone.SUCCESS -> colors.onSuccessContainer
    MessageTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
    MessageTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
  }
  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = message,
        modifier = Modifier.weight(1f),
        color = contentColor,
        style = MaterialTheme.typography.bodySmall,
      )
      if (actionLabel != null) {
        TextButton(onClick = onAction) { Text(actionLabel) }
      }
      TextButton(onClick = onDismiss) { Text("Dismiss") }
    }
  }
}

@Composable
private fun EditorScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
  val alpha by animateFloatAsState(
    targetValue =
      when {
        scrollState.maxValue <= 0 -> 0f
        scrollState.isScrollInProgress -> 1f
        else -> 0.38f
      },
    animationSpec = tween(durationMillis = if (scrollState.isScrollInProgress) 120 else 650),
    label = "editor scrollbar",
  )
  val color = MaterialTheme.colorScheme.onSurfaceVariant
  Canvas(modifier) {
    if (alpha <= 0f || scrollState.maxValue <= 0 || size.height <= 0f) return@Canvas
    val contentHeight = size.height + scrollState.maxValue
    val thumbHeight = (size.height * (size.height / contentHeight)).coerceAtLeast(28.dp.toPx())
    val travel = (size.height - thumbHeight).coerceAtLeast(0f)
    val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    drawRoundRect(
      color = color.copy(alpha = 0.55f * alpha),
      topLeft = Offset(0f, travel * progress),
      size = Size(size.width, thumbHeight),
      cornerRadius = CornerRadius(size.width / 2f, size.width / 2f),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiRewriteSheet(
  state: AiRewriteState,
  onApplySuggestion: (AiSuggestion) -> Unit,
  onConfirmConsent: () -> Unit,
  onRetry: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
    ) {
      Text("AI improvement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(6.dp))
      Text(
        "AI can change meaning. Review every suggestion before replacing your text.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(20.dp))

      when (state) {
        AiRewriteState.Idle -> Unit
        is AiRewriteState.Consent -> {
          Text(
            "To create a suggestion, ClearWrite sends this selected passage and nearby context to the AI provider. The ClearWrite backend does not store it.",
            style = MaterialTheme.typography.bodyMedium,
          )
          Spacer(Modifier.height(16.dp))
          Button(onClick = onConfirmConsent, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to AI")
          }
        }
        is AiRewriteState.Loading -> {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(12.dp))
            Text("Creating suggestions…")
          }
        }
        is AiRewriteState.Error -> {
          Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
          Spacer(Modifier.height(16.dp))
          Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
        }
        is AiRewriteState.Ready -> {
          Text("Original", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.height(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(state.target.originalText, modifier = Modifier.padding(14.dp))
          }
          Spacer(Modifier.height(16.dp))
          state.suggestions.forEachIndexed { index, suggestion ->
            SuggestionCard(
              number = index + 1,
              suggestion = suggestion,
              onApply = onApplySuggestion,
            )
            if (index != state.suggestions.lastIndex) Spacer(Modifier.height(12.dp))
          }
          state.warnings.firstOrNull()?.let { warning ->
            Spacer(Modifier.height(12.dp))
            Text(
              warning,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(8.dp))
          TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
            Text("Try another")
          }
        }
      }

      Spacer(Modifier.height(8.dp))
      TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep my original") }
    }
  }
}

private enum class SelectionRewriteSheetMode {
  ACTIONS,
  TONE,
  CUSTOM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionRewriteSheet(
  target: AiRewriteTarget,
  onRewrite: (SelectionRewriteAction, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var mode by remember { mutableStateOf(SelectionRewriteSheetMode.ACTIONS) }
  var customInstruction by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
    ) {
      Text("Rewrite selection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = target.originalText,
          modifier = Modifier.padding(14.dp),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 4,
        )
      }
      Spacer(Modifier.height(18.dp))

      when (mode) {
        SelectionRewriteSheetMode.ACTIONS -> {
          Text("Quick improvements", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.height(4.dp))
          RewriteOption("Improve clarity") {
            onRewrite(SelectionRewriteAction.IMPROVE_CLARITY, "preserve", "")
          }
          RewriteOption("Shorten") {
            onRewrite(SelectionRewriteAction.SHORTEN, "preserve", "")
          }
          RewriteOption("Simplify") {
            onRewrite(SelectionRewriteAction.SIMPLIFY, "preserve", "")
          }
          Spacer(Modifier.height(12.dp))
          Text("More options", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          TextButton(onClick = { mode = SelectionRewriteSheetMode.TONE }, modifier = Modifier.fillMaxWidth()) { Text("Change tone") }
          TextButton(onClick = { mode = SelectionRewriteSheetMode.CUSTOM }, modifier = Modifier.fillMaxWidth()) { Text("Custom instruction") }
        }
        SelectionRewriteSheetMode.TONE -> {
          Text("Choose a tone", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          listOf("Professional", "Friendly", "Confident", "Formal").forEach { tone ->
            RewriteOption(tone) {
              onRewrite(SelectionRewriteAction.CHANGE_TONE, tone.lowercase(), "")
            }
          }
          TextButton(onClick = { mode = SelectionRewriteSheetMode.ACTIONS }) { Text("Back") }
        }
        SelectionRewriteSheetMode.CUSTOM -> {
          OutlinedTextField(
            value = customInstruction,
            onValueChange = {
              customInstruction = it.take(MAX_AI_CUSTOM_INSTRUCTION_CHARACTERS)
            },
            label = { Text("How should this be rewritten?") },
            supportingText = {
              Text("${customInstruction.length}/$MAX_AI_CUSTOM_INSTRUCTION_CHARACTERS")
            },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(Modifier.height(12.dp))
          Button(
            onClick = {
              onRewrite(SelectionRewriteAction.CUSTOM, "preserve", customInstruction)
            },
            enabled = customInstruction.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Rewrite with AI · Pro")
          }
          TextButton(onClick = { mode = SelectionRewriteSheetMode.ACTIONS }) { Text("Back") }
        }
      }

      TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
  }
}

@Composable
private fun RewriteOption(label: String, onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(50.dp),
    shape = MaterialTheme.shapes.small,
  ) {
    Text(label)
  }
}

@Composable
private fun SuggestionCard(number: Int, suggestion: AiSuggestion, onApply: (AiSuggestion) -> Unit) {
  var editedText by remember(suggestion.text) { mutableStateOf(suggestion.text) }
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Suggestion $number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = editedText,
        onValueChange = { editedText = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Suggested text") },
        supportingText = { Text("You can edit this before using it.") },
        minLines = 2,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        suggestion.explanation,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = { onApply(suggestion.copy(text = editedText.trim())) },
        enabled = editedText.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Use suggestion")
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmptyEditorPreview() {
  ClearWriteTheme(dynamicColor = false) {
    EditorScreen(
      uiState = MainScreenUiState(),
      onTextChanged = {},
      onPasteUnavailable = {},
      onImportClick = {},
      onShowAnalysis = {},
      onDismissAnalysis = {},
      onIssueCategorySelected = {},
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
      onRenameDocument = {},
      onMenuDestinationSelected = {},
      onMessageShown = {},
    )
  }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DraftEditorPreview() {
  ClearWriteTheme(dynamicColor = false) {
    EditorScreen(
      uiState = MainScreenUiState(text = "Clear writing helps readers understand an idea without unnecessary effort."),
      onTextChanged = {},
      onPasteUnavailable = {},
      onImportClick = {},
      onShowAnalysis = {},
      onDismissAnalysis = {},
      onIssueCategorySelected = {},
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
      onRenameDocument = {},
      onMenuDestinationSelected = {},
      onMessageShown = {},
    )
  }
}
