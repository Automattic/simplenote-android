package com.automattic.simplenote.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automattic.simplenote.databinding.CollaboratorRowBinding
import com.automattic.simplenote.databinding.CollaboratorsHeaderBinding

class CollaboratorsAdapter(
    private val onDeleteClick: (collaborator: String) -> Unit,
) : ListAdapter<CollaboratorsAdapter.CollaboratorDataItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                val binding = CollaboratorsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CollaboratorHeaderViewHolder(binding)
            }
            ITEM_VIEW_TYPE_ITEM -> {
                val binding = CollaboratorRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CollaboratorViewHolder(binding, onDeleteClick)
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CollaboratorDataItem.HeaderItem -> ITEM_VIEW_TYPE_HEADER
            is CollaboratorDataItem.CollaboratorItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CollaboratorViewHolder) {
            holder.bind(getItem(position) as CollaboratorDataItem.CollaboratorItem)
        }
    }

    sealed class CollaboratorDataItem {
        abstract val id: Int
        object HeaderItem : CollaboratorDataItem() {
            override val id = Int.MIN_VALUE
        }
        data class CollaboratorItem(val email: String) : CollaboratorDataItem() {
            override val id = email.hashCode()
        }
    }

    class CollaboratorHeaderViewHolder(
        binding: CollaboratorsHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class CollaboratorViewHolder(
        private val binding: CollaboratorRowBinding,
        private val onDeleteClick: (collaborator: String) -> Unit
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(collaborator: CollaboratorDataItem.CollaboratorItem) {
            binding.collaboratorText.text = collaborator.email
            binding.collaboratorRemoveButton.setOnClickListener { onDeleteClick(collaborator.email) }
        }
    }

    companion object {
        private const val ITEM_VIEW_TYPE_HEADER = 0
        private const val ITEM_VIEW_TYPE_ITEM = 1
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CollaboratorDataItem>() {
            override fun areItemsTheSame(oldItem: CollaboratorDataItem, newItem: CollaboratorDataItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CollaboratorDataItem, newItem: CollaboratorDataItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
