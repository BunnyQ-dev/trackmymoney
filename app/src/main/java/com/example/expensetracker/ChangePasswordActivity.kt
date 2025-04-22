package com.example.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Change password"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupButtons() {
        binding.changePasswordButton.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = binding.currentPassword.text.toString()
        val newPassword = binding.newPassword.text.toString()
        val confirmPassword = binding.confirmPassword.text.toString()

        // Field validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Execute password change request
        lifecycleScope.launch {
            try {
                val token = TokenManager.getToken(this@ChangePasswordActivity)
                if (token == null) {
                    Toast.makeText(this@ChangePasswordActivity, "You are not authorized", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val authHeader = "Bearer $token"
                val request = ChangePasswordRequest(
                    old_password = currentPassword,
                    new_password = newPassword
                )

                val response = ApiClient.apiService.changePassword(request, authHeader)

                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@ChangePasswordActivity, "Error: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChangePasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 