package com.example.easee

import TTSManager
import android.content.Intent
import android.gesture.GestureOverlayView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.easee.utils.FirebaseHelper
import com.example.easee.utils.GestureMotionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userPhoneTextView: TextView
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // TO SETUP TOOLBAR
        setupToolbar()

        // TO SETUP GESTURE MOTION
        val gestureOverlayView = findViewById<GestureOverlayView>(R.id.gestureOverlayView)
        GestureMotionHelper.initialize(this, gestureOverlayView, R.raw.gestures)

        // Set profile pic circular
        val profileImage: ImageView = findViewById(R.id.profileImage)
        profileImage.background = getDrawable(R.drawable.circular_image)
        profileImage.clipToOutline = true

        // TO SETUP TEXT TO SPEECH
        TTSManager.init(this)
        TTSManager.speak("User Page Activity")

        // Check if the user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            Toast.makeText(this, "You must log in to access the profile.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Prevent returning to this activity
            return
        }

        // Initialize TextViews
        userNameTextView = findViewById(R.id.userName)
        userEmailTextView = findViewById(R.id.userEmail)
        userPhoneTextView = findViewById(R.id.userPhone)
        storage = FirebaseStorage.getInstance()

        // Fetch and display user data
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            fetchUserData(userId)
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }



        findViewById<View>(R.id.editProfileButton).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.viewHistoryButton).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<View>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        findViewById<View>(R.id.logoutButton).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LandingPageActivity::class.java))
            finish() // Close the current activity to prevent the user from going back
        }
    }


    private fun setupToolbar(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "User Profile"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }
    }

    override fun onResume() {
        super.onResume()
        // Fetch and display user data
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            fetchUserData(userId)// Fetch data each time the activity is resumed
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }

        TTSManager.speak("User Page Activity")

    }

    private fun fetchUserData(userId: String) {
        FirebaseHelper.fetchUserProfile(
            userId,
            onSuccess = { profileData, profileImageUri ->
                userNameTextView.text = profileData["name"]
                userEmailTextView.text = profileData["email"]
                userPhoneTextView.text = "${profileData["countryCode"]} ${profileData["phone"]}"

                if (profileImageUri != null) {
                    Glide.with(this)
                        .load(profileImageUri)
                        .placeholder(R.drawable.ic_default_user)
                        .into(findViewById(R.id.profileImage))
                } else {
                    Log.d("UserProfileActivity", "No image selected")
                }
            },
            onFailure = { exception ->
                Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("UserProfileActivity", "Error fetching user data", exception)
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        //TTSManager.shutdown()
        //Glide.with(this).onDestroy()
    }
}
