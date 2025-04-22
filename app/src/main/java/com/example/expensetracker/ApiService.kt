package com.example.expensetracker

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("transactions/")
    suspend fun addTransaction(
        @Body request: ExpenseRequest,
        @Header("Authorization") authHeader: String
    ): Response<TransactionResponse>

    @GET("categories/")
    suspend fun getCategories(
        @Header("Authorization") authHeader: String
    ): Response<List<CategoryResponse>>

    @GET("balance/")
    suspend fun getBalance(
        @Header("Authorization") authHeader: String
    ): Response<BalanceResponse>

    @GET("transactions/")
    suspend fun getTransactions(
        @Header("Authorization") authHeader: String
    ): Response<List<TransactionResponse>>
    
    @POST("change-password/")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
        @Header("Authorization") authHeader: String
    ): Response<BaseResponse>
    
    // API методи для роботи з цілями
    @GET("goals/")
    suspend fun getGoals(
        @Header("Authorization") authHeader: String
    ): Response<List<GoalResponse>>
    
    @POST("goals/")
    suspend fun createGoal(
        @Body request: GoalRequest,
        @Header("Authorization") authHeader: String
    ): Response<GoalResponse>
    
    @POST("goals/{goal_id}/add-funds/")
    suspend fun addFundsToGoal(
        @Path("goal_id") goalId: Int,
        @Query("amount") amount: Float,
        @Header("Authorization") authHeader: String
    ): Response<BaseResponse>
    
    @POST("goals/{goal_id}/return-funds/")
    suspend fun returnFundsFromGoal(
        @Path("goal_id") goalId: Int,
        @Query("username") username: String,
        @Header("Authorization") authHeader: String
    ): Response<BaseResponse>
    
    @DELETE("goals/{goal_id}/")
    suspend fun deleteGoal(
        @Path("goal_id") goalId: Int,
        @Header("Authorization") authHeader: String
    ): Response<BaseResponse>
}

