package com.atulpandey.clearwrite.billing

import com.atulpandey.clearwrite.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VerifiedEntitlement(
  val active: Boolean,
  val accessUntilMillis: Long,
)

interface EntitlementVerifier {
  suspend fun verify(purchaseToken: String): Result<VerifiedEntitlement>
}

internal class PlayEntitlementVerifier(
  private val backendUrl: String = BuildConfig.AI_BACKEND_URL,
) : EntitlementVerifier {
  override suspend fun verify(purchaseToken: String): Result<VerifiedEntitlement> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(backendUrl.isNotBlank()) { "The ClearWrite service is not configured in this build." }
        val connection =
          (URL("${backendUrl.trimEnd('/')}/v1/billing/verify").openConnection() as HttpURLConnection)
            .apply {
              requestMethod = "POST"
              connectTimeout = 10_000
              readTimeout = 20_000
              doOutput = true
              useCaches = false
              setRequestProperty("Content-Type", "application/json; charset=utf-8")
              setRequestProperty("Accept", "application/json")
            }
        try {
          val request =
            JSONObject()
              .put("packageName", BuildConfig.APPLICATION_ID)
              .put("productId", BuildConfig.PRO_PRODUCT_ID)
              .put("purchaseToken", purchaseToken)
          connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.toString()) }
          val status = connection.responseCode
          val stream = if (status in 200..299) connection.inputStream else connection.errorStream
          val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
          val body = responseText.takeIf { it.isNotBlank() }?.let(::JSONObject)
          if (status !in 200..299) {
            error(body?.optString("error")?.takeIf(String::isNotBlank) ?: "Could not verify the subscription.")
          }
          VerifiedEntitlement(
            active = body?.optBoolean("active", false) == true,
            accessUntilMillis = body?.optLong("accessUntilMillis", 0L) ?: 0L,
          )
        } finally {
          connection.disconnect()
        }
      }
    }
}
