package com.example.expensetracker

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
    val category: String,
    val amount: Float
)

data class ExpenseResponse(val message: String)


data class IncomeRequest(
    val category: String,
    val amount: Double
)

data class BalanceResponse(
    val balance: Double
)

data class IncomeResponse(
    val message: String
)
