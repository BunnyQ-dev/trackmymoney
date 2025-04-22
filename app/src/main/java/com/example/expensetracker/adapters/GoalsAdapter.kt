package com.example.expensetracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.CurrencyManager
import com.example.expensetracker.databinding.ItemGoalBinding
import com.example.expensetracker.models.Goal

class GoalsAdapter(
    private val goals: List<Goal>,
    private val onDepositClick: (Goal) -> Unit,
    private val onWithdrawClick: (Goal) -> Unit,
    private val onDeleteClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    inner class GoalViewHolder(private val binding: ItemGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            with(binding) {
                val context = root.context
                val currencySymbol = CurrencyManager.getCurrencySymbol(context)

                goalNameTextView.text = goal.name
                goalDescriptionTextView.text = ""

                val progress = if (goal.targetAmount > 0) {
                    (goal.savedAmount / goal.targetAmount * 100).toInt()
                } else {
                    0
                }
                
                goalProgressIndicator.progress = progress
                goalProgressTextView.text = "$progress%"

                val savedAmount = String.format("%.2f", goal.savedAmount)
                val targetAmount = String.format("%.2f", goal.targetAmount)
                goalAmountTextView.text = "$currencySymbol$savedAmount / $currencySymbol$targetAmount"

                depositButton.setOnClickListener { onDepositClick(goal) }
                withdrawButton.setOnClickListener { onWithdrawClick(goal) }

                root.setOnLongClickListener {
                    onDeleteClick(goal)
                    true
                }
            }
        }
    }
} 