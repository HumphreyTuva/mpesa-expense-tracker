package com.mpesa.tracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mpesa.tracker.R
import com.mpesa.tracker.data.model.Transaction
import com.mpesa.tracker.data.model.TransactionType
import com.mpesa.tracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }

        private val TYPE_EMOJI = mapOf(
            TransactionType.RECEIVE   to "💰",
            TransactionType.SEND      to "📤",
            TransactionType.PAYBILL   to "📄",
            TransactionType.BUY_GOODS to "🛍️",
            TransactionType.WITHDRAW  to "🏧",
            TransactionType.AIRTIME   to "📱",
            TransactionType.UNKNOWN   to "❓"
        )

        private val TYPE_BG = mapOf(
            TransactionType.RECEIVE   to "#0D2B22",
            TransactionType.SEND      to "#1A1030",
            TransactionType.PAYBILL   to "#0D1A2B",
            TransactionType.BUY_GOODS to "#2B1020",
            TransactionType.WITHDRAW  to "#1E1530",
            TransactionType.AIRTIME   to "#0D2228",
            TransactionType.UNKNOWN   to "#1E2235"
        )
    }

    private val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(tx: Transaction) {
            val ctx = b.root.context

            b.tvName.text     = tx.displayName().uppercase()
            b.tvDate.text     = dateFmt.format(Date(tx.timestamp))
            b.tvCategory.text = tx.category
            b.tvTypeEmoji.text = TYPE_EMOJI[tx.type] ?: "💸"

            // Amount with sign and colour
            val amountStr = if (tx.isExpense) "-Ksh %,.0f".format(tx.amount)
                            else "+Ksh %,.0f".format(tx.amount)
            b.tvAmount.text = amountStr
            b.tvAmount.setTextColor(
                if (tx.isExpense) ContextCompat.getColor(ctx, R.color.expense_red)
                else ContextCompat.getColor(ctx, R.color.income_green)
            )

            // Icon background colour by type
            val bgHex = TYPE_BG[tx.type] ?: "#1E2235"
            b.cardTypeIcon.setCardBackgroundColor(android.graphics.Color.parseColor(bgHex))

            // Visual feedback for excluded transactions
            if (tx.isExcluded) {
                b.root.alpha = 0.5f
                b.tvName.text = "${tx.displayName().uppercase()} (EXCLUDED)"
            } else {
                b.root.alpha = 1.0f
                b.tvName.text = tx.displayName().uppercase()
            }

            b.root.setOnClickListener { onClick(tx) }
        }
    }
}
