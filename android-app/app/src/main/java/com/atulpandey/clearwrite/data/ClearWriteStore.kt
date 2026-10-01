package com.atulpandey.clearwrite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atulpandey.clearwrite.analysis.AnalysisCategory
import com.atulpandey.clearwrite.analysis.WritingGoal
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemePreference {
  SYSTEM,
  LIGHT,
  DARK,
}

data class StoredClearWriteState(
  val documentText: String = "",
  val importedFileName: String? = null,
  val enabledChecks: Set<AnalysisCategory> = AnalysisCategory.entries.toSet(),
  val showWordCount: Boolean = true,
  val aiConsentGranted: Boolean = false,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val writingGoal: WritingGoal = WritingGoal.GENERAL,
  val selectionRewriteHintDismissed: Boolean = false,
)

interface ClearWriteLocalStore {
  val state: Flow<StoredClearWriteState>

  suspend fun saveDocument(text: String, importedFileName: String?)

  suspend fun clearDocument()

  suspend fun saveEnabledChecks(categories: Set<AnalysisCategory>)

  suspend fun saveShowWordCount(show: Boolean)

  suspend fun saveAiConsent(granted: Boolean)

  suspend fun saveThemePreference(preference: ThemePreference)

  suspend fun saveWritingGoal(goal: WritingGoal)

  suspend fun saveSelectionRewriteHintDismissed(dismissed: Boolean)
}

private val Context.clearWriteDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "clearwrite_local_state")

class DataStoreClearWriteLocalStore(context: Context) : ClearWriteLocalStore {
  private val dataStore = context.applicationContext.clearWriteDataStore

  override val state: Flow<StoredClearWriteState> =
    dataStore.data
      .catch { error ->
        if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
        else throw error
      }
      .map { preferences ->
        val savedChecks = preferences[enabledChecksKey]
        val enabledChecks =
          savedChecks
            ?.mapNotNullTo(mutableSetOf()) { savedName ->
              AnalysisCategory.entries.firstOrNull { it.name == savedName }
            }
            ?: AnalysisCategory.entries.toSet()
        StoredClearWriteState(
          documentText = preferences[documentTextKey].orEmpty(),
          importedFileName = preferences[importedFileNameKey],
          enabledChecks = enabledChecks,
          showWordCount = preferences[showWordCountKey] ?: true,
          aiConsentGranted = preferences[aiConsentKey] ?: false,
          themePreference =
            preferences[themePreferenceKey]
              ?.let { storedName ->
                ThemePreference.entries.firstOrNull { it.name == storedName }
              }
              ?: ThemePreference.SYSTEM,
          writingGoal =
            preferences[writingGoalKey]
              ?.let { storedName -> WritingGoal.entries.firstOrNull { it.name == storedName } }
              ?: WritingGoal.GENERAL,
          selectionRewriteHintDismissed = preferences[selectionRewriteHintDismissedKey] ?: false,
        )
      }

  override suspend fun saveDocument(text: String, importedFileName: String?) {
    dataStore.edit { preferences ->
      preferences[documentTextKey] = text
      if (importedFileName == null) preferences.remove(importedFileNameKey)
      else preferences[importedFileNameKey] = importedFileName
    }
  }

  override suspend fun clearDocument() {
    dataStore.edit { preferences ->
      preferences.remove(documentTextKey)
      preferences.remove(importedFileNameKey)
    }
  }

  override suspend fun saveEnabledChecks(categories: Set<AnalysisCategory>) {
    dataStore.edit { preferences -> preferences[enabledChecksKey] = categories.mapTo(mutableSetOf()) { it.name } }
  }

  override suspend fun saveShowWordCount(show: Boolean) {
    dataStore.edit { preferences -> preferences[showWordCountKey] = show }
  }

  override suspend fun saveAiConsent(granted: Boolean) {
    dataStore.edit { preferences -> preferences[aiConsentKey] = granted }
  }

  override suspend fun saveThemePreference(preference: ThemePreference) {
    dataStore.edit { preferences -> preferences[themePreferenceKey] = preference.name }
  }

  override suspend fun saveWritingGoal(goal: WritingGoal) {
    dataStore.edit { preferences -> preferences[writingGoalKey] = goal.name }
  }

  override suspend fun saveSelectionRewriteHintDismissed(dismissed: Boolean) {
    dataStore.edit { preferences -> preferences[selectionRewriteHintDismissedKey] = dismissed }
  }

  private companion object {
    val documentTextKey = stringPreferencesKey("document_text")
    val importedFileNameKey = stringPreferencesKey("imported_file_name")
    val enabledChecksKey = stringSetPreferencesKey("enabled_checks")
    val showWordCountKey = booleanPreferencesKey("show_word_count")
    val aiConsentKey = booleanPreferencesKey("ai_consent_granted")
    val themePreferenceKey = stringPreferencesKey("theme_preference")
    val writingGoalKey = stringPreferencesKey("writing_goal")
    val selectionRewriteHintDismissedKey = booleanPreferencesKey("selection_rewrite_hint_dismissed")
  }
}
