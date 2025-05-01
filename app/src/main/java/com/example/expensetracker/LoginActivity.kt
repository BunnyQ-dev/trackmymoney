package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<TextInputEditText>(R.id.username)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerText = findViewById<TextView>(R.id.registerText)

        // Check if user is already logged in
        if (TokenManager.getToken(this) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate inputs
            if (username.isEmpty()) {
                usernameInput.error = "Username is required"
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                return@setOnClickListener
            }

            // Show login in progress
            loginButton.isEnabled = false
            loginButton.text = "Logging in..."

            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Attempting login for user: $username")
                    val loginRequest = LoginRequest(username, password)
                    val response = ApiClient.apiService.login(loginRequest)
                    
                    if (response.isSuccessful) {
                        response.body()?.let { loginResponse ->
                            Log.d(TAG, "Login successful, token received: ${loginResponse.access_token.take(10)}...")
                            
                            // Save token
                            TokenManager.saveToken(this@LoginActivity, loginResponse.access_token)
                            // Save username
                            TokenManager.saveUsername(this@LoginActivity, username)

                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            
                            // Navigate to main screen
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } ?: run {
                            Log.e(TAG, "Empty response from server")
                            Toast.makeText(this@LoginActivity, "Empty response from server", Toast.LENGTH_SHORT).show()
                            resetLoginButton()
                        }
                    } else {
                        // Try to get error message from response
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Login failed with code: ${response.code()}, message: ${response.message()}, error body: $errorBody")
                        
                        // Handle different HTTP error codes
                        when (response.code()) {
                            400 -> Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                            401 -> Toast.makeText(this@LoginActivity, "Unauthorized access", Toast.LENGTH_SHORT).show()
                            404 -> Toast.makeText(this@LoginActivity, "Service not found", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                        resetLoginButton()
                    }
                } catch (e: HttpException) {
                    Log.e(TAG, "HTTP Exception during login", e)
                    Toast.makeText(this@LoginActivity, "Server error: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetLoginButton()
                } catch (e: IOException) {
                    Log.e(TAG, "IO Exception during login", e)
                    Toast.makeText(this@LoginActivity, "Network Error: Check your internet connection", Toast.LENGTH_SHORT).show()
                    resetLoginButton()
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected exception during login", e)
                    Toast.makeText(this@LoginActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    resetLoginButton()
                }
            }
        }
    }
    
    private fun resetLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.isEnabled = true
        loginButton.text = "Login"
    }
}