package com.example.expensetracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            val categoryEditText = findViewById<EditText>(R.id.category)
            val amountEditText = findViewById<EditText>(R.id.amount)

            val category = categoryEditText.text.toString()
            val amountText = amountEditText.text.toString()

            if (category.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val amount = amountText.toFloat()  // Використовуємо Float замість Double, щоб відповідати ExpenseRequest

                lifecycleScope.launch {
                    val token = TokenManager.getToken(this@MainActivity) ?: run {
                        Toast.makeText(this@MainActivity, "Not logged in", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    try {
                        val authHeader = "Bearer $token"
                        val expenseRequest = ExpenseRequest(category, amount)

                        val response = ApiClient.apiService.addExpense(expenseRequest, authHeader)

                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                            // Очищаємо поля вводу
                            categoryEditText.setText("")
                            amountEditText.setText("")
                        } else {
                            val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                            Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}