package com.example.expensetracker.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.ApiClient
import com.example.expensetracker.LoginActivity
import com.example.expensetracker.TokenManager
import com.example.expensetracker.adapters.TransactionAdapter
import com.example.expensetracker.databinding.FragmentTransactionsBinding
import com.example.expensetracker.models.TransactionItem
import kotlinx.coroutines.launch
import java.util.Date

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private var transactionList = listOf<TransactionItem>()
    private var filteredList = listOf<TransactionItem>()
    private var currentSortOrder = SortOrder.NEWEST_FIRST
    private var currentFilter = Filter.ALL
    
    enum class SortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST,
        AMOUNT_HIGH_TO_LOW,
        AMOUNT_LOW_TO_HIGH
    }
    
    enum class Filter {
        ALL,
        EXPENSES_ONLY,
        INCOME_ONLY
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Setting up SwipeRefreshLayout for data refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadTransactions()
        }
        
        // Sort button
        binding.sortButton.setOnClickListener {
            toggleSortOrder()
        }
        
        // Filter button
        binding.filterButton.setOnClickListener {
            toggleFilters()
        }
        
        // Apply filter button
        binding.applyFilterButton.setOnClickListener {
            applyFilters()
        }
        
        // Sort button initialization
        updateSortButtonText()
        
        loadTransactions()
    }
    
    private fun toggleSortOrder() {
        currentSortOrder = when(currentSortOrder) {
            SortOrder.NEWEST_FIRST -> SortOrder.OLDEST_FIRST
            SortOrder.OLDEST_FIRST -> SortOrder.AMOUNT_HIGH_TO_LOW
            SortOrder.AMOUNT_HIGH_TO_LOW -> SortOrder.AMOUNT_LOW_TO_HIGH
            SortOrder.AMOUNT_LOW_TO_HIGH -> SortOrder.NEWEST_FIRST
        }
        
        // Update sort button text
        updateSortButtonText()
        
        // Apply sorting
        applySortAndFilter()
    }
    
    private fun updateSortButtonText() {
        binding.sortButton.text = when(currentSortOrder) {
            SortOrder.NEWEST_FIRST -> "Newest"
            SortOrder.OLDEST_FIRST -> "Oldest"
            SortOrder.AMOUNT_HIGH_TO_LOW -> "By amount ↓"
            SortOrder.AMOUNT_LOW_TO_HIGH -> "By amount ↑"
        }
    }
    
    private fun toggleFilters() {
        if (binding.filterLayout.visibility == View.VISIBLE) {
            binding.filterLayout.visibility = View.GONE
        } else {
            binding.filterLayout.visibility = View.VISIBLE
        }
    }
    
    private fun applyFilters() {
        currentFilter = when {
            binding.expenseRadio.isChecked -> Filter.EXPENSES_ONLY
            binding.incomeRadio.isChecked -> Filter.INCOME_ONLY
            else -> Filter.ALL
        }
        
        // Apply filtering and sorting
        applySortAndFilter()
        
        // Hide filter panel
        binding.filterLayout.visibility = View.GONE
    }
    
    private fun applyFilter() {
        filteredList = when(currentFilter) {
            Filter.ALL -> transactionList
            Filter.EXPENSES_ONLY -> transactionList.filter { it.isExpense }
            Filter.INCOME_ONLY -> transactionList.filter { !it.isExpense }
        }
    }
    
    private fun applySortAndFilter() {
        // First filter
        applyFilter()
        
        // Then sort the filtered list
        val sortedList = when(currentSortOrder) {
            SortOrder.NEWEST_FIRST -> filteredList.sortedByDescending { it.date }
            SortOrder.OLDEST_FIRST -> filteredList.sortedBy { it.date }
            SortOrder.AMOUNT_HIGH_TO_LOW -> filteredList.sortedByDescending { it.amount }
            SortOrder.AMOUNT_LOW_TO_HIGH -> filteredList.sortedBy { it.amount }
        }
        
        // Update RecyclerView
        binding.transactionsRecyclerView.adapter = TransactionAdapter(sortedList)
        
        // Show message if there are no transactions
        binding.noTransactionsText.visibility = if (sortedList.isEmpty()) View.VISIBLE else View.GONE
    }
    
    fun loadTransactions() {
        binding.progressBar.visibility = View.VISIBLE
        binding.noTransactionsText.visibility = View.GONE

        lifecycleScope.launch {
            // Redirect to login if user not authenticated
            val token = TokenManager.getToken(requireContext())
            if (token == null) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
                return@launch
            }
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false

            val authHeader = "Bearer $token"
            val response = ApiClient.apiService.getTransactions(authHeader)

            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false

            if (response.isSuccessful) {
                response.body()?.let { transactions ->
                    transactionList = transactions.map { transaction ->
                        TransactionItem(
                            id = transaction.id,
                            category = transaction.category,
                            amount = transaction.amount,
                            date = transaction.date,
                            isExpense = transaction.type == "expense"
                        )
                    }

                    // Apply filtering and sorting
                    applySortAndFilter()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error loading transactions",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Ignoring exceptions during transactions loading
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 