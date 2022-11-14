package com.automattic.simplenote.billing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automattic.simplenote.R
import com.automattic.simplenote.viewmodels.IapViewModel

class SubscriptionDurationAdapter :
    ListAdapter<IapViewModel.PlansListItem, SubscriptionDurationAdapter.PlanListItemViewHolder>(
        SubscriptionOffersDiffCallback
    ) {

    override fun onBindViewHolder(
        holder: PlanListItemViewHolder,
        position: Int
    ) {
        val uiState = getItem(position)
        holder.onBind(uiState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlanListItemViewHolder = PlanListItemViewHolder(parent)

    class PlanListItemViewHolder(
        internal val parent: ViewGroup,
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.subscription_duration_list_row, parent, false)
    ) {
        private val planName = itemView.findViewById<TextView>(R.id.plan_name)
        private val planPrice = itemView.findViewById<TextView>(R.id.plan_price)
        private val container = itemView.findViewById<View>(R.id.container)

        fun onBind(uiState: IapViewModel.PlansListItem) {
            planName.setText(uiState.period)
            planPrice.text = uiState.price

            container.setOnClickListener {
                uiState.onTapListener.invoke(uiState.offerId, uiState.tracker)
            }
        }
    }

    object SubscriptionOffersDiffCallback :
        DiffUtil.ItemCallback<IapViewModel.PlansListItem>() {
        override fun areItemsTheSame(
            oldItem: IapViewModel.PlansListItem,
            newItem: IapViewModel.PlansListItem
        ): Boolean {
            return oldItem.offerId == newItem.offerId
        }

        override fun areContentsTheSame(
            oldItem: IapViewModel.PlansListItem,
            newItem: IapViewModel.PlansListItem
        ): Boolean {
            return oldItem.period == newItem.period && oldItem.price == newItem.price
        }
    }
}
