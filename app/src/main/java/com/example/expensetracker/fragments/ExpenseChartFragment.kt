package com.example.expensetracker.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.ApiClient
import com.example.expensetracker.LoginActivity
import com.example.expensetracker.TokenManager
import com.example.expensetracker.databinding.FragmentExpenseChartBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch

class ExpenseChartFragment : Fragment() {

    private var _binding: FragmentExpenseChartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()

        loadChartData()
    }
    
    private fun setupPieChart() {
        binding.expensePieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Expenses by categories"
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }
    
    fun loadChartData() {
        lifecycleScope.launch {
            // Redirect to login if user not authenticated
            val token = TokenManager.getToken(requireContext())
            if (token == null) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
                return@launch
            }
            try {
                val authHeader = "Bearer $token"
                
                val response = ApiClient.apiService.getTransactions(authHeader)
                
                if (response.isSuccessful) {
                    response.body()?.let { transactions ->
                        val expenses = transactions.filter { it.type == "expense" }

                        val categoryExpenses = expenses.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() } }

                        if (categoryExpenses.isEmpty()) {
                            binding.noDataText.visibility = View.VISIBLE
                            binding.expensePieChart.visibility = View.GONE
                            return@let
                        }
                        
                        binding.noDataText.visibility = View.GONE
                        binding.expensePieChart.visibility = View.VISIBLE

                        val entries = ArrayList<PieEntry>()
                        for ((category, amount) in categoryExpenses) {
                            entries.add(PieEntry(amount.toFloat(), category))
                        }

                        val dataSet = PieDataSet(entries, "Expense categories")
                        dataSet.sliceSpace = 3f
                        dataSet.selectionShift = 5f

                        val colors = ArrayList<Int>()
                        for (color in ColorTemplate.MATERIAL_COLORS) {
                            colors.add(color)
                        }
                        for (color in ColorTemplate.VORDIPLOM_COLORS) {
                            colors.add(color)
                        }
                        dataSet.colors = colors

                        val data = PieData(dataSet)
                        data.setValueFormatter(PercentFormatter(binding.expensePieChart))
                        data.setValueTextSize(11f)
                        data.setValueTextColor(Color.BLACK)

                        binding.expensePieChart.data = data
                        binding.expensePieChart.highlightValues(null)
                        binding.expensePieChart.invalidate()
                    }
                } else {
                    // Error loading data silently
                }
            } catch (e: Exception) {
                // Ignoring exception during chart loading
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadChartData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 