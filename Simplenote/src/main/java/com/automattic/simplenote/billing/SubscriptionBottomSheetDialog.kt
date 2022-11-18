package com.automattic.simplenote.billing

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.automattic.simplenote.BottomSheetDialogBase
import com.automattic.simplenote.R
import com.automattic.simplenote.analytics.AnalyticsTracker
import com.automattic.simplenote.databinding.BottomSheetSubscriptionsBinding
import com.automattic.simplenote.viewmodels.IapViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class SubscriptionBottomSheetDialog : BottomSheetDialogBase() {
    private lateinit var viewModel: IapViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_subscriptions, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(BottomSheetSubscriptionsBinding.bind(view)) {
            viewModel = ViewModelProvider(
                activity as FragmentActivity
            ).get(IapViewModel::class.java)

            val adapter = SubscriptionDurationAdapter()
            viewModel.planOffers.observe(viewLifecycleOwner) { offers ->
                adapter.submitList(offers){
                    view.findViewById<View>(R.id.plans_loading_progress).visibility = View.GONE
                }
            }

            viewModel.onPurchaseRequest.observe(viewLifecycleOwner) { offerToken ->
                viewModel.startPurchaseFlow(
                    offerToken,
                    requireActivity()
                )
            }

            contentRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
            contentRecyclerView.adapter = adapter

            viewModel.onBottomSheetDisplayed()

            dialog?.setOnShowListener { dialogInterface ->
                val sheetDialog = dialogInterface as? BottomSheetDialog

                val bottomSheet = sheetDialog?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) as? FrameLayout

                bottomSheet?.let {
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        AnalyticsTracker.track(AnalyticsTracker.Stat.IAP_PLANS_DIALOG_DISMISSED)
    }

    companion object {
        @JvmStatic
        val TAG: String = SubscriptionBottomSheetDialog::class.java.simpleName
    }
}
