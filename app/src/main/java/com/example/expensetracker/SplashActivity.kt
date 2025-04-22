package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY: Long = 2000
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appNameTextView = findViewById<TextView>(R.id.appNameTextView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val loadingTextView = findViewById<TextView>(R.id.loadingTextView)
        val progressTextView = findViewById<TextView>(R.id.progressTextView)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        appNameTextView.startAnimation(fadeIn)
        progressBar.startAnimation(fadeIn)
        loadingTextView.startAnimation(fadeIn)
        progressTextView.startAnimation(fadeIn)

        simulateLoading(progressBar, progressTextView)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            
            finish()
        }, SPLASH_DELAY)
    }
    
    private fun simulateLoading(progressBar: ProgressBar, progressTextView: TextView) {
        thread {
            var progress = 0
            while (progress <= 100) {
                try {
                    Thread.sleep(20)
                    progress += 1

                    runOnUiThread {
                        progressBar.progress = progress
                        progressTextView.text = "$progress%"
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }
} 