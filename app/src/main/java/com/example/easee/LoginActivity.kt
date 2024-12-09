package com.example.easee

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.easee.utils.FirebaseHelper

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        TTSManager.init(this)
        TTSManager.speak("Login Activity")

        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val signUpLink: TextView = findViewById(R.id.signUpLink)
        val resetLink: TextView = findViewById(R.id.resetLink)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            val validationMessage = FirebaseHelper.isLoginInputValid(email, password)
            if (validationMessage != null) {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseHelper.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = FirebaseHelper.auth.currentUser?.uid
                        if (userId != null) {
                            fetchUserData(userId)
                        } else {
                            Toast.makeText(this, "User ID is null after login.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signUpLink.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        resetLink.setOnClickListener {
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
        }

        togglePasswordVisiblity()
    }

    override fun onDestroy() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun fetchUserData(userId: String) {
        FirebaseHelper.firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val email = document.getString("email") ?: "N/A"
                    val password = document.getString("password") ?: "N/A"
                    val name = document.get("name") ?: "N/A"

                    // Display or use the retrieved user data
                    Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()

                    // Navigate to the home page
                    startActivity(Intent(this, HomePageActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "User data not found in Firestore.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "Error fetching user data", e)
            }
    }

    private fun togglePasswordVisiblity() {
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val passwordToggle: ImageButton = findViewById(R.id.passwordToggle)

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_visiblity_off)
            }

            passwordEditText.setSelection(passwordEditText.text.length)
        }
    }

    override fun onResume() {
        super.onResume()
        TTSManager.speak("Login Activity")
    }
}
