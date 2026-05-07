package com.mpesa.tracker.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.mpesa.tracker.MpesaTrackerApp
import com.mpesa.tracker.R
import com.mpesa.tracker.databinding.FragmentDashboardBinding
import com.mpesa.tracker.ui.transactions.TransactionAdapter
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory((requireActivity().application as MpesaTrackerApp).repository)
    }

    private lateinit var recentAdapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecentList()
        setupPieChart()
        setupMonthNavigation()
        observeViewModel()

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.transactionsFragment)
        }
    }

    private fun setupRecentList() {
        recentAdapter = TransactionAdapter { }
        binding.rvRecentTransactions.adapter = recentAdapter
        binding.rvRecentTransactions.isNestedScrollingEnabled = false
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled  = false
            setUsePercentValues(true)
            isDrawHoleEnabled      = true
            holeRadius             = 75f // Larger hole for cleaner look
            transparentCircleRadius = 78f
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false) // This removes the category names from the chart slices
            
            legend.apply {
                isEnabled   = true
                textColor   = Color.parseColor("#8890B0")
                textSize    = 11f
                xEntrySpace = 12f
                yEntrySpace = 4f
                form        = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
            }
            setNoDataText("No expenses this month")
            setNoDataTextColor(Color.parseColor("#8890B0"))
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.nextMonth() }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                return@observe
            }
            binding.progressBar.visibility = View.GONE

            // Summary
            binding.tvTotalIncome.text   = "Ksh %,.0f".format(state.totalIncome)
            binding.tvTotalExpenses.text = "Ksh %,.0f".format(state.totalExpenses)

            val net = state.balance
            binding.tvNetBalance.text = (if (net >= 0) "+" else "") + "Ksh %,.0f".format(net)
            binding.tvNetBalance.setTextColor(
                if (net >= 0) ContextCompat.getColor(requireContext(), R.color.income_green)
                else ContextCompat.getColor(requireContext(), R.color.expense_red)
            )

            updatePieChart(state.categoryBreakdown.map { it.category to it.total.toFloat() })
            recentAdapter.submitList(state.recentTransactions)
        }

        viewModel.selectedMonth.observe(viewLifecycleOwner) { updateMonthLabel() }
        viewModel.selectedYear.observe(viewLifecycleOwner)  { updateMonthLabel() }
    }

    private fun updateMonthLabel() {
        val month = viewModel.selectedMonth.value ?: return
        val year  = viewModel.selectedYear.value  ?: return
        val cal   = Calendar.getInstance().apply { set(year, month, 1) }
        binding.tvMonthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    private fun updatePieChart(data: List<Pair<String, Float>>) {
        if (data.isEmpty()) { binding.pieChart.clear(); return }

        val chartColors = listOf(
            Color.parseColor("#9D4EDD"), Color.parseColor("#00E5A0"),
            Color.parseColor("#FF6B9D"), Color.parseColor("#00C8FF"),
            Color.parseColor("#FFB800"), Color.parseColor("#FF6348"),
            Color.parseColor("#7BED9F"), Color.parseColor("#FF4757")
        )

        val entries = data.take(10).map { (label, value) -> PieEntry(value, label) }
        val dataSet = PieDataSet(entries, "").apply {
            colors         = chartColors
            setDrawValues(false) // Hide values on the chart slices
            sliceSpace     = 3f
        }

        binding.pieChart.apply {
            this.data = PieData(dataSet)
            animateY(900, Easing.EaseInOutCubic)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
