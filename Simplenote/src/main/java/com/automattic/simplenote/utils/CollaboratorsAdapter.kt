package com.automattic.simplenote.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automattic.simplenote.databinding.CollaboratorRowBinding

class CollaboratorsAdapter(
    private val onDeleteClick: (collaborator: String) -> Unit,
) : ListAdapter<String, CollaboratorsAdapter.CollaboratorViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollaboratorViewHolder {
        val binding = CollaboratorRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollaboratorViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CollaboratorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CollaboratorViewHolder(
        private val binding: CollaboratorRowBinding,
        private val onDeleteClick: (collaborator: String) -> Unit
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(collaborator: String) {
            binding.collaboratorText.text = collaborator
            binding.collaboratorRemoveButton.setOnClickListener { onDeleteClick(collaborator) }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}
