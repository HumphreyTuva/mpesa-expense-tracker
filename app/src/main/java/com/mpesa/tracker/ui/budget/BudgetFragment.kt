package com.mpesa.tracker.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.mpesa.tracker.MpesaTrackerApp
import com.mpesa.tracker.R
import com.mpesa.tracker.databinding.DialogAddBudgetBinding
import com.mpesa.tracker.databinding.FragmentBudgetBinding
import java.text.SimpleDateFormat
import java.util.*

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory((requireActivity().application as MpesaTrackerApp).repository)
    }

    private lateinit var adapter: BudgetAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Month label
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvBudgetMonth.text = sdf.format(Calendar.getInstance().time)

        // Adapter
        adapter = BudgetAdapter { item -> showDeleteConfirmation(item) }
        binding.rvBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BudgetFragment.adapter
        }

        binding.fabAddBudget.setOnClickListener { showAddBudgetBottomSheet() }

        // Observe budgets with spend data
        viewModel.budgetsWithSpent.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptyBudget.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBudgets.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE

            // Update summary totals
            val totalBudget = list.sumOf { it.budget.limitAmount }
            val totalSpent  = list.sumOf { it.spent }
            val remaining   = totalBudget - totalSpent
            binding.tvTotalBudget.text    = "Ksh %,.0f".format(totalBudget)
            binding.tvTotalSpent.text     = "Ksh %,.0f".format(totalSpent)
            binding.tvTotalRemaining.text = "Ksh %,.0f".format(remaining.coerceAtLeast(0.0))
        }
    }

    private fun showDeleteConfirmation(item: com.mpesa.tracker.data.model.BudgetWithSpent) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete the budget for ${item.budget.category}?")
            .setPositiveButton("Delete") { _, _ ->
                val budgetToDelete = item.budget
                viewModel.deleteBudget(budgetToDelete)
                
                val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
                
                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Budget deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { 
                        viewModel.addBudget(budgetToDelete.category, budgetToDelete.limitAmount)
                    }
                    .setAnchorView(bottomNav)
                    .setActionTextColor(resources.getColor(R.color.purple_primary, null))
                    .setBackgroundTint(resources.getColor(R.color.bg_surface, null))
                    .setTextColor(resources.getColor(R.color.text_primary, null))
                    .apply {
                        view.elevation = 100f
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddBudgetBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_MpesaTracker_BottomSheet)
        val sheetBinding = DialogAddBudgetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Populate spinner
        val catAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewModel.availableCategories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        sheetBinding.spinnerCategory.adapter = catAdapter

        sheetBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        sheetBinding.btnSave.setOnClickListener {
            val category = sheetBinding.spinnerCategory.selectedItem?.toString()
            val amountStr = sheetBinding.etBudgetAmount.text?.toString()

            if (category.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountStr?.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                sheetBinding.etBudgetAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            viewModel.addBudget(category, amount)
            dialog.dismiss()

            val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Budget set for $category", Snackbar.LENGTH_LONG)
                .setAnchorView(bottomNav)
                .setBackgroundTint(resources.getColor(R.color.bg_surface, null))
                .setTextColor(resources.getColor(R.color.text_primary, null))
                .apply {
                    view.elevation = 100f
                }
                .show()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
