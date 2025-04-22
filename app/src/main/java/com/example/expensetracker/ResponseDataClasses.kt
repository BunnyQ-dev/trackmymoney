package com.example.expensetracker

import java.util.Date

data class RegisterResponse(val message: String)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class ExpenseRequest(
    val category_id: Int,
    val amount: Float
)

data class ExpenseResponse(val message: String)

data class BaseResponse(
    val message: String
)

data class BalanceResponse(
    val balance: Double
)

data class CategoryResponse(
    val id: Int,
    val name: String,
    val is_income: Boolean
)

data class TransactionResponse(
    val id: Int,
    val type: String,
    val amount: Float,
    val category: String,
    val date: Date
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class GoalRequest(
    val name: String,
    val target_amount: Float
)

data class GoalResponse(
    val id: Int,
    val name: String,
    val target_amount: Float,
    val saved_amount: Float
)

data class GoalDepositRequest(
    val amount: Float
)

data class GoalWithdrawRequest(
    val amount: Float? = null
)
