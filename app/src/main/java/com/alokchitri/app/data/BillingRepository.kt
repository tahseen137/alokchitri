package com.alokchitri.app.data

import android.app.Activity
import android.content.Context
import com.alokchitri.app.AppConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the Upgrade screen needs from Google Play. Prices are Play's formatted strings, never our own. */
data class BillingState(
    /** Lifetime unlock owned (Play says so, or the last time we could ask it did). */
    val unlocked: Boolean = false,
    val unlockPrice: String? = null,
    val topUpPrice: String? = null,
    /** Play reachable and the products loaded; false shows "try again" and disables the buy buttons. */
    val ready: Boolean = false,
    /** A purchase is waiting for payment (e.g. cash at a shop); Play completes it later. */
    val pending: Boolean = false,
)

/**
 * Google Play Billing for the two one-time products in [AppConfig]. Owned purchases are re-read on every
 * [connect], so a reinstall or a second phone restores the unlock; top-ups are consumed and turned into
 * feedback credits as soon as Play reports them, including ones bought while the app was closed.
 */
class BillingRepository(
    context: Context,
    private val prefs: Prefs,
    private val quota: FeedbackQuota,
    private val scope: CoroutineScope,
    /** Called after credits were added or the unlock changed, so screens re-read the quota. */
    private val onEntitlementsChanged: () -> Unit,
) {
    private val _state = MutableStateFlow(BillingState(unlocked = prefs.unlocked))
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private var products: Map<String, ProductDetails> = emptyMap()

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                scope.launch { purchases.forEach { handle(it) } }
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (client.isReady) {
            scope.launch { refresh() }
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { refresh() }
                else _state.update { it.copy(ready = false) }
            }
            override fun onBillingServiceDisconnected() = _state.update { it.copy(ready = false) }
        })
    }

    /** Opens Play's purchase sheet; false if the product isn't loaded (the button should be disabled then). */
    fun buy(activity: Activity, productId: String): Boolean {
        val details = products[productId] ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    private suspend fun refresh() {
        val query = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(AppConfig.PRODUCT_UNLOCK, AppConfig.PRODUCT_TOPUP).map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
            },
        ).build()
        val details = client.queryProductDetails(query)
        products = details.productDetailsList.orEmpty().associateBy { it.productId }
        _state.update {
            it.copy(
                unlockPrice = products[AppConfig.PRODUCT_UNLOCK]?.oneTimePurchaseOfferDetails?.formattedPrice,
                topUpPrice = products[AppConfig.PRODUCT_TOPUP]?.oneTimePurchaseOfferDetails?.formattedPrice,
                ready = details.billingResult.responseCode == BillingClient.BillingResponseCode.OK && products.isNotEmpty(),
            )
        }

        val owned = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (owned.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        // Play is the source of truth: a refunded unlock disappears from this list.
        setUnlocked(owned.purchasesList.any { AppConfig.PRODUCT_UNLOCK in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED })
        owned.purchasesList.forEach { handle(it) }
    }

    private suspend fun handle(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _state.update { it.copy(pending = true) }
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        _state.update { it.copy(pending = false) }

        if (AppConfig.PRODUCT_UNLOCK in purchase.products) {
            // Unacknowledged purchases are refunded by Play after three days.
            if (!purchase.isAcknowledged) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
            }
            setUnlocked(true)
        }
        if (AppConfig.PRODUCT_TOPUP in purchase.products) {
            // Credit only after Play confirms the consume, so a failed consume can't be credited twice.
            val result = client.consumePurchase(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                quota.addTopUp(AppConfig.TOPUP_CREDITS * purchase.quantity)
                onEntitlementsChanged()
            }
        }
    }

    private fun setUnlocked(unlocked: Boolean) {
        prefs.unlocked = unlocked // FeedbackQuota.dailyLimit follows this
        if (_state.value.unlocked != unlocked) {
            _state.update { it.copy(unlocked = unlocked) }
        }
        onEntitlementsChanged()
    }
}
