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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale
import kotlin.math.log

class ForgetPasswordActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    lateinit var tts: TextToSpeech
    //lateinit var announceText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        TTSManager.init(this)
        TTSManager.speak("Forget Password Activity")
    }

    override fun onResume() {
        super.onResume()
        TTSManager.speak("Forget Password Activity")
    }

    override fun onDestroy() {
        // Shutdown TextToSpeech when activity is destroyed to release resources
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }



}