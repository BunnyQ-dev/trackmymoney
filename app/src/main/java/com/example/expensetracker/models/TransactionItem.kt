package com.example.expensetracker.models

import java.util.Date

data class TransactionItem(
    val id: Int,
    val category: String,
    val amount: Float,
    val date: Date,
    val isExpense: Boolean
) 