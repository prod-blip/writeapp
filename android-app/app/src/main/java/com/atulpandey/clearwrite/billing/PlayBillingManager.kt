package com.atulpandey.clearwrite.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.atulpandey.clearwrite.BuildConfig
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProOffer(
  val offerToken: String,
  val basePlanId: String,
  val offerId: String?,
  val formattedPrice: String,
  val billingPeriod: String,
)

data class BillingUiState(
  val isReady: Boolean = false,
  val isRefreshing: Boolean = true,
  val isPro: Boolean = false,
  val isReviewerAccess: Boolean = false,
  val offers: List<ProOffer> = emptyList(),
  val message: String? = null,
)

class PlayBillingManager(
  context: Context,
  private val entitlementVerifier: EntitlementVerifier = PlayEntitlementVerifier(),
  private val reviewerAccessVerifier: ReviewerAccessVerifier = BackendReviewerAccessVerifier(),
) : PurchasesUpdatedListener {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val entitlementCache = appContext.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
  private val productDetails = AtomicReference<ProductDetails?>(null)
  private val currentPurchaseToken = AtomicReference<String?>(cachedToken())
  private val currentReviewerToken = AtomicReference<String?>(cachedReviewerToken())
  private val reviewerAccessIsValid = cachedReviewerAccessUntil() > System.currentTimeMillis() && cachedReviewerToken() != null
  private val _state =
    MutableStateFlow(
      BillingUiState(
        isRefreshing = true,
        isPro = cachedAccessUntil() > System.currentTimeMillis() && cachedToken() != null || reviewerAccessIsValid,
        isReviewerAccess = reviewerAccessIsValid,
      )
    )
  val state: StateFlow<BillingUiState> = _state.asStateFlow()

  val activePurchaseToken: String?
    get() = currentPurchaseToken.get().takeIf { cachedAccessUntil() > System.currentTimeMillis() }

  val activeReviewerToken: String?
    get() = currentReviewerToken.get().takeIf { cachedReviewerAccessUntil() > System.currentTimeMillis() }

  private val billingClient =
    BillingClient.newBuilder(appContext)
      .setListener(this)
      .enablePendingPurchases(
        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
      )
      .enableAutoServiceReconnection()
      .build()

  init {
    connect()
  }

  private fun connect() {
    billingClient.startConnection(
      object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
          if (result.responseCode == BillingResponseCode.OK) {
            _state.update { it.copy(isReady = true, message = null) }
            queryProduct()
            restorePurchases(showSuccess = false)
          } else {
            _state.update {
              it.copy(
                isReady = false,
                isRefreshing = false,
                message = result.userMessage("Google Play Billing is unavailable."),
              )
            }
          }
        }

        override fun onBillingServiceDisconnected() {
          _state.update { it.copy(isReady = false) }
        }
      }
    )
  }

  private fun queryProduct() {
    val product =
      QueryProductDetailsParams.Product.newBuilder()
        .setProductId(BuildConfig.PRO_PRODUCT_ID)
        .setProductType(ProductType.SUBS)
        .build()
    val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
    billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
      if (result.responseCode != BillingResponseCode.OK) {
        _state.update { it.copy(message = result.userMessage("Could not load Pro plans.")) }
        return@queryProductDetailsAsync
      }
      val details = detailsResult.productDetailsList.firstOrNull()
      productDetails.set(details)
      val offers =
        details?.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
          val recurringPhase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
          ProOffer(
            offerToken = offer.offerToken,
            basePlanId = offer.basePlanId,
            offerId = offer.offerId,
            formattedPrice = recurringPhase.formattedPrice,
            billingPeriod = recurringPhase.billingPeriod.toReadablePeriod(),
          )
        }.distinctBy { it.offerToken }
      _state.update {
        it.copy(
          offers = offers,
          message = if (details == null) "ClearWrite Pro is not configured in Google Play yet." else it.message,
        )
      }
    }
  }

  fun launchPurchase(activity: Activity, offerToken: String) {
    val details = productDetails.get()
    if (details == null) {
      _state.update { it.copy(message = "ClearWrite Pro is not available yet.") }
      queryProduct()
      return
    }
    val offerExists = details.subscriptionOfferDetails.orEmpty().any { it.offerToken == offerToken }
    if (!offerExists) {
      _state.update { it.copy(message = "That subscription option is no longer available. Try again.") }
      queryProduct()
      return
    }
    val productParams =
      BillingFlowParams.ProductDetailsParams.newBuilder()
        .setProductDetails(details)
        .setOfferToken(offerToken)
        .build()
    val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
    val result = billingClient.launchBillingFlow(activity, flowParams)
    if (result.responseCode != BillingResponseCode.OK) {
      _state.update { it.copy(message = result.userMessage("Could not start the purchase.")) }
    }
  }

  fun restorePurchases(showSuccess: Boolean = true) {
    if (!billingClient.isReady) {
      _state.update { it.copy(message = "Connecting to Google Play…") }
      connect()
      return
    }
    _state.update { it.copy(isRefreshing = true, message = null) }
    val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
    billingClient.queryPurchasesAsync(params) { result, purchases ->
      if (result.responseCode != BillingResponseCode.OK) {
        _state.update {
          it.copy(
            isRefreshing = false,
            message = result.userMessage("Could not restore purchases."),
          )
        }
        return@queryPurchasesAsync
      }
      processPurchases(purchases, showSuccess)
    }
  }

  override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
    when (result.responseCode) {
      BillingResponseCode.OK -> processPurchases(purchases.orEmpty(), showSuccess = true)
      BillingResponseCode.USER_CANCELED -> _state.update { it.copy(message = null) }
      BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases(showSuccess = true)
      else -> _state.update { it.copy(message = result.userMessage("The purchase did not complete.")) }
    }
  }

  private fun processPurchases(purchases: List<Purchase>, showSuccess: Boolean) {
    val purchase =
      purchases.firstOrNull {
        BuildConfig.PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
      }
    val pending =
      purchases.any {
        BuildConfig.PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PENDING
      }
    if (purchase == null) {
      clearPurchaseEntitlement()
      val reviewerAccess = hasValidReviewerAccess()
      _state.update {
        it.copy(
          isRefreshing = false,
          isPro = reviewerAccess,
          isReviewerAccess = reviewerAccess,
          message =
            when {
              pending -> "Your purchase is pending. Pro will activate after Google Play confirms payment."
              reviewerAccess -> if (showSuccess) "Reviewer access is active." else it.message
              showSuccess -> "No active ClearWrite Pro subscription was found."
              else -> it.message
            },
        )
      }
      return
    }
    verifyPurchase(purchase.purchaseToken, showSuccess)
  }

  private fun verifyPurchase(token: String, showSuccess: Boolean) {
    _state.update { it.copy(isRefreshing = true, message = "Verifying your subscription…") }
    scope.launch {
      entitlementVerifier.verify(token)
        .onSuccess { entitlement ->
          if (entitlement.active) {
            cacheEntitlement(token, entitlement.accessUntilMillis)
            currentPurchaseToken.set(token)
            _state.update {
              it.copy(
                isRefreshing = false,
                isPro = true,
                isReviewerAccess = false,
                message = if (showSuccess) "ClearWrite Pro is active." else null,
              )
            }
          } else {
            clearPurchaseEntitlement()
            val reviewerAccess = hasValidReviewerAccess()
            _state.update {
              it.copy(
                isRefreshing = false,
                isPro = reviewerAccess,
                isReviewerAccess = reviewerAccess,
                message = if (reviewerAccess) "Reviewer access is active." else "Google Play did not confirm an active subscription.",
              )
            }
          }
        }
        .onFailure { error ->
          val cacheStillValid = cachedAccessUntil() > System.currentTimeMillis() && cachedToken() == token
          val reviewerAccess = hasValidReviewerAccess()
          currentPurchaseToken.set(token.takeIf { cacheStillValid })
          _state.update {
            it.copy(
              isRefreshing = false,
              isPro = cacheStillValid || reviewerAccess,
              isReviewerAccess = !cacheStillValid && reviewerAccess,
              message = if (reviewerAccess) "Reviewer access is active." else error.message ?: "Could not verify the subscription.",
            )
          }
        }
    }
  }

  fun activateReviewerAccess(code: String) {
    val normalizedCode = code.trim()
    if (normalizedCode.isEmpty()) {
      _state.update { it.copy(message = "Enter the reviewer access code.") }
      return
    }
    _state.update { it.copy(isRefreshing = true, message = "Verifying reviewer access…") }
    scope.launch {
      reviewerAccessVerifier.unlock(normalizedCode)
        .onSuccess { entitlement ->
          if (
            entitlement.active &&
              entitlement.reviewToken.isNotBlank() &&
              entitlement.accessUntilMillis > System.currentTimeMillis()
          ) {
            cacheReviewerEntitlement(entitlement.reviewToken, entitlement.accessUntilMillis)
            currentReviewerToken.set(entitlement.reviewToken)
            _state.update {
              it.copy(
                isRefreshing = false,
                isPro = true,
                isReviewerAccess = true,
                message = "Reviewer access is active.",
              )
            }
          } else {
            clearReviewerEntitlement()
            _state.update {
              it.copy(isRefreshing = false, message = "The reviewer access code could not be verified.")
            }
          }
        }
        .onFailure { error ->
          _state.update {
            it.copy(
              isRefreshing = false,
              message = error.message ?: "Could not verify reviewer access.",
            )
          }
        }
    }
  }

  fun clearMessage() {
    _state.update { it.copy(message = null) }
  }

  fun close() {
    scope.cancel()
    billingClient.endConnection()
  }

  private fun cacheEntitlement(token: String, serverAccessUntilMillis: Long) {
    val now = System.currentTimeMillis()
    val cacheUntil = minOf(serverAccessUntilMillis, now + MAX_OFFLINE_CACHE_MILLIS)
    entitlementCache.edit().putString(KEY_TOKEN, token).putLong(KEY_ACCESS_UNTIL, cacheUntil).apply()
  }

  private fun clearPurchaseEntitlement() {
    currentPurchaseToken.set(null)
    entitlementCache.edit().remove(KEY_TOKEN).remove(KEY_ACCESS_UNTIL).apply()
  }

  private fun cacheReviewerEntitlement(token: String, accessUntilMillis: Long) {
    entitlementCache.edit()
      .putString(KEY_REVIEWER_TOKEN, token)
      .putLong(KEY_REVIEWER_ACCESS_UNTIL, accessUntilMillis)
      .apply()
  }

  private fun clearReviewerEntitlement() {
    currentReviewerToken.set(null)
    entitlementCache.edit().remove(KEY_REVIEWER_TOKEN).remove(KEY_REVIEWER_ACCESS_UNTIL).apply()
  }

  private fun hasValidReviewerAccess(): Boolean =
    cachedReviewerAccessUntil() > System.currentTimeMillis() && cachedReviewerToken() != null

  private fun cachedToken(): String? = entitlementCache.getString(KEY_TOKEN, null)

  private fun cachedAccessUntil(): Long = entitlementCache.getLong(KEY_ACCESS_UNTIL, 0L)

  private fun cachedReviewerToken(): String? = entitlementCache.getString(KEY_REVIEWER_TOKEN, null)

  private fun cachedReviewerAccessUntil(): Long = entitlementCache.getLong(KEY_REVIEWER_ACCESS_UNTIL, 0L)

  private fun BillingResult.userMessage(fallback: String): String =
    debugMessage.takeIf { it.isNotBlank() }?.let { "$fallback ($it)" } ?: fallback

  private companion object {
    const val CACHE_NAME = "clearwrite_billing"
    const val KEY_TOKEN = "verified_purchase_token"
    const val KEY_ACCESS_UNTIL = "verified_access_until"
    const val KEY_REVIEWER_TOKEN = "reviewer_access_token"
    const val KEY_REVIEWER_ACCESS_UNTIL = "reviewer_access_until"
    const val MAX_OFFLINE_CACHE_MILLIS = 24L * 60L * 60L * 1_000L
  }
}

private fun String.toReadablePeriod(): String =
  when (this) {
    "P1W" -> "week"
    "P1M" -> "month"
    "P3M" -> "3 months"
    "P6M" -> "6 months"
    "P1Y" -> "year"
    else -> "billing period"
  }
