package com.example.expensetracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.ApiClient
import com.example.expensetracker.CurrencyManager
import com.example.expensetracker.ExpenseRequest
import com.example.expensetracker.R
import com.example.expensetracker.TokenManager
import com.example.expensetracker.databinding.FragmentIncomeBinding
import kotlinx.coroutines.launch

class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!
    private var incomeCategories = listOf<String>()

    // Interface for communicating with the activity
    interface IncomeFragmentListener {
        fun onIncomeAdded()
    }

    private var listener: IncomeFragmentListener? = null

    fun setListener(listener: IncomeFragmentListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set currency symbol
        updateCurrencySymbol()
        
        // Setup income button
        binding.addIncomeButton.setOnClickListener {
            addIncome()
        }

        // Load categories
        loadCategories()
    }
    
    // New method to update currency symbol
    fun updateCurrencySymbol() {
        if (!isAdded) return
        val currencySymbol = CurrencyManager.getCurrencySymbol(requireContext())
        binding.incomeAmountLayout.prefixText = currencySymbol
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = context ?: return@launch
            try {
                val token = TokenManager.getToken(ctx) ?: return@launch
                val authHeader = "Bearer $token"

                val response = ApiClient.apiService.getCategories(authHeader)

                if (response.isSuccessful) {
                    response.body()?.let { categoriesResponse ->
                        // Filter only income categories
                        val list = categoriesResponse.filter { it.is_income }.map { it.name }
                        incomeCategories = list

                        // Setup dropdown for income categories
                        val adapter = ArrayAdapter(
                            ctx,
                            android.R.layout.simple_dropdown_item_1line,
                            list
                        )
                        binding.incomeCategory.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                // Ignoring error loading categories
            }
        }
    }

    private fun addIncome() {
        val categoryText = binding.incomeCategory.text.toString()
        val amountText = binding.incomeAmount.text.toString()

        if (categoryText.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(context, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val amount = amountText.toFloat()
            if (amount <= 0) {
                Toast.makeText(context, getString(R.string.amount_must_be_positive), Toast.LENGTH_SHORT).show()
                return
            }

            // Find category ID
            viewLifecycleOwner.lifecycleScope.launch {
                val token = TokenManager.getToken(requireContext()) ?: return@launch
                val authHeader = "Bearer $token"

                try {
                    val categoriesResponse = ApiClient.apiService.getCategories(authHeader)

                    if (categoriesResponse.isSuccessful) {
                        val categories = categoriesResponse.body() ?: return@launch
                        val category = categories.find { it.name == categoryText && it.is_income }

                        if (category != null) {
                            // Add income via transactions endpoint
                            val response = ApiClient.apiService.addTransaction(
                                ExpenseRequest(category.id, amount),
                                authHeader
                            )

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.income_added_successfully),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Clear input fields
                                binding.incomeCategory.text = null
                                binding.incomeAmount.text = null

                                // Notify activity about the added income
                                listener?.onIncomeAdded()
                            } else {
                                val errorMessage = response.errorBody()?.string() ?: getString(R.string.unknown_error)
                                Toast.makeText(context, getString(R.string.error, errorMessage), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, getString(R.string.category_not_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(context, getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 