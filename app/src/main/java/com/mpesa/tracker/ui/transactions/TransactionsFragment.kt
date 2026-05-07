package com.mpesa.tracker.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.mpesa.tracker.MpesaTrackerApp
import com.mpesa.tracker.R
import com.mpesa.tracker.data.model.Transaction
import com.mpesa.tracker.databinding.DialogEditTransactionBinding
import com.mpesa.tracker.databinding.FragmentTransactionsBinding

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels {
        TransactionsViewModelFactory((requireActivity().application as MpesaTrackerApp).repository)
    }

    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupCategoryChips()
        setupSwipeToExclude()
        observeTransactions()
        
        binding.fabAddTransaction.setOnClickListener {
            showAddManualDialog()
        }
        
        binding.btnManageCategories.setOnClickListener {
            showManageCategoriesDialog()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { tx ->
            showEditDialog(tx)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionsFragment.adapter
        }
    }

    private fun setupCategoryChips() {
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->
            binding.chipGroupCategories.removeAllViews()
            
            // Add "All" chip
            addCategoryChip("All", true)
            // Add "Excluded" chip
            addCategoryChip("Excluded", false)
            
            categoryList.forEach { category ->
                addCategoryChip(category.name, false)
            }
        }
    }

    private fun addCategoryChip(name: String, isDefault: Boolean) {
        val chip = Chip(requireContext()).apply {
            text        = name
            isCheckable = true
            isChecked   = isDefault
            chipBackgroundColor = resources.getColorStateList(R.color.chip_bg_selector, null)
            setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
            chipStrokeWidth = 1f
            chipStrokeColor = resources.getColorStateList(R.color.chip_stroke_selector, null)
        }
        chip.setOnClickListener {
            for (i in 0 until binding.chipGroupCategories.childCount)
                (binding.chipGroupCategories.getChildAt(i) as? Chip)?.isChecked = false
            chip.isChecked = true
            viewModel.setFilterCategory(if (name == "All") null else name)
        }
        binding.chipGroupCategories.addView(chip)
    }

    private fun showManageCategoriesDialog() {
        val dialogBinding = com.mpesa.tracker.databinding.DialogManageCategoriesBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .create()

        val categoryAdapter = CategoryManageAdapter { category ->
            if (category.isSystem) {
                Toast.makeText(context, "System categories cannot be deleted", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.deleteCategory(category)
            }
        }

        dialogBinding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        viewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }

        dialogBinding.btnAddCategory.setOnClickListener {
            val name = dialogBinding.etNewCategory.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.addCategory(name)
                dialogBinding.etNewCategory.setText("")
            }
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditDialog(tx: Transaction) {
        val dialogBinding = DialogEditTransactionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.etAmount.setText(tx.amount.toString())
        dialogBinding.etRecipient.setText(tx.recipient ?: tx.phone ?: "")
        dialogBinding.etNotes.setText(tx.note)
        dialogBinding.switchExclude.isChecked = tx.isExcluded

        // Get current categories from ViewModel
        val currentCategories = viewModel.categories.value?.map { it.name } ?: emptyList()
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currentCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCategory.adapter = spinnerAdapter
        
        val categoryIndex = currentCategories.indexOf(tx.category)
        if (categoryIndex >= 0) {
            dialogBinding.spinnerCategory.setSelection(categoryIndex)
        }

        dialogBinding.btnSave.setOnClickListener {
            val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: tx.amount
            val recipient = dialogBinding.etRecipient.text.toString()
            val category = dialogBinding.spinnerCategory.selectedItem?.toString() ?: "Other"
            val notes = dialogBinding.etNotes.text.toString()
            val isExcluded = dialogBinding.switchExclude.isChecked

            val updatedTx = tx.copy(
                amount = amount,
                recipient = recipient,
                category = category,
                note = notes,
                isExcluded = isExcluded
            )

            viewModel.updateTransaction(updatedTx)
            
            if (category != tx.category && recipient.isNotEmpty()) {
                viewModel.saveCategoryMapping(recipient, category)
            }

            dialog.dismiss()
            showSnackbar("Transaction updated", tx)
        }

        dialog.show()
    }

    private fun showAddManualDialog() {
        val dialogBinding = DialogEditTransactionBinding.inflate(layoutInflater)
        dialogBinding.btnSave.text = "Add Transaction"
        
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogBinding.root)
            .create()

        val currentCategories = viewModel.categories.value?.map { it.name } ?: emptyList()
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currentCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCategory.adapter = spinnerAdapter

        dialogBinding.btnSave.setOnClickListener {
            val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val recipient = dialogBinding.etRecipient.text.toString()
            val category = dialogBinding.spinnerCategory.selectedItem?.toString() ?: "Other"
            val notes = dialogBinding.etNotes.text.toString()
            val isExcluded = dialogBinding.switchExclude.isChecked

            if (amount <= 0) {
                dialogBinding.etAmount.error = "Enter valid amount"
                return@setOnClickListener
            }

            val newTx = Transaction(
                transactionId = "MANUAL_${System.currentTimeMillis()}",
                type = com.mpesa.tracker.data.model.TransactionType.PAYBILL, // Manual is usually an expense
                amount = amount,
                recipient = recipient,
                category = category,
                note = notes,
                timestamp = System.currentTimeMillis(),
                rawSms = "Manual entry",
                isManual = true,
                isExcluded = isExcluded
            )

            viewModel.insertManualTransaction(newTx)
            dialog.dismiss()
            showSnackbar("Manual transaction added")
        }

        dialog.show()
    }

    private fun showSnackbar(message: String, oldTx: Transaction? = null) {
        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .apply {
                if (oldTx != null) {
                    setAction("Undo") { viewModel.updateTransaction(oldTx) }
                    setActionTextColor(resources.getColor(R.color.purple_primary, null))
                }
            }
            .setAnchorView(bottomNav)
            .setBackgroundTint(resources.getColor(R.color.bg_surface, null))
            .setTextColor(resources.getColor(R.color.text_primary, null))
            .show()
    }

    private fun setupSwipeToExclude() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val tx = adapter.currentList[position]
                val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
                
                val message = if (tx.isExcluded) "Transaction restored" else "Transaction excluded"
                val actionColor = if (tx.isExcluded) R.color.income_green else R.color.purple_primary
                
                if (tx.isExcluded) {
                    viewModel.updateTransaction(tx.copy(isExcluded = false))
                } else {
                    viewModel.excludeTransaction(tx)
                }

                Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .setAction("Undo") { 
                        viewModel.updateTransaction(tx) 
                    }
                    .setAnchorView(bottomNav)
                    .setActionTextColor(resources.getColor(actionColor, null))
                    .setBackgroundTint(resources.getColor(R.color.bg_surface, null))
                    .setTextColor(resources.getColor(R.color.text_primary, null))
                    .apply {
                        view.elevation = 100f
                    }
                    .show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTransactions)
    }

    private fun observeTransactions() {
        viewModel.filteredTransactions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
