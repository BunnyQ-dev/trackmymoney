package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<TextInputEditText>(R.id.username)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerText = findViewById<TextView>(R.id.registerText)

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(LoginRequest(username, password))
                    if (response.isSuccessful) {
                        response.body()?.let { loginResponse ->
                            // Save token
                            TokenManager.saveToken(this@LoginActivity, loginResponse.access_token)
                            // Save username
                            TokenManager.saveUsername(this@LoginActivity, username)

                            // Navigate to main screen
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } ?: run {
                            Toast.makeText(this@LoginActivity, "Error: Empty response", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}