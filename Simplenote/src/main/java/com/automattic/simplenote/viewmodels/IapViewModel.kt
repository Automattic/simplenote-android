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
import com.automattic.simplenote.utils.PrefUtils

class IapViewModel(application: Application) :
    AndroidViewModel(application), PurchasesUpdatedListener, ProductDetailsResponseListener {
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

    init {
        startBillingConnection()
    }

    private val productDetails = ArrayList<ProductDetails>()

    var isStarted: Boolean = false

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

    private fun onPlanSelected(offerToken: String) {
        _onPurchaseRequest.postValue(offerToken)
        _plansBottomSheetVisibility.postValue(false)
    }

    // Billing

    private fun startBillingConnection() {
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
                PrefUtils.setIsSubscriptionActive(getApplication(), hasActiveSubscription)
                _iapBannerVisibility.postValue(!hasActiveSubscription)
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
                        PrefUtils.setIsSubscriptionActive(getApplication(), true)
                        _iapBannerVisibility.postValue(false)
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

    data class PlansListItem(
        val offerId: String,
        @StringRes val period: Int,
        val price: String,
        val onTapListener: ((String) -> Unit)
    )

    data class IapSnackbarMessage(@StringRes val messageResId: Int)

    companion object {
        private const val TAG: String = "MainViewModel"

        private const val SUSTAINER_SUB_PRODUCT = "sustainer_subscription"

        private val LIST_OF_PRODUCTS = listOf(SUSTAINER_SUB_PRODUCT)
    }
}