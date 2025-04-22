package com.example.expensetracker.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.ApiClient
import com.example.expensetracker.CurrencyManager
import com.example.expensetracker.GoalRequest
import com.example.expensetracker.GoalResponse
import com.example.expensetracker.R
import com.example.expensetracker.TokenManager
import com.example.expensetracker.adapters.GoalsAdapter
import com.example.expensetracker.databinding.DialogAddGoalBinding
import com.example.expensetracker.databinding.DialogDepositAmountBinding
import com.example.expensetracker.databinding.DialogWithdrawAmountBinding
import com.example.expensetracker.databinding.FragmentGoalsBinding
import com.example.expensetracker.models.Goal
import kotlinx.coroutines.launch

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    
    private var balance: Double = 0.0
    
    // Interface for communicating with the activity
    interface GoalsFragmentListener {
        fun onBalanceChanged()
    }
    
    private var listener: GoalsFragmentListener? = null

    private lateinit var goalsAdapter: GoalsAdapter
    private val goalsList = mutableListOf<Goal>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is GoalsFragmentListener) {
            listener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupAddGoalButton()
        
        loadBalance()
        loadGoals()
    }

    private fun setupRecyclerView() {
        goalsAdapter = GoalsAdapter(
            goalsList,
            onDepositClick = { goal -> showDepositDialog(goal) },
            onWithdrawClick = { goal -> showWithdrawDialog(goal) },
            onDeleteClick = { goal -> showDeleteDialog(goal) }
        )

        binding.goalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalsAdapter
        }
    }
    
    private fun setupAddGoalButton() {
        binding.addGoalButton.setOnClickListener {
            showAddGoalDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadBalance()
            loadGoals()
        }
    }

    private fun loadBalance() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))
                
                val authHeader = "Bearer $token"
                val response = ApiClient.apiService.getBalance(authHeader)
                
                if (response.isSuccessful) {
                    balance = response.body()?.balance ?: 0.0
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_loading_balance),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun updateCurrencyDisplay() {
        if (!isAdded) return
        goalsAdapter.notifyDataSetChanged()
    }

    private fun loadGoals() {
        binding.progressBar.visibility = View.VISIBLE
        binding.goalsRecyclerView.visibility = View.INVISIBLE
        binding.addGoalButton.isEnabled = false
        binding.swipeRefreshLayout.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(requireContext())
                if (token != null) {
                    val response = ApiClient.apiService.getGoals("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        val goals = response.body()!!
                        goalsList.clear()
                        goalsList.addAll(goals.map { goalResponse -> 
                            Goal(
                                id = goalResponse.id,
                                name = goalResponse.name,
                                targetAmount = goalResponse.target_amount,
                                savedAmount = goalResponse.saved_amount
                            )
                        })
                        goalsAdapter.notifyDataSetChanged()
                        
                        binding.emptyStateGroup.visibility = if (goalsList.isEmpty()) View.VISIBLE else View.GONE
                        binding.goalsRecyclerView.visibility = if (goalsList.isEmpty()) View.GONE else View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), "Failed to load goals: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Authentication error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.addGoalButton.isEnabled = true
                binding.swipeRefreshLayout.isEnabled = true
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun showAddGoalDialog() {
        val dialogBinding = DialogAddGoalBinding.inflate(LayoutInflater.from(requireContext()))
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val currencySymbol = CurrencyManager.getCurrencySymbol(requireContext())
        dialogBinding.goalAmountLayout.hint = getString(R.string.goal_amount) + " (" + currencySymbol + ")"
        
        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.addGoalButton.setOnClickListener {
            val name = dialogBinding.goalNameEditText.text.toString().trim()
            val amountText = dialogBinding.goalAmountEditText.text.toString().trim()
            
            if (name.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            val amount = amountText.toFloatOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.amount_must_be_positive),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            createGoal(name, amount)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun createGoal(name: String, amount: Float) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.goalsRecyclerView.visibility = View.INVISIBLE
                binding.addGoalButton.isEnabled = false
                binding.swipeRefreshLayout.isEnabled = false
                
                val token = TokenManager.getToken(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))
                
                val authHeader = "Bearer $token"
                val goalRequest = GoalRequest(name, amount)

                println("Creating goal request: $goalRequest with token: $token")
                
                val response = ApiClient.apiService.createGoal(goalRequest, authHeader)
                
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.goal_created),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadGoals()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error, "Failed to create goal: $errorBody"),
                        Toast.LENGTH_SHORT
                    ).show()
                    println("Error creating goal: $errorBody")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Exception creating goal: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.addGoalButton.isEnabled = true
                binding.swipeRefreshLayout.isEnabled = true
            }
        }
    }

    private fun showDepositDialog(goal: Goal) {
        val dialogBinding = DialogDepositAmountBinding.inflate(LayoutInflater.from(requireContext()))

        val currencySymbol = CurrencyManager.getCurrencySymbol(requireContext())
        dialogBinding.amountEditText.hint = getString(R.string.deposit_amount) + " (" + currencySymbol + ")"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.deposit_to_goal))
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(getString(R.string.deposit)) { _, _ ->
                val amountText = dialogBinding.amountEditText.text.toString().trim()
                if (amountText.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.enter_valid_amount),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                
                val amount = amountText.toFloatOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.amount_must_be_positive),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val remainingAmount = goal.targetAmount - goal.savedAmount
                if (amount > remainingAmount) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.exceeds_goal_amount),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                
                addFundsToGoal(goal.id, amount)
            }
            .create()
        
        dialog.show()
    }

    private fun addFundsToGoal(goalId: Int, amount: Float) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.goalsRecyclerView.visibility = View.INVISIBLE
                binding.addGoalButton.isEnabled = false
                binding.swipeRefreshLayout.isEnabled = false
                
                val token = TokenManager.getToken(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))

                if (balance < amount) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.insufficient_balance),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    binding.goalsRecyclerView.visibility = View.VISIBLE
                    binding.addGoalButton.isEnabled = true
                    binding.swipeRefreshLayout.isEnabled = true
                    return@launch
                }
                
                val authHeader = "Bearer $token"
                val response = ApiClient.apiService.addFundsToGoal(goalId, amount, authHeader)
                
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.deposit_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadBalance()
                    loadGoals()

                    listener?.onBalanceChanged()
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.insufficient_balance),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error, "Failed to deposit"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.goalsRecyclerView.visibility = View.VISIBLE
                binding.addGoalButton.isEnabled = true
                binding.swipeRefreshLayout.isEnabled = true
            }
        }
    }

    private fun showWithdrawDialog(goal: Goal) {
        val dialogBinding = DialogWithdrawAmountBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.amountEditText.setText(goal.savedAmount.toString())

        val currencySymbol = CurrencyManager.getCurrencySymbol(requireContext())
        dialogBinding.amountEditText.hint = getString(R.string.withdraw_amount) + " (" + currencySymbol + ")"
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.withdraw_from_goal))
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(getString(R.string.withdraw)) { _, _ ->
                returnFundsFromGoal(goal.id)
            }
            .create()
        
        dialog.show()
    }

    private fun returnFundsFromGoal(goalId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.goalsRecyclerView.visibility = View.INVISIBLE
                binding.addGoalButton.isEnabled = false
                binding.swipeRefreshLayout.isEnabled = false
                
                val token = TokenManager.getToken(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))
                
                val username = TokenManager.getUsername(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))
                
                val authHeader = "Bearer $token"
                val response = ApiClient.apiService.returnFundsFromGoal(goalId, username, authHeader)
                
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.withdraw_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadBalance()
                    loadGoals()

                    listener?.onBalanceChanged()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error, "Failed to withdraw"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.goalsRecyclerView.visibility = View.VISIBLE
                binding.addGoalButton.isEnabled = true
                binding.swipeRefreshLayout.isEnabled = true
            }
        }
    }
    
    private fun showDeleteDialog(goal: Goal) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_goal))
            .setMessage(getString(R.string.delete_goal_confirmation))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteGoal(goal.id)
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    private fun deleteGoal(goalId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.goalsRecyclerView.visibility = View.INVISIBLE
                binding.addGoalButton.isEnabled = false
                binding.swipeRefreshLayout.isEnabled = false
                
                val token = TokenManager.getToken(requireContext())
                    ?: throw IllegalStateException(getString(R.string.not_authorized))
                
                val authHeader = "Bearer $token"
                val response = ApiClient.apiService.deleteGoal(goalId, authHeader)
                
                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.goal_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadGoals()

                    loadBalance()
                    listener?.onBalanceChanged()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error, "Failed to delete goal"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.goalsRecyclerView.visibility = View.VISIBLE
                binding.addGoalButton.isEnabled = true
                binding.swipeRefreshLayout.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}