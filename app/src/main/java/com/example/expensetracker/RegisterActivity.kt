package com.example.expensetracker
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameInput = findViewById<EditText>(R.id.username)
        val emailInput = findViewById<EditText>(R.id.email)
        val passwordInput = findViewById<EditText>(R.id.password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val loginText = findViewById<TextView>(R.id.loginText)

        // Navigate to login activity
        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        registerBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate inputs
            if (username.isEmpty()) {
                usernameInput.error = "Username is required"
                return@setOnClickListener
            }
            
            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                return@setOnClickListener
            }

            // Simple email validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            
            // Password strength validation
            if (password.length < 8) {
                passwordInput.error = "Password must be at least 8 characters long"
                return@setOnClickListener
            }

            // Show registration in progress
            registerBtn.isEnabled = false
            registerBtn.text = "Registering..."

            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Attempting registration for user: $username, email: $email")
                    val request = RegisterRequest(
                        username = username,
                        email = email,
                        password = password
                    )
                    val response = ApiClient.apiService.register(request)
                    
                    if (response.isSuccessful) {
                        Log.d(TAG, "Registration successful")
                        Toast.makeText(this@RegisterActivity, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                        // After successful registration, redirect to login
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        // Try to get error message from response
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Registration failed with code: ${response.code()}, message: ${response.message()}, error body: $errorBody")
                        
                        // Handle different HTTP error codes
                        when (response.code()) {
                            400 -> Toast.makeText(this@RegisterActivity, "Username or email already exists", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@RegisterActivity, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                        resetRegisterButton()
                    }
                } catch (e: HttpException) {
                    Log.e(TAG, "HTTP Exception during registration", e)
                    Toast.makeText(this@RegisterActivity, "Server error: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetRegisterButton()
                } catch (e: IOException) {
                    Log.e(TAG, "IO Exception during registration", e)
                    Toast.makeText(this@RegisterActivity, "Network Error: Check your internet connection", Toast.LENGTH_SHORT).show()
                    resetRegisterButton()
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during registration", e)
                    Toast.makeText(this@RegisterActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    resetRegisterButton()
                }
            }
        }
    }
    
    private fun resetRegisterButton() {
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        registerBtn.isEnabled = true
        registerBtn.text = "Register"
    }
}
