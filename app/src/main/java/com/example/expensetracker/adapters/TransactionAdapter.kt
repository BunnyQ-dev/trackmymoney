package com.example.expensetracker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.CurrencyManager
import com.example.expensetracker.R
import com.example.expensetracker.databinding.ItemExpenseBinding
import com.example.expensetracker.models.TransactionItem
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val transactions: List<TransactionItem>) : 
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("uk"))
    
    class TransactionViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context
        
        holder.binding.apply {
            expenseCategory.text = transaction.category
            expenseDate.text = dateFormat.format(transaction.date)
            
            val sign = if (transaction.isExpense) "-" else "+"
            val color = if (transaction.isExpense) 
                root.context.getColor(R.color.expense_color) 
            else 
                root.context.getColor(R.color.income_color)

            val currencySymbol = CurrencyManager.getCurrencySymbol(context)
                
            expenseAmount.text = "$sign$currencySymbol${String.format("%.2f", transaction.amount)}"
            expenseAmount.setTextColor(color)

            val iconTint = if (transaction.isExpense) 
                root.context.getColor(R.color.expense_color) 
            else 
                root.context.getColor(R.color.income_color)
                
            categoryIcon.setColorFilter(iconTint)
        }
    }
    
    override fun getItemCount() = transactions.size
} 