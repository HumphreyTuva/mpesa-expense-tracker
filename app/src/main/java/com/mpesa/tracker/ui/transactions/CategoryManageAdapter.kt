package com.mpesa.tracker.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mpesa.tracker.data.model.Category
import com.mpesa.tracker.databinding.ItemCategoryManageBinding

class CategoryManageAdapter(
    private val onDelete: (Category) -> Unit
) : ListAdapter<Category, CategoryManageAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCategoryManageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.tvCategoryName.text = category.name
            binding.btnDelete.visibility = if (category.isSystem) View.GONE else View.VISIBLE
            binding.btnDelete.setOnClickListener { onDelete(category) }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
    }
}
