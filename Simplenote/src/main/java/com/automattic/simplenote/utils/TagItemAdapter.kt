package com.automattic.simplenote.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automattic.simplenote.databinding.TagsListRowBinding
import com.automattic.simplenote.models.TagItem

class TagItemAdapter(
        private val onEditClick: (tagItem: TagItem) -> Unit,
        private val onDeleteClick: (tagItem: TagItem) -> Unit,
        private val onLongDeleteClick: (view: View) -> Unit,
) : ListAdapter<TagItem, TagItemAdapter.TagItemViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagItemViewHolder {
        val binding = TagsListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagItemViewHolder(
                binding,
                onEditClick,
                onDeleteClick,
                onLongDeleteClick
        )
    }

    override fun onBindViewHolder(holder: TagItemViewHolder, position: Int) {
        val currentTagItem = getItem(position)
        holder.bind(currentTagItem)
    }

    class TagItemViewHolder(
            private val binding: TagsListRowBinding,
            private val onEditClick: (tagItem: TagItem) -> Unit,
            private val onDeleteClick: (tagItem: TagItem) -> Unit,
            private val onLongDeleteClick: (view: View) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(currentTagItem: TagItem) {
            binding.apply {
                tagName.text = currentTagItem.tag.name
                tagCount.text = if (currentTagItem.noteCount > 0) currentTagItem.noteCount.toString() else ""
                tagTrash.setOnClickListener {
                    onDeleteClick(currentTagItem)
                }
                tagTrash.setOnLongClickListener {
                    onLongDeleteClick(it)
                    true
                }

                binding.root.setOnClickListener {
                    onEditClick(currentTagItem)
                }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TagItem>() {
            override fun areItemsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
                return oldItem.tag.simperiumKey == newItem.tag.simperiumKey
            }

            override fun areContentsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
                return oldItem == newItem
            }

        }
    }

}
