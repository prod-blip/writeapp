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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
  val documents: List<SavedDocument> = emptyList(),
  val activeDocumentId: String? = null,
)

data class SavedDocument(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "Untitled document",
  val text: String = "",
  val importedFileName: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = createdAt,
)

interface ClearWriteLocalStore {
  val state: Flow<StoredClearWriteState>

  suspend fun saveDocument(text: String, importedFileName: String?)

  suspend fun saveDocumentLibrary(documents: List<SavedDocument>, activeDocumentId: String?)

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
          documents = decodeDocuments(preferences[documentsKey]),
          activeDocumentId = preferences[activeDocumentIdKey],
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

  override suspend fun saveDocumentLibrary(
    documents: List<SavedDocument>,
    activeDocumentId: String?,
  ) {
    dataStore.edit { preferences ->
      preferences[documentsKey] = encodeDocuments(documents)
      if (activeDocumentId == null) preferences.remove(activeDocumentIdKey)
      else preferences[activeDocumentIdKey] = activeDocumentId
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
    val documentsKey = stringPreferencesKey("saved_documents")
    val activeDocumentIdKey = stringPreferencesKey("active_document_id")

    fun encodeDocuments(documents: List<SavedDocument>): String =
      JSONArray().apply {
        documents.forEach { document ->
          put(
            JSONObject().apply {
              put("id", document.id)
              put("title", document.title)
              put("text", document.text)
              put("importedFileName", document.importedFileName ?: JSONObject.NULL)
              put("createdAt", document.createdAt)
              put("updatedAt", document.updatedAt)
            }
          )
        }
      }.toString()

    fun decodeDocuments(raw: String?): List<SavedDocument> =
      runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyList()
        val json = JSONArray(raw)
        buildList {
          for (index in 0 until json.length()) {
            val item = json.optJSONObject(index) ?: continue
            val text = item.optString("text", "")
            add(
              SavedDocument(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                title = item.optString("title").ifBlank { "Untitled document" },
                text = text,
                importedFileName = item.optString("importedFileName").takeIf { it.isNotBlank() && it != "null" },
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
              )
            )
          }
        }
      }.getOrDefault(emptyList())
  }
}
