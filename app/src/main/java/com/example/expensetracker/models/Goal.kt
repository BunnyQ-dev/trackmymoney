package com.example.expensetracker.models

import java.util.Date

/**
 * Клас, що представляє фінансову ціль користувача
 */
data class Goal(
    val id: Int,
    val name: String,
    val targetAmount: Float,
    val savedAmount: Float
) {
    val remainingAmount: Float
        get() = targetAmount - savedAmount
} 