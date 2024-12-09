package com.example.easee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.easee.utils.GestureMotionHelper

class SettingActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var voiceSpeedSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        TTSManager.init(this)
        TTSManager.speak("Setting Activity")

        setupToolbar()

        // HELP BUTTON LISTENER
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }

        updateGestureSwitch()

        updateTtsSwitch()

        updateDropDownList()

    }

    private fun updateGestureSwitch() {
        val gestureSwitch: Switch = findViewById(R.id.gestureSwitch)
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load the saved gesture state
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        gestureSwitch.isChecked = isGestureEnabled

        // Set up toggle listener for Gesture switch
        gestureSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Store the updated state in SharedPreferences
            sharedPreferences.edit().putBoolean("GestureMotionEnabled", isChecked).apply()

            // Enable or disable gesture motion
            if (isChecked) {
                GestureMotionHelper.enableGestureMotion()
                Toast.makeText(this, "Gesture Motion Enabled", Toast.LENGTH_SHORT).show()
            } else {
                GestureMotionHelper.disableGestureMotion()
                Toast.makeText(this, "Gesture Motion Disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTtsSwitch() {
        val ttsSwitch: Switch = findViewById(R.id.voiceCommandSwitch)
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load the saved TTS state
        val isTtsEnabled = sharedPreferences.getBoolean("TtsEnabled", true)
        ttsSwitch.isChecked = isTtsEnabled

        // Set up toggle listener for TTS switch
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Store the updated state in SharedPreferences
            sharedPreferences.edit().putBoolean("TtsEnabled", isChecked).apply()

            // Enable or disable TTS based on the switch state
            if (isChecked) {
                TTSManager.enableTts(this)
                TTSManager.speak("Text-to-Speech Enabled")
                Toast.makeText(this, "Text-to-Speech Enabled", Toast.LENGTH_SHORT).show()
            } else {
                TTSManager.disableTts()
                Toast.makeText(this, "Text-to-Speech Disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDropDownList(){

        val btnSaveChange : Button = findViewById<Button>(R.id.btnSaveChanges)

        // Initialize spinners
        languageSpinner = findViewById(R.id.languageSpinner)
        voiceSpeedSpinner = findViewById(R.id.voiceSpeedSpinner)

        // TO SET UP LANGUAGE SPINNER
        val languages = resources.getStringArray(R.array.language_options)
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter

        // TO SETUP LISTENER FOR ITEM SELECTION IN LANGUAGE SPINNER
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                btnSaveChange.setOnClickListener{
                   // logic
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // TO SET UP VOICE SPEED SPINNER
        val voiceSpeeds = resources.getStringArray(R.array.voice_speed_options)
        val voiceSpeedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceSpeeds)
        voiceSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceSpeedSpinner.adapter = voiceSpeedAdapter

        // TO SETUP LISTENER FOR ITEM SELECTION IN VOICE SPINNER
        voiceSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedVoiceSpeed = voiceSpeeds[position]

                btnSaveChange.setOnClickListener{
                    TTSManager.updateSpeechRateBasedOnSpinner(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TTSManager.speak("Setting Activity")
    }

    private fun setupToolbar(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Setting"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }
    }

    // TO BACK TO PREVIOUS PAGE
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}