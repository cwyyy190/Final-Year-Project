package com.example.easee

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.easee.utils.FirebaseHelper

class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isCfmPasswordVisible = false
    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        TTSManager.init(this)
        TTSManager.speak("Sign Up Activity", this)

        val signUpButton: Button = findViewById(R.id.signUpButton)
        val loginLink: TextView = findViewById(R.id.loginLink)

        // Handle sign-up button click
        signUpButton.setOnClickListener {
            processInput()

        }
            // Navigate to login page
            loginLink.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }


        // Toggle password visibility
        togglePasswordVisibility()
    }

    private fun processInput(){
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val cfmPasswordEditText: EditText = findViewById(R.id.confirmPasswordEditText)

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val cfmPassword = cfmPasswordEditText.text.toString().trim()

        // Validate input
        val validationMessage = FirebaseHelper.isSignUpInputValid(email, password, cfmPassword)
        if (validationMessage != null) {
            Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
            return
        }

        // Create user with Firebase Authentication
        FirebaseHelper.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = FirebaseHelper.auth.currentUser?.uid
                    if (userId != null) {
                        val userData = hashMapOf(
                            "email" to email,
                            "password" to password
                        )
                        FirebaseHelper.firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sign-up successful and data saved to Firestore!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomePageActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "User ID is null, user may not be authenticated.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        TTSManager.speak("Sign Up Activity", this)
    }

    override fun onDestroy() {
        // Shut down TextToSpeech to release resources
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun togglePasswordVisibility() {
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val passwordToggle: ImageButton = findViewById(R.id.passwordToggle)

        val cfmPasswordEditText: EditText = findViewById(R.id.confirmPasswordEditText)
        val cfmPasswordToggle: ImageButton = findViewById(R.id.confirmPasswordToggle)

        // Toggle visibility on button click for password
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

        // Toggle visibility on button click for confirm password
        cfmPasswordToggle.setOnClickListener {
            isCfmPasswordVisible = !isCfmPasswordVisible
            if (isCfmPasswordVisible) {
                cfmPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                cfmPasswordToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                cfmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                cfmPasswordToggle.setImageResource(R.drawable.ic_visiblity_off)
            }
            cfmPasswordEditText.setSelection(cfmPasswordEditText.text.length)
        }
    }
}
