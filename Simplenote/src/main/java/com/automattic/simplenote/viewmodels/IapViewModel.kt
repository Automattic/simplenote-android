package com.automattic.simplenote.viewmodels

import android.app.Activity
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.di.IO_THREAD
import com.automattic.simplenote.models.Preferences
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.min

private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

@HiltViewModel
class IapViewModel @Inject constructor(
    application: Simplenote, @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) :
    AndroidViewModel(application), PurchasesUpdatedListener, ProductDetailsResponseListener,
    Bucket.OnNetworkChangeListener<Preferences>, Bucket.OnSaveObjectListener<Preferences>,
    Bucket.OnDeleteObjectListener<Preferences> {
    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    private val billingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _onPurchaseRequest = SingleLiveEvent<String>()
    val onPurchaseRequest: LiveData<String> = _onPurchaseRequest

    private val _planOffers = MutableLiveData<List<PlansListItem>?>()
    val planOffers: LiveData<List<PlansListItem>?> = _planOffers

    private val _plansBottomSheetVisibility = MutableLiveData<Boolean>()
    val plansBottomSheetVisibility: LiveData<Boolean> = _plansBottomSheetVisibility

    private val _iapBannerVisibility = MutableLiveData<Boolean>()
    val iapBannerVisibility: LiveData<Boolean> = _iapBannerVisibility

    private val _snackbarMessage = SingleLiveEvent<IapSnackbarMessage>()
    val snackbarMessage: LiveData<IapSnackbarMessage> = _snackbarMessage

    private val preferencesBucket = getApplication<Simplenote>().preferencesBucket

    init {
        preferencesBucket.addOnNetworkChangeListener(this)
        preferencesBucket.addOnSaveObjectListener(this)
        preferencesBucket.addOnDeleteObjectListener(this)
        viewModelScope.launch {
            startBillingConnection()
        }
    }

    private val productDetails = ArrayList<ProductDetails>()

    private var isStarted: Boolean = false

    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
    }

    fun onIapBannerClicked() {
        _plansBottomSheetVisibility.postValue(true)
    }

    fun onBottomSheetDisplayed() {
        queryProductDetails()
    }

    private fun onPlanSelected(offerToken: String, tracker: AnalyticsTracker.Stat) {
        AnalyticsTracker.track(tracker)
        _onPurchaseRequest.postValue(offerToken)
        _plansBottomSheetVisibility.postValue(false)
    }

    // Billing

    private val billingListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Log.i(TAG, "Billing response OK")
                    reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                    queryPurchases()
                }

                else -> {
                    val errorCode = billingResponseCodeToString(billingResult.responseCode)
                    val errorMsg = "Unable to establish connection with the billing API, " +
                            "error: $errorCode."
                    Log.e(TAG, errorMsg)

                    if (shouldRetryConnection(billingResult.responseCode)) {
                        viewModelScope.launch {
                            retryBillingServiceConnectionWithExponentialBackoff()
                        }
                    }
                }
            }
        }

        override fun onBillingServiceDisconnected() {
            Log.i(TAG, "Billing connection disconnected")
            viewModelScope.launch {
                retryBillingServiceConnectionWithExponentialBackoff()
            }
        }
    }

    private suspend fun startBillingConnection() = withContext(ioDispatcher) {
        try {
            billingClient.startConnection(billingListener)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error starting billing connection: ${e.message}")
        }
    }

    private suspend fun retryBillingServiceConnectionWithExponentialBackoff() {
        Log.i(TAG, "Retrying billing service connection after $reconnectMilliseconds MS delay")
        delay(reconnectMilliseconds)
        startBillingConnection()

        reconnectMilliseconds =
            min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        for (product in LIST_OF_PRODUCTS) {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

        }
        params.setProductList(productList).let { productDetailsParams ->
            billingClient.queryProductDetailsAsync(productDetailsParams.build(), this)
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
        }
        // Query for existing subscription products that have been purchased.
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSubscription = purchaseList.isNotEmpty()
                if (hasActiveSubscription && doesNotHaveSubscriptionOnOtherPlatforms()) {
                    val preferences = preferencesBucket.get(Preferences.PREFERENCES_OBJECT_KEY)
                    preferences.setActiveSubscription(purchaseList.first().purchaseTime / 1000)
                } else if (doesNotHaveSubscriptionOnOtherPlatforms()) {
                    val preferences = preferencesBucket.get(Preferences.PREFERENCES_OBJECT_KEY)
                    preferences.removeActiveSubscription()
                }
                updateIapBannerVisibility()
            } else {
                Log.e(TAG, billingResult.debugMessage)
            }
        }
    }

    fun startPurchaseFlow(
        offerToken: String,
        activity: Activity
    ) {
        val billingParams =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setOfferToken(offerToken)
                        .setProductDetails(productDetails.first()) // we have only one product
                        .build()
                )
            )

        billingClient.launchBillingFlow(
            activity,
            billingParams.build()
        )
    }

    // Perform new subscription purchases' acknowledgement client side.
    private fun acknowledgePurchases(purchase: Purchase?) {
        purchase?.let {
            if (!it.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(it.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(
                    params
                ) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        val product = purchase.packageName + "." + purchase.products.firstOrNull()
                        AnalyticsTracker.track(
                            AnalyticsTracker.Stat.IAP_PURCHASE_COMPLETED,
                            mapOf("product" to product)
                        )

                        val preferences = preferencesBucket.get(Preferences.PREFERENCES_OBJECT_KEY)
                        preferences.setActiveSubscription(purchase.purchaseTime / 1000)

                        _snackbarMessage.postValue(IapSnackbarMessage(R.string.subscription_purchase_success))
                    } else {
                        _snackbarMessage.postValue(IapSnackbarMessage(R.string.subscription_purchase_error))
                    }
                }
            }
        }
    }

    // Listeners

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
            && !purchases.isNullOrEmpty()
        ) {
            // Handle the purchases
            for (purchase in purchases) {
                acknowledgePurchases(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.e(TAG, "User has cancelled")
        } else {
            // Handle any other error codes.
            _snackbarMessage.postValue(IapSnackbarMessage(R.string.subscription_purchase_error))
        }
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsList: MutableList<ProductDetails>
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                productDetails.clear()
                productDetails.addAll(productDetailsList)

                val products =
                    productDetailsList.first().subscriptionOfferDetails?.map { offerDetails ->
                        PlansListItem(
                            offerId = offerDetails.offerToken,
                            period = periodCodeToResource(
                                offerDetails.pricingPhases
                                    .pricingPhaseList.first().billingPeriod
                            ),
                            price = offerDetails.pricingPhases.pricingPhaseList.first().formattedPrice,
                            tracker = periodCodeToTracker(
                                offerDetails.pricingPhases
                                    .pricingPhaseList.first().billingPeriod
                            ),
                            onTapListener = this::onPlanSelected
                        )
                    }

                _planOffers.postValue(products)
            }
            else -> {
                Log.i(TAG, "onProductDetailsResponse: $responseCode $debugMessage")
                _plansBottomSheetVisibility.postValue(false)
                _snackbarMessage.postValue(IapSnackbarMessage(R.string.subscription_plans_fetching_error))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }

    private fun periodCodeToResource(code: String): Int {
        return when (code) {
            "P1W" -> R.string.subscription_weekly
            "P1M" -> R.string.subscription_monthly
            "P3M" -> R.string.subscription_three_month
            "P6M" -> R.string.subscription_six_month
            "P1Y" -> R.string.subscription_yearly_month
            else -> R.string.subscription_unknown
        }
    }

    private fun periodCodeToTracker(code: String): AnalyticsTracker.Stat {
        return when (code) {
            "P1M" -> AnalyticsTracker.Stat.IAP_MONTHLY_BUTTON_TAPPED
            "P1Y" -> AnalyticsTracker.Stat.IAP_YEARLY_BUTTON_TAPPED
            else -> AnalyticsTracker.Stat.IAP_UNKNOWN_BUTTON_TAPPED
        }
    }

    data class PlansListItem(
        val offerId: String,
        @StringRes val period: Int,
        val price: String,
        val tracker: AnalyticsTracker.Stat,
        val onTapListener: ((String, AnalyticsTracker.Stat) -> Unit)
    )

    data class IapSnackbarMessage(@StringRes val messageResId: Int)

    companion object {
        private const val TAG: String = "IapViewModel"

        private const val SUSTAINER_SUB_PRODUCT = "sustainer_subscription"

        private val LIST_OF_PRODUCTS = listOf(SUSTAINER_SUB_PRODUCT)
    }

    private fun doesNotHaveSubscriptionOnOtherPlatforms(): Boolean {
        val preferences = preferencesBucket.get(Preferences.PREFERENCES_OBJECT_KEY)

        preferences?.let {
            val currentSubscriptionPlatform = preferences.currentSubscriptionPlatform

            return currentSubscriptionPlatform == null
                    || currentSubscriptionPlatform == Preferences.SubscriptionPlatform.ANDROID
        }
        return false
    }

    override fun onNetworkChange(
        bucket: Bucket<Preferences>?,
        type: Bucket.ChangeType?,
        key: String?
    ) {
        updateIapBannerVisibility()
    }

    override fun onSaveObject(bucket: Bucket<Preferences>?, `object`: Preferences?) {
        updateIapBannerVisibility()
    }

    override fun onDeleteObject(bucket: Bucket<Preferences>?, `object`: Preferences?) {
        updateIapBannerVisibility()
    }

    private fun updateIapBannerVisibility() = try {
        val preferences: Preferences = preferencesBucket.get(Preferences.PREFERENCES_OBJECT_KEY)
        if (preferences.currentSubscriptionPlatform != null) {
            _iapBannerVisibility.postValue(false)
        } else {
            _iapBannerVisibility.postValue(true)
        }
    } catch (ignore: BucketObjectMissingException) {
        _iapBannerVisibility.postValue(false)
    }

    private fun billingResponseCodeToString(responseCode: Int): String {
        return when(responseCode) {
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Service Timeout"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Feature not supported"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Service disconnected"
            BillingClient.BillingResponseCode.OK -> "Success"
            BillingClient.BillingResponseCode.USER_CANCELED -> "User canceled"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Service unavailable"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing unavailable"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Item unavailable"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer error"
            BillingClient.BillingResponseCode.ERROR -> "Generic error"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Item already owned"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Item not owned"
            else -> "Unknown error code"
        }
    }

    private fun shouldRetryConnection(responseCode: Int) = when(responseCode) {
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> true
        else -> false
    }
}
