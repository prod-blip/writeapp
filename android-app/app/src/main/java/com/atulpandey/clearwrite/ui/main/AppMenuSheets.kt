package com.atulpandey.clearwrite.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.WritingGoal
import com.atulpandey.clearwrite.data.ThemePreference
import com.atulpandey.clearwrite.billing.BillingUiState
import com.atulpandey.clearwrite.billing.ProOffer
import com.atulpandey.clearwrite.theme.ClearWriteSpacing
import com.atulpandey.clearwrite.theme.ClearWriteThemeTokens

internal enum class MenuDestination(val label: String) {
  DOCUMENT("Document"),
  WRITING("Writing"),
  APP("Settings & help"),
  UPGRADE("ClearWrite Pro"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppMenuSheet(
  destination: MenuDestination,
  uiState: MainScreenUiState,
  onDismiss: () -> Unit,
  onDeleteDocument: () -> Unit,
  onCheckEnabledChanged: (AnalysisCategory, Boolean) -> Unit,
  onEnableAllChecks: () -> Unit,
  onShowWordCountChanged: (Boolean) -> Unit,
  onThemePreferenceChanged: (ThemePreference) -> Unit,
  onWritingGoalChanged: (WritingGoal) -> Unit,
  onResetAiConsent: () -> Unit,
  onImportDocument: () -> Unit,
  onStartNewDocument: () -> Unit,
  onOpenUpgrade: () -> Unit,
  billingState: BillingUiState,
  onPurchase: (String) -> Unit,
  onRestorePurchase: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).verticalScroll(rememberScrollState())
        .padding(horizontal = ClearWriteSpacing.xLarge).navigationBarsPadding().padding(bottom = ClearWriteSpacing.xLarge),
    ) {
      SheetHeader(destination.label, onDismiss)
      Spacer(Modifier.height(8.dp))
      when (destination) {
        MenuDestination.DOCUMENT -> DocumentContent(uiState, onDismiss, onImportDocument, onStartNewDocument, onDeleteDocument)
        MenuDestination.WRITING -> WritingContent(uiState, onCheckEnabledChanged, onEnableAllChecks, onWritingGoalChanged)
        MenuDestination.APP -> AppContent(uiState, onShowWordCountChanged, onThemePreferenceChanged, onResetAiConsent, onDeleteDocument, onOpenUpgrade)
        MenuDestination.UPGRADE -> UpgradeContent(billingState, onPurchase, onRestorePurchase)
      }
    }
  }
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    TextButton(onClick = onDismiss) { Text("Close") }
  }
}

@Composable
private fun DocumentContent(
  uiState: MainScreenUiState,
  onOpenDocument: () -> Unit,
  onImportDocument: () -> Unit,
  onStartNewDocument: () -> Unit,
  onDeleteDocument: () -> Unit,
) {
  Text(
    if (uiState.isPro) "ClearWrite Pro" else "Free plan · one saved document",
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
  Spacer(Modifier.height(12.dp))
  if (uiState.text.isBlank()) {
    Text("No saved document yet", style = MaterialTheme.typography.titleMedium)
    Text("Start writing in the editor. Your draft saves automatically on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  } else {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
      Column(Modifier.padding(16.dp)) {
        Text(uiState.importedFileName ?: "Current document", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("${uiState.wordCount} words · Saved on this device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(uiState.text.replace(Regex("\\s+"), " ").take(180), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
      }
    }
    TextButton(onClick = onOpenDocument, modifier = Modifier.fillMaxWidth()) { Text("Return to editor") }
  }
  HorizontalDivider(Modifier.padding(vertical = 12.dp))
  ActionRow(ClearWriteIcon.IMPORT, "Import document", "TXT or DOCX") { onImportDocument() }
  ActionRow(ClearWriteIcon.DOCUMENT, "Start new document", "Replace the current draft") { onStartNewDocument() }
  if (uiState.text.isNotBlank()) {
    TextButton(onClick = onDeleteDocument, modifier = Modifier.fillMaxWidth()) {
      Text("Delete saved document", color = MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
private fun WritingContent(
  uiState: MainScreenUiState,
  onCheckEnabledChanged: (AnalysisCategory, Boolean) -> Unit,
  onEnableAllChecks: () -> Unit,
  onWritingGoalChanged: (WritingGoal) -> Unit,
) {
  var showAdvancedChecks by remember { mutableStateOf(false) }
  val recommendedChecks =
    listOf(
      AnalysisCategory.LONG_SENTENCE,
      AnalysisCategory.VERY_HARD_SENTENCE,
      AnalysisCategory.LONG_PARAGRAPH,
      AnalysisCategory.WORDY_PHRASE,
      AnalysisCategory.REPEATED_WORD,
    )
  val advancedChecks = AnalysisCategory.entries.filterNot(recommendedChecks::contains)

  SectionTitle("Writing goal")
  Text("Your goal adjusts readability expectations in the clarity score.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(6.dp))
  WritingGoal.entries.forEach { goal ->
    WritingGoalRow(goal, uiState.writingGoal == goal) { onWritingGoalChanged(goal) }
  }
  HorizontalDivider(Modifier.padding(vertical = 16.dp))
  SectionTitle("Local checks")
  Text("Choose the checks included in analysis. These run on your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  Spacer(Modifier.height(8.dp))
  recommendedChecks.forEachIndexed { index, category ->
    PreferenceSwitchRow(category.title, category.description, category in uiState.enabledChecks, "${category.title} check") {
      onCheckEnabledChanged(category, it)
    }
    if (index != recommendedChecks.lastIndex) HorizontalDivider()
  }
  OutlinedButton(
    onClick = { showAdvancedChecks = !showAdvancedChecks },
    modifier = Modifier.fillMaxWidth().padding(top = ClearWriteSpacing.small),
    shape = MaterialTheme.shapes.small,
  ) {
    Text(if (showAdvancedChecks) "Hide advanced checks" else "Show advanced checks")
  }
  AnimatedVisibility(
    visible = showAdvancedChecks,
    enter = fadeIn(tween(180)),
    exit = fadeOut(tween(120)),
  ) {
    Column(Modifier.animateContentSize(tween(200))) {
      Spacer(Modifier.height(ClearWriteSpacing.small))
      advancedChecks.forEachIndexed { index, category ->
        PreferenceSwitchRow(category.title, category.description, category in uiState.enabledChecks, "${category.title} check") {
          onCheckEnabledChanged(category, it)
        }
        if (index != advancedChecks.lastIndex) HorizontalDivider()
      }
    }
  }
  TextButton(onClick = onEnableAllChecks, modifier = Modifier.fillMaxWidth()) { Text("Enable all checks") }
}

@Composable
private fun AppContent(
  uiState: MainScreenUiState,
  onShowWordCountChanged: (Boolean) -> Unit,
  onThemePreferenceChanged: (ThemePreference) -> Unit,
  onResetAiConsent: () -> Unit,
  onDeleteDocument: () -> Unit,
  onOpenUpgrade: () -> Unit,
) {
  SectionTitle("Appearance")
  ThemePreference.entries.forEach { preference ->
    ThemePreferenceRow(preference, uiState.themePreference == preference) { onThemePreferenceChanged(preference) }
  }
  HorizontalDivider(Modifier.padding(vertical = 12.dp))
  PreferenceSwitchRow(
    "Show word count", "Show the count in the compact analysis summary.", uiState.showWordCount,
    "Show word count setting", onShowWordCountChanged,
  )
  HorizontalDivider(Modifier.padding(vertical = 12.dp))
  SectionTitle("Privacy & data")
  FactRow("Writing analysis runs locally on your device.")
  FactRow("AI receives text only when you request a rewrite.")
  FactRow("The ClearWrite backend does not store submitted passages.")
  val disabledActionColors = ButtonDefaults.textButtonColors(
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
  )
  TextButton(onClick = onResetAiConsent, enabled = uiState.aiConsentGranted, colors = disabledActionColors) { Text("Reset AI permission") }
  TextButton(onClick = onDeleteDocument, enabled = uiState.text.isNotBlank(), colors = disabledActionColors) {
    Text("Delete saved document", color = if (uiState.text.isNotBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f))
  }
  HorizontalDivider(Modifier.padding(vertical = 12.dp))
  HelpContent()
  HorizontalDivider(Modifier.padding(vertical = 12.dp))
  OutlinedButton(onClick = onOpenUpgrade, modifier = Modifier.fillMaxWidth()) { Text("Explore ClearWrite Pro") }
}

@Composable
private fun UpgradeContent(
  billingState: BillingUiState,
  onPurchase: (String) -> Unit,
  onRestorePurchase: () -> Unit,
) {
  Surface(
    color = ClearWriteThemeTokens.colors.aiContainer,
    contentColor = ClearWriteThemeTokens.colors.onAiContainer,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(ClearWriteSpacing.large)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ClearWriteIconGraphic(ClearWriteIcon.SPARK, ClearWriteThemeTokens.colors.aiAccent, Modifier.size(20.dp))
        Text(
          "ClearWrite Pro",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(start = ClearWriteSpacing.small),
        )
      }
      Text(
        if (billingState.isPro) "Your subscription is active." else "Optional AI help for the moments you want it.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = ClearWriteSpacing.small),
      )
    }
  }
  Spacer(Modifier.height(12.dp))
  FeatureLine("AI rewrite suggestions")
  FeatureLine("Up to 10,000 words per draft")
  FeatureLine("Free local clarity checks remain available")
  Spacer(Modifier.height(18.dp))
  if (billingState.isRefreshing) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
      CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
    }
  }
  if (!billingState.isPro) {
    billingState.offers.forEach { offer ->
      SubscriptionOfferButton(offer, onPurchase)
      Spacer(Modifier.height(8.dp))
    }
    if (billingState.offers.isEmpty() && !billingState.isRefreshing) {
      Text(
        "ClearWrite Pro is not available for purchase yet.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  TextButton(
    onClick = onRestorePurchase,
    enabled = billingState.isReady && !billingState.isRefreshing,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Restore purchase")
  }
  billingState.message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  if (!billingState.isPro && billingState.offers.isNotEmpty()) {
    Text(
      "Subscriptions renew automatically unless cancelled in Google Play.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SubscriptionOfferButton(offer: ProOffer, onPurchase: (String) -> Unit) {
  val planName = if (offer.offerId == null) "ClearWrite Pro" else "ClearWrite Pro offer"
  Button(onClick = { onPurchase(offer.offerToken) }, modifier = Modifier.fillMaxWidth()) {
    Text("$planName · ${offer.formattedPrice}/${offer.billingPeriod}")
  }
}

@Composable
private fun FeatureLine(text: String) {
  Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Surface(color = ClearWriteThemeTokens.colors.aiContainer, shape = RoundedCornerShape(50)) {
      ClearWriteIconGraphic(ClearWriteIcon.CHECK, ClearWriteThemeTokens.colors.aiAccent, Modifier.padding(5.dp).size(14.dp))
    }
    Text(text, modifier = Modifier.padding(start = 12.dp))
  }
}

@Composable
private fun HelpContent() {
  val context = LocalContext.current
  var copied by remember { mutableStateOf(false) }
  SectionTitle("Help")
  HelpAnswer("How is clarity calculated?", "It combines sentence clarity, concision, readability for your goal, and paragraph flow. Tap the score in analysis for details.")
  HelpAnswer("How do I rewrite selected text?", "Select text in the editor, then tap Rewrite with AI.")
  HelpAnswer("How do I undo an AI change?", "Use the persistent Undo action shown after applying a suggestion.")
  OutlinedButton(onClick = {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("ClearWrite feedback", FEEDBACK_TEMPLATE))
    copied = true
  }, modifier = Modifier.fillMaxWidth()) {
    Text(if (copied) "Feedback template copied" else "Copy feedback template")
  }
}

@Composable
private fun HelpAnswer(question: String, answer: String) {
  Text(question, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
  Text(answer, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
  Spacer(Modifier.height(4.dp))
}

@Composable
private fun ActionRow(icon: ClearWriteIcon, title: String, subtitle: String, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().selectable(selected = false, role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ClearWriteIconGraphic(icon, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
      Text(title, fontWeight = FontWeight.Medium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ClearWriteIconGraphic(ClearWriteIcon.CHEVRON_RIGHT, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
  }
}

@Composable
private fun WritingGoalRow(goal: WritingGoal, selected: Boolean, onSelected: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelected)
      .semantics { contentDescription = "${goal.title} writing goal" }.padding(vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected, onClick = null)
    Column(Modifier.padding(start = 8.dp)) {
      Text(goal.title)
      Text(goal.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun ThemePreferenceRow(preference: ThemePreference, selected: Boolean, onSelected: () -> Unit) {
  val title = when (preference) { ThemePreference.SYSTEM -> "System default"; ThemePreference.LIGHT -> "Light"; ThemePreference.DARK -> "Dark" }
  Row(
    Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelected)
      .semantics { contentDescription = "$title theme option" }.padding(vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected, onClick = null)
    Text(title, modifier = Modifier.padding(start = 8.dp))
  }
}

@Composable
private fun PreferenceSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  contentDescription: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
      .semantics { this.contentDescription = contentDescription }.padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f).padding(end = 16.dp)) {
      Text(title, fontWeight = FontWeight.Medium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked, onCheckedChange = null)
  }
}

@Composable
private fun FactRow(text: String) {
  Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
    ClearWriteIconGraphic(ClearWriteIcon.CHECK, MaterialTheme.colorScheme.primary, Modifier.padding(top = 2.dp).size(18.dp))
    Text(text, modifier = Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private const val FEEDBACK_TEMPLATE = "ClearWrite feedback\n\nWhat happened:\n\nWhat I expected:\n\nAnything else:"
