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
    private lateinit var gestureSwitch: Switch
    private lateinit var ttsSwitch: Switch
    private lateinit var btnSave : Button

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
        TTSManager.speak("Setting Activity", this)

        setupToolbar()

        // HELP BUTTON LISTENER
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }

        // Initialize preferences map for temporary changes
        val tempPreferences = mutableMapOf<String, Any>()

        // Initialize UI components
        initializePreferences()
        // Initialize UI components
        gestureSwitch = findViewById(R.id.gestureSwitch)
        ttsSwitch = findViewById(R.id.voiceCommandSwitch)
        languageSpinner = findViewById(R.id.languageSpinner)
        voiceSpeedSpinner = findViewById(R.id.voiceSpeedSpinner)
        btnSave = findViewById(R.id.btnSaveChanges)


        // Set up temporary preference updates
        updateGestureSwitch(tempPreferences)
        updateTtsSwitch(tempPreferences)
        updateDdlLanguage(tempPreferences)
        updateDdlVoiceSpeed(tempPreferences)

        btnSave.setOnClickListener{

            setupSaveChangesButton(tempPreferences)
            startActivity(Intent(this, UserProfileActivity::class.java))
            Toast.makeText(this, "Changes has been saved", Toast.LENGTH_SHORT).show()
        }

    }

    private fun initializePreferences() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        gestureSwitch = findViewById(R.id.gestureSwitch)
        ttsSwitch = findViewById(R.id.voiceCommandSwitch)
        languageSpinner = findViewById(R.id.languageSpinner)
        voiceSpeedSpinner = findViewById(R.id.voiceSpeedSpinner)

        // Load and apply saved Gesture Motion preference
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        gestureSwitch.isChecked = isGestureEnabled
        gestureSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("GestureMotionEnabled", isChecked).apply()
            if (isChecked) {
                GestureMotionHelper.enableGestureMotion()
                Toast.makeText(this, "Gesture Motion Enabled", Toast.LENGTH_SHORT).show()
            } else {
                GestureMotionHelper.disableGestureMotion()
                Toast.makeText(this, "Gesture Motion Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Load and apply saved TTS preference
        val isTtsEnabled = sharedPreferences.getBoolean("TtsEnabled", true)
        ttsSwitch.isChecked = isTtsEnabled
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("TtsEnabled", isChecked).apply()
            if (isChecked) {
                TTSManager.enableTts(this)
                TTSManager.speak("Text-to-Speech Enabled", this)
                Toast.makeText(this, "Text-to-Speech Enabled", Toast.LENGTH_SHORT).show()
            } else {
                TTSManager.disableTts(this)
                Toast.makeText(this, "Text-to-Speech Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Load and apply saved Language preference
        val languages = resources.getStringArray(R.array.language_options)
        val savedLanguagePosition = sharedPreferences.getInt("SelectedLanguage", 0)
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter
        languageSpinner.setSelection(savedLanguagePosition)
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                sharedPreferences.edit().putInt("SelectedLanguage", position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Load and apply saved Voice Speed preference
        val voiceSpeeds = resources.getStringArray(R.array.voice_speed_options)
        val savedVoiceSpeedPosition = sharedPreferences.getInt("SelectedVoiceSpeed", 0)
        val voiceSpeedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceSpeeds)
        voiceSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceSpeedSpinner.adapter = voiceSpeedAdapter
        voiceSpeedSpinner.setSelection(savedVoiceSpeedPosition)
        voiceSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                sharedPreferences.edit().putInt("SelectedVoiceSpeed", position).apply()
                TTSManager.updateSpeechRateBasedOnSpinner(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun refreshPreferencesState() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Refresh gesture switch state
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        gestureSwitch.isChecked = isGestureEnabled

        // Refresh TTS switch state
        val isTtsEnabled = sharedPreferences.getBoolean("TtsEnabled", true)
        ttsSwitch.isChecked = isTtsEnabled

        // Refresh language spinner state
        val savedLanguagePosition = sharedPreferences.getInt("SelectedLanguage", 0)
        if (::languageSpinner.isInitialized) {
            languageSpinner.setSelection(savedLanguagePosition)
        }

        // Refresh voice speed spinner state
        val savedVoiceSpeedPosition = sharedPreferences.getInt("SelectedVoiceSpeed", 0)
        if (::voiceSpeedSpinner.isInitialized) {
            voiceSpeedSpinner.setSelection(savedVoiceSpeedPosition)
        }
    }

    private fun setupSaveChangesButton(tempPreferences: MutableMap<String, Any>) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Persist the temporary preferences
        tempPreferences.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
            }
        }
        // Save selected Voice Speed explicitly
        val selectedVoiceSpeedPosition = voiceSpeedSpinner.selectedItemPosition
        editor.putInt("SelectedVoiceSpeed", selectedVoiceSpeedPosition)

        editor.apply()

        // Apply runtime changes
        if (tempPreferences["GestureMotionEnabled"] as? Boolean == true) {
            GestureMotionHelper.enableGestureMotion()
            Toast.makeText(this, "Gesture Motion Enabled", Toast.LENGTH_SHORT).show()
            TTSManager.speak("Gesture Motion Enabled", this)
        } else {
            GestureMotionHelper.disableGestureMotion()
            Toast.makeText(this, "Gesture Motion Disabled", Toast.LENGTH_SHORT).show()
            TTSManager.speak("Gesture Motion Disabled", this)
        }

        if (tempPreferences["TtsEnabled"] as? Boolean == true) {
            TTSManager.enableTts(this)
            Toast.makeText(this, "Text-to-Speech Enabled", Toast.LENGTH_SHORT).show()
            TTSManager.speak("Text-to-Speech Enabled", this)
        } else {
            TTSManager.disableTts(this)
            Toast.makeText(this, "Text-to-Speech Disabled", Toast.LENGTH_SHORT).show()
            TTSManager.speak("Text-to-Speech Disabled", this)
        }
    }

    private fun updateGestureSwitch(tempPreferences: MutableMap<String, Any>) {
        val gestureSwitch: Switch = findViewById(R.id.gestureSwitch)

        // Load the saved gesture state into the switch
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        gestureSwitch.isChecked = isGestureEnabled

        // Set up toggle listener to update temporary state
        gestureSwitch.setOnCheckedChangeListener { _, isChecked ->
            tempPreferences["GestureMotionEnabled"] = isChecked
            if (isChecked) {
                TTSManager.speak("Gesture Motion Enabled", this)
                Toast.makeText(this, "Gesture Motion Enabled", Toast.LENGTH_SHORT).show()
                GestureMotionHelper.enableGestureMotion()
            }
            else {
                TTSManager.speak("Gesture Motion Disabled", this)
                Toast.makeText(this, "Gesture Motion Disabled", Toast.LENGTH_SHORT).show()
                GestureMotionHelper.disableGestureMotion()
            }
        }
    }

    private fun updateTtsSwitch(tempPreferences: MutableMap<String, Any>) {
        val ttsSwitch: Switch = findViewById(R.id.voiceCommandSwitch)
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load the saved TTS state
        val isTtsEnabled = sharedPreferences.getBoolean("TtsEnabled", true)
        ttsSwitch.isChecked = isTtsEnabled

        // Set up toggle listener for TTS switch
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                TTSManager.enableTts(this)
                TTSManager.speak("Text-to-Speech Enabled", this)
                Toast.makeText(this, "Text-to-Speech Enabled", Toast.LENGTH_SHORT).show()
            } else {
                TTSManager.disableTts(this)
                Toast.makeText(this, "Text-to-Speech Disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun updateDdlLanguage(tempPreferences: MutableMap<String, Any>){
        // Initialize spinners
        languageSpinner = findViewById(R.id.languageSpinner)

        // TO SET UP LANGUAGE SPINNER
        val languages = resources.getStringArray(R.array.language_options)
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter

        // TO SETUP LISTENER FOR ITEM SELECTION IN LANGUAGE SPINNER
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                btnSave.setOnClickListener{
                   //
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

    }

    private fun updateDdlVoiceSpeed(tempPreferences: MutableMap<String, Any>){
        voiceSpeedSpinner = findViewById(R.id.voiceSpeedSpinner)

        // TO SET UP VOICE SPEED SPINNER
        val voiceSpeeds = resources.getStringArray(R.array.voice_speed_options)
        val voiceSpeedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceSpeeds)
        voiceSpeedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceSpeedSpinner.adapter = voiceSpeedAdapter

        // TO SETUP LISTENER FOR ITEM SELECTION IN VOICE SPINNER
        voiceSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedVoiceSpeed = voiceSpeeds[position]

                btnSave.setOnClickListener{
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
        refreshPreferencesState()
        TTSManager.speak("Setting Activity", this)
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