package com.example.expensetracker

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("add_expense")
    suspend fun addExpense(
        @Body request: ExpenseRequest,
        @Header("Authorization") authHeader: String
    ): Response<ExpenseResponse>

    @GET("get_income_categories")
    suspend fun getIncomeCategories(
        @Header("Authorization") authHeader: String
    ): Response<List<String>>

    @GET("get_expense_categories")
    suspend fun getExpenseCategories(
        @Header("Authorization") authHeader: String
    ): Response<List<String>>

    @POST("add_income")
    suspend fun addIncome(
        @Body request: IncomeRequest,
        @Header("Authorization") authHeader: String
    ): Response<IncomeResponse>

    @GET("balance")
    suspend fun getBalance(
        @Header("Authorization") authHeader: String
    ): Response<BalanceResponse>


}

