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
import com.example.expensetracker.databinding.FragmentExpenseBinding
import kotlinx.coroutines.launch

class ExpenseFragment : Fragment() {

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!
    private var expenseCategories = listOf<String>()
    
    // Interface for communicating with the activity
    interface ExpenseFragmentListener {
        fun onExpenseAdded()
    }
    
    private var listener: ExpenseFragmentListener? = null
    
    fun setListener(listener: ExpenseFragmentListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set currency symbol
        updateCurrencySymbol()
        
        // Setup expense button
        binding.addExpenseButton.setOnClickListener {
            addExpense()
        }
        
        // Load categories
        loadCategories()
    }
    
    // New method to update currency symbol
    fun updateCurrencySymbol() {
        val currencySymbol = CurrencyManager.getCurrencySymbol(requireContext())
        binding.amountLayout.prefixText = currencySymbol
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(requireContext()) ?: return@launch
                val authHeader = "Bearer $token"
                
                val response = ApiClient.apiService.getCategories(authHeader)
                
                if (response.isSuccessful) {
                    response.body()?.let { categoriesResponse ->
                        // Filter only expense categories
                        expenseCategories = categoriesResponse.filter { !it.is_income }.map { it.name }
                        
                        // Setup dropdown for expense categories
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            expenseCategories
                        )
                        binding.category.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context, 
                    getString(R.string.error_loading_categories, e.message), 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun addExpense() {
        val categoryText = binding.category.text.toString()
        val amountText = binding.amount.text.toString()
        
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
            lifecycleScope.launch {
                val token = TokenManager.getToken(requireContext()) ?: return@launch
                val authHeader = "Bearer $token"
                
                try {
                    val categoriesResponse = ApiClient.apiService.getCategories(authHeader)
                    
                    if (categoriesResponse.isSuccessful) {
                        val categories = categoriesResponse.body() ?: return@launch
                        val category = categories.find { it.name == categoryText && !it.is_income }
                        
                        if (category != null) {
                            // Add expense via transactions endpoint
                            val response = ApiClient.apiService.addTransaction(
                                ExpenseRequest(category.id, amount), 
                                authHeader
                            )
                            
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.expense_added_successfully),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Clear input fields
                                binding.category.text = null
                                binding.amount.setText("")
                                
                                // Notify activity about the added expense
                                listener?.onExpenseAdded()
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