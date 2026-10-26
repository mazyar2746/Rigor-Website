package com.example.billing

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Typealias to allow Payment terminology as required.
 */
typealias BillingClient = Payment

sealed class BillingState {
    data object Idle : BillingState()
    data object Connecting : BillingState()
    data object Connected : BillingState()
    data class Purchasing(val sku: String) : BillingState()
    data class PurchaseSuccess(val purchaseInfo: PurchaseInfo) : BillingState()
    data object PurchaseCanceled : BillingState()
    data class Error(val message: String) : BillingState()
}

/**
 * Robust Cafe Bazaar In-App Billing Manager using Poolakey SDK.
 * Manages connections, subscription purchases via Activity Result API, and query of subscribed products.
 * Includes graceful fallback and crash protection when running on emulators, web previews,
 * or devices without the Cafe Bazaar application.
 */
class BillingManager(
    private val context: Context,
    val rsaPublicKey: String = BAZAAR_RSA_PUBLIC_KEY
) {
    companion object {
        const val TAG = "BillingManager"
        const val SKU_RIGOR_PRO = "rigor_pro_subscription"
        const val BAZAAR_RSA_PUBLIC_KEY =
            "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwCnUP5SCfkI4qRLcrj9bS3IrPQzFN76ET0gASVUdwRPyI8PCBYQDCECwjopWPcQM3R2GDq6bNjMRZehmW5A4ekYqDBAmdlMJEMmHtA/57RzAO6IOaLNz4ZlKLnGADyBvfVbHBQNkc3rLAlwfvgZtpdFMyPvrrxJXa2VMOigM+e+IRiVtMXxBwORMOgGFQyzVnuljLFfZA0jH52HamxYrbZVjawF9kheTWhsyv5xc4sCAwEAAQ=="
    }

    private val paymentConfig: PaymentConfiguration? = try {
        PaymentConfiguration(
            localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = rsaPublicKey),
            shouldSupportSubscription = true
        )
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to create PaymentConfiguration", t)
        null
    }

    val billingClient: Payment? = try {
        paymentConfig?.let { config ->
            Payment(
                context = context,
                config = config
            )
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to instantiate Poolakey Payment client", t)
        null
    }

    private var connection: Connection? = null

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    fun connect(
        onConnected: (() -> Unit)? = null,
        onFailed: ((Throwable) -> Unit)? = null
    ) {
        val client = billingClient
        if (client == null) {
            val msg = "Poolakey billing is unavailable on this device (Cafe Bazaar app required)."
            Log.w(TAG, msg)
            _billingState.value = BillingState.Error(msg)
            onFailed?.invoke(IllegalStateException(msg))
            return
        }

        _billingState.value = BillingState.Connecting
        try {
            connection = client.connect {
                connectionSucceed {
                    Log.d(TAG, "Cafe Bazaar Poolakey Billing connected successfully")
                    _billingState.value = BillingState.Connected
                    onConnected?.invoke()
                }
                connectionFailed { throwable ->
                    val errorMsg = throwable.message ?: "Failed to connect to Cafe Bazaar billing"
                    Log.e(TAG, "Connection failed: $errorMsg", throwable)
                    _billingState.value = BillingState.Error(errorMsg)
                    onFailed?.invoke(throwable)
                }
                disconnected {
                    Log.w(TAG, "Poolakey disconnected")
                    _billingState.value = BillingState.Idle
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during billing connect", e)
            _billingState.value = BillingState.Error(e.message ?: "Unknown billing error")
            onFailed?.invoke(e)
        }
    }

    /**
     * Start the purchase flow for a subscription using ActivityResultRegistry.
     */
    fun subscribe(
        registry: ActivityResultRegistry,
        sku: String = SKU_RIGOR_PRO,
        payload: String = "rigor_pro_applet_${System.currentTimeMillis()}",
        onSuccess: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val client = billingClient
        if (client == null) {
            val msg = "Cafe Bazaar application is required to subscribe via Bazaar."
            _billingState.value = BillingState.Error(msg)
            onError(msg)
            return
        }

        _billingState.value = BillingState.Purchasing(sku)

        val purchaseRequest = PurchaseRequest(
            productId = sku,
            payload = payload
        )

        try {
            client.subscribeProduct(
                registry = registry,
                request = purchaseRequest
            ) {
                purchaseFlowBegan {
                    Log.d(TAG, "Subscription purchase flow began for SKU: $sku")
                }
                failedToBeginFlow { throwable ->
                    val msg = throwable.message ?: "Unable to launch Cafe Bazaar purchase screen"
                    Log.e(TAG, "Failed to begin purchase flow: $msg", throwable)
                    _billingState.value = BillingState.Error(msg)
                    onError(msg)
                }
                purchaseSucceed { purchaseInfo ->
                    Log.d(TAG, "Purchase succeeded: token=${purchaseInfo.purchaseToken}")
                    _billingState.value = BillingState.PurchaseSuccess(purchaseInfo)
                    onSuccess(purchaseInfo)
                }
                purchaseCanceled {
                    Log.d(TAG, "Purchase canceled by user")
                    _billingState.value = BillingState.PurchaseCanceled
                    onCanceled()
                }
                purchaseFailed { throwable ->
                    val msg = throwable.message ?: "Subscription payment failed"
                    Log.e(TAG, "Purchase failed: $msg", throwable)
                    _billingState.value = BillingState.Error(msg)
                    onError(msg)
                }
            }
        } catch (e: Throwable) {
            val msg = e.message ?: "Error launching subscription flow"
            Log.e(TAG, "Exception in subscribeProduct", e)
            _billingState.value = BillingState.Error(msg)
            onError(msg)
        }
    }

    /**
     * Queries active subscriptions to verify if the user already has an active Rigor Pro subscription.
     */
    fun checkActiveSubscriptions(
        onResult: (hasActiveSub: Boolean, purchaseInfo: PurchaseInfo?) -> Unit
    ) {
        val client = billingClient
        if (client == null) {
            onResult(false, null)
            return
        }

        try {
            client.getSubscribedProducts {
                querySucceed { purchasedEntities ->
                    val matching = purchasedEntities.firstOrNull { it.productId == SKU_RIGOR_PRO }
                    val hasActive = matching != null
                    Log.d(TAG, "Active subscriptions query: found=${purchasedEntities.size}, active=$hasActive")
                    onResult(hasActive, matching)
                }
                queryFailed { throwable ->
                    Log.w(TAG, "Failed to query active subscriptions: ${throwable.message}")
                    onResult(false, null)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Exception querying active subscriptions", e)
            onResult(false, null)
        }
    }

    fun release() {
        try {
            connection?.disconnect()
            connection = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error disconnecting billing", e)
        }
    }
}
