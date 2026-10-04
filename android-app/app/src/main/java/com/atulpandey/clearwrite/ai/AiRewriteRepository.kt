package com.atulpandey.clearwrite.ai

import com.atulpandey.clearwrite.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiRewriteRequest(
  val documentRevision: Int,
  val selectedText: String,
  val contextBefore: String,
  val contextAfter: String,
  val issueType: String,
  val action: String,
  val tone: String = "preserve",
  val customInstruction: String = "",
  val locale: String = "en",
  val requestId: String = UUID.randomUUID().toString(),
)

data class AiSuggestion(
  val text: String,
  val explanation: String,
)

data class AiRewriteResponse(
  val requestId: String,
  val documentRevision: Int,
  val suggestions: List<AiSuggestion>,
  val warnings: List<String>,
)

sealed interface AiRewriteResult {
  data class Success(val response: AiRewriteResponse) : AiRewriteResult

  data class Failure(val message: String) : AiRewriteResult
}

interface AiRewriteService {
  suspend fun improve(request: AiRewriteRequest): AiRewriteResult
}

class AiRewriteRepository(
  private val backendUrl: String = BuildConfig.AI_BACKEND_URL,
  private val developmentToken: String = BuildConfig.AI_DEV_TOKEN,
  private val purchaseTokenProvider: () -> String? = { null },
  private val reviewerTokenProvider: () -> String? = { null },
) : AiRewriteService {
  override suspend fun improve(request: AiRewriteRequest): AiRewriteResult =
    withContext(Dispatchers.IO) {
      if (backendUrl.isBlank()) {
        return@withContext AiRewriteResult.Failure("AI is not configured in this build.")
      }

      val connection =
        runCatching {
          (URL("${backendUrl.trimEnd('/')}/v1/ai/improvements").openConnection() as HttpURLConnection)
            .apply {
              requestMethod = "POST"
              connectTimeout = 10_000
              readTimeout = 30_000
              doOutput = true
              useCaches = false
              setRequestProperty("Content-Type", "application/json; charset=utf-8")
              setRequestProperty("Accept", "application/json")
              if (developmentToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $developmentToken")
              }
              purchaseTokenProvider()?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("X-Play-Purchase-Token", it)
              }
              reviewerTokenProvider()?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("X-Reviewer-Access-Token", it)
              }
            }
        }.getOrElse {
          return@withContext AiRewriteResult.Failure("Could not connect to the writing service.")
        }

      try {
        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
          writer.write(request.toJson().toString())
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val body = responseText.takeIf { it.isNotBlank() }?.let(::JSONObject)

        if (status !in 200..299) {
          val serverMessage = body?.optString("error")?.takeIf { it.isNotBlank() }
          return@withContext AiRewriteResult.Failure(
            serverMessage ?: "The writing service could not complete this request."
          )
        }

        val response = body?.toResponse()
          ?: return@withContext AiRewriteResult.Failure("The writing service returned an empty response.")
        if (response.requestId != request.requestId || response.documentRevision != request.documentRevision) {
          return@withContext AiRewriteResult.Failure("The suggestion no longer matches this draft.")
        }
        val usefulSuggestions = response.suggestions.filter { it.text != request.selectedText }
        if (usefulSuggestions.isEmpty()) {
          return@withContext AiRewriteResult.Failure("No usable suggestion was returned.")
        }
        AiRewriteResult.Success(response.copy(suggestions = usefulSuggestions))
      } catch (_: Exception) {
        AiRewriteResult.Failure("Could not reach the writing service. Check the connection and try again.")
      } finally {
        connection.disconnect()
      }
    }
}

private fun AiRewriteRequest.toJson(): JSONObject =
  JSONObject()
    .put("requestId", requestId)
    .put("documentRevision", documentRevision)
    .put("selectedText", selectedText)
    .put("contextBefore", contextBefore)
    .put("contextAfter", contextAfter)
    .put("issueType", issueType)
    .put("action", action)
    .put("tone", tone)
    .put("customInstruction", customInstruction)
    .put("locale", locale)

private fun JSONObject.toResponse(): AiRewriteResponse {
  val suggestionsJson = getJSONArray("suggestions")
  val suggestions =
    buildList {
      for (index in 0 until suggestionsJson.length()) {
        val item = suggestionsJson.getJSONObject(index)
        val text = item.getString("text").trim()
        val explanation = item.getString("explanation").trim()
        if (text.isNotEmpty()) add(AiSuggestion(text = text, explanation = explanation))
      }
    }
  val warningsJson = optJSONArray("warnings")
  val warnings =
    buildList {
      if (warningsJson != null) {
        for (index in 0 until warningsJson.length()) add(warningsJson.getString(index))
      }
    }
  return AiRewriteResponse(
    requestId = getString("requestId"),
    documentRevision = getInt("documentRevision"),
    suggestions = suggestions,
    warnings = warnings,
  )
}
