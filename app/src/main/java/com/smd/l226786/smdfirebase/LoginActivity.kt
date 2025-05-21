package com.smd.l226786.smdfirebase

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.smd.l226786.smdfirebase.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)

        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // setup click listeners
        // Login
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                showProgress(true)

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        showProgress(false)

                        if (task.isSuccessful) {
                            auth.currentUser?.let {
                                // Save user ID
                                sharedPreferences.edit().apply {
                                    putBoolean("is_logged_in", true)
                                    putString("user_id", it.uid)
                                    apply()
                                }
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            // Login failed
                            handleLoginError(task.exception)
                        }
                    }
            }
        }

        // Sign up
        binding.textViewSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Forgot password
        binding.textViewForgotPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email is required"
            return false
        }
        
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Password is required"
            return false
        }
        
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null
        return true
    }

    private fun handleLoginError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                binding.textInputLayoutEmail.error = "Email not registered. Please sign up."
                binding.textViewSignUp.alpha = 1.0f
            }
            is FirebaseAuthInvalidCredentialsException -> {
                binding.textInputLayoutPassword.error = "Incorrect password"
                binding.textViewForgotPassword.alpha = 1.0f
            }
            else -> {
                Toast.makeText(
                    this,
                    "Authentication failed: ${exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        showProgress(true)
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgress(false)
                
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
