package com.automattic.simplenote.viewmodels

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.automattic.simplenote.R
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.models.Preferences
import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import java.lang.IllegalStateException

class IapViewModel(application: Application) :
    AndroidViewModel(application), PurchasesUpdatedListener, ProductDetailsResponseListener,
    Bucket.OnNetworkChangeListener<Preferences>, Bucket.OnSaveObjectListener<Preferences>,
    Bucket.OnDeleteObjectListener<Preferences> {
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
        startBillingConnection()
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

    private fun startBillingConnection() {
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Billing response OK")
                        queryPurchases()
                    } else {
                        Log.e(TAG, billingResult.debugMessage)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.i(TAG, "Billing connection disconnected")
                    startBillingConnection()
                }
            })
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error starting billing connection: ${e.message}")
        }
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
        private const val TAG: String = "MainViewModel"

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
}
