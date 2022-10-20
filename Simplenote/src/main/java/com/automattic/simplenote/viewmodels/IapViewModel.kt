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
import com.automattic.simplenote.billing.BillingClientWrapper

class IapViewModel(application: Application) :
    AndroidViewModel(application), PurchasesUpdatedListener, ProductDetailsResponseListener {
    private val billingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _billingConnectionState = MutableLiveData(false)
    val billingConnectionState: LiveData<Boolean> = _billingConnectionState

    private val _onPurchaseRequest = MutableLiveData<PurchaseRequest>()
    val onPurchaseRequest: LiveData<PurchaseRequest> = _onPurchaseRequest


    private val _planOffers = MutableLiveData<List<PlansListItem>?>()
    val planOffers: LiveData<List<PlansListItem>?> = _planOffers

    // Current Purchases
    private val _purchases = MutableLiveData<List<Purchase>>(listOf())
    val purchases: LiveData<List<Purchase>> = _purchases

    // Start the billing connection when the viewModel is initialized.
    init {
        startBillingConnection()
    }

    data class PurchaseRequest(val offerToke: String, val productDetails: ProductDetails)

    data class IapState(val isVisible: Boolean)

    data class Plan(val offerId: String, @StringRes val period: Int, val price: String)


    data class PlansListItem(val plan: Plan, val purchaseDetails: PurchaseRequest, val onTapListener: ((PurchaseRequest) -> Unit))

    // Establish a connection to Google Play.
    fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing response OK")
                    // The BillingClient is ready. You can query purchases and product details here
//                    queryPurchases()
//                    queryProductDetails()
//                    billingConnectionState.postValue(true)
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

    // Query Google Play Billing for products available to sell and present them in the UI
    fun queryProductDetails() {

        _planOffers.postValue(emptyList())

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
            Log.i(TAG, "queryProductDetailsAsync")
            billingClient.queryProductDetailsAsync(productDetailsParams.build(), this)
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
        }
        // Query for existing subscription products that have been purchased.
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!purchaseList.isNullOrEmpty()) {
                    _purchases.value = purchaseList
                } else {
                    _purchases.value = emptyList()
                }

            } else {
                Log.e(TAG, billingResult.debugMessage)
            }
        }
    }

    /**
     * Retrieves all eligible base plans and offers using tags from ProductDetails.
     *
     * @param offerDetails offerDetails from a ProductDetails returned by the library.
     * @param tag string representing tags associated with offers and base plans.
     *
     * @return the eligible offers and base plans in a list.
     *
     */
    private fun retrieveEligibleOffers(
        offerDetails: MutableList<ProductDetails.SubscriptionOfferDetails>,
        tag: String
    ): List<ProductDetails.SubscriptionOfferDetails> {
        val eligibleOffers = emptyList<ProductDetails.SubscriptionOfferDetails>().toMutableList()
        offerDetails.forEach { offerDetail ->
            if (offerDetail.offerTags.contains(tag)) {
                eligibleOffers.add(offerDetail)
            }
        }

        return eligibleOffers
    }

    /**
     * Calculates the lowest priced offer amongst all eligible offers.
     * In this implementation the lowest price of all offers' pricing phases is returned.
     * It's possible the logic can be implemented differently.
     * For example, the lowest average price in terms of month could be returned instead.
     *
     * @param offerDetails List of of eligible offers and base plans.
     *
     * @return the offer id token of the lowest priced offer.
     */
    private fun leastPricedOfferToken(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>
    ): String {
        var offerToken = String()
        var leastPricedOffer: ProductDetails.SubscriptionOfferDetails
        var lowestPrice = Int.MAX_VALUE

        if (!offerDetails.isNullOrEmpty()) {
            for (offer in offerDetails) {
                for (price in offer.pricingPhases.pricingPhaseList) {
                    if (price.priceAmountMicros < lowestPrice) {
                        lowestPrice = price.priceAmountMicros.toInt()
                        leastPricedOffer = offer
                        offerToken = leastPricedOffer.offerToken
                    }
                }
            }
        }
        return offerToken
    }

    /**
     * BillingFlowParams Builder for normal purchases.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param offerToken the least priced offer's offer id token returned by
     * [leastPricedOfferToken].
     *
     * @return [BillingFlowParams] builder.
     */
    private fun billingFlowParamsBuilder(
        productDetails: ProductDetails,
        offerToken: String
    ): BillingFlowParams.Builder {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        )
    }

    /**
     * Use the Google Play Billing Library to make a purchase.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param currentPurchases List of current [Purchase] objects needed for upgrades or downgrades.
     * @param billingClient Instance of [BillingClientWrapper].
     * @param activity [Activity] instance.
     */
    fun buy(
        offerToken: String,
        productDetails: ProductDetails,
        activity: Activity
    ) {
        val billingParams =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setOfferToken(offerToken)
                        .setProductDetails(productDetails)
                        .build()
                )
            )

        billingClient.launchBillingFlow(
            activity,
            billingParams.build()
        )
    }

    // When an activity is destroyed the viewModel's onCleared is called, so we terminate the
    // billing connection.
    override fun onCleared() {
        billingClient.endConnection()
    }


    fun periodCodeToResource(code: String): Int {
        return when (code) {
            "P1W" -> R.string.subscription_weekly
            "P1M" -> R.string.subscription_monthly
            "P3M" -> R.string.subscription_three_month
            "P6M" -> R.string.subscription_six_month
            "P1Y" -> R.string.subscription_yearly_month
            else -> R.string.subscription_unknown
        }
    }


    companion object {
        private const val TAG: String = "MainViewModel"

        private const val MAX_CURRENT_PURCHASES_ALLOWED = 1

        // List of subscription product offerings
        private const val SUSTAINER_SUB = "sustainer_subscription"

        private val LIST_OF_PRODUCTS = listOf(SUSTAINER_SUB)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
            && !purchases.isNullOrEmpty()
        ) {
            // Post new purchase List to _purchases
//            _purchases.value = purchases

            // Then, handle the purchases
            for (purchase in purchases) {
                acknowledgePurchases(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.e(TAG, "User has cancelled")
        } else {
            // Handle any other error codes.
        }
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
                        Log.v(TAG, "aknowledged")
                    }
                }
            }
        }
    }

    private fun onPlanSelected(productDetails: PurchaseRequest){
        _onPurchaseRequest.postValue(productDetails)
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsList: MutableList<ProductDetails>
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                var newMap = emptyMap<String, ProductDetails>()
                if (productDetailsList.isNullOrEmpty()) {
                    Log.e(
                        TAG,
                        "onProductDetailsResponse: " +
                                "Found null or empty ProductDetails. " +
                                "Check to see if t`he Products you requested are correctly " +
                                "published in the Google Play Console."
                    )
                } else {
                    newMap = productDetailsList.associateBy {
                        it.productId
                    }
                }

                val products = productDetailsList.first().subscriptionOfferDetails?.map { offerDetails ->
                        PlansListItem(
                            Plan(
                                offerId = offerDetails.offerToken,
                                period = periodCodeToResource(offerDetails.pricingPhases.pricingPhaseList.first().billingPeriod),
                                price = offerDetails.pricingPhases.pricingPhaseList.first().formattedPrice
                            ),
                            purchaseDetails = PurchaseRequest(offerDetails.offerToken, productDetailsList.first()),
                            onTapListener = this::onPlanSelected
                        )
                    }


                _planOffers.postValue(products)
            }
            else -> {
                Log.i(TAG, "onProductDetailsResponse: $responseCode $debugMessage")
            }
        }
    }
}