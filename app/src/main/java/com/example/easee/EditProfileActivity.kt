package com.example.easee

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.easee.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditProfileActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var countryCodeSpinner: Spinner
    private lateinit var passwordEditText: EditText
    private lateinit var countryFlag: ImageView
    private lateinit var passwordToggle: ImageButton
    private lateinit var saveChange: Button
    private lateinit var profileImage: ImageView
    private lateinit var cameraIcon: ImageView
    private lateinit var firestore: FirebaseFirestore
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    private var selectedImageUri: Uri? = null
    private val storageReference = FirebaseStorage.getInstance().reference
    private val databaseReference = FirebaseDatabase.getInstance().reference.child("Users")
    private lateinit var storage: FirebaseStorage

    // Set up country code to flag map
    private val countryFlagMap = mapOf(
        "+60" to R.drawable.ic_malaysia,
        "+65" to R.drawable.ic_singapore
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top , systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        TTSManager.init(this)
        TTSManager.speak("Edit Profile Activity")

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        countryCodeSpinner = findViewById(R.id.countryCodeSpinner)
        passwordEditText = findViewById(R.id.passwordEditText)
        countryFlag = findViewById(R.id.countryFlag)
        passwordToggle = findViewById(R.id.passwordToggle)
        saveChange = findViewById(R.id.saveChangesButton)
        profileImage = findViewById(R.id.profileImage)
        cameraIcon = findViewById(R.id.cameraIcon)
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set profile pic circular
        profileImage.background = getDrawable(R.drawable.circular_image)
        profileImage.clipToOutline = true

        // Set the default flag as Malaysia
        countryFlag.setImageResource(R.drawable.ic_malaysia)

        // Set up country code spinner adapter
        val countryCodes = resources.getStringArray(R.array.country_code_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countryCodeSpinner.adapter = adapter

        // Set listener to update flag based on selected country code
        countryCodeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {
                val selectedCountryCode = countryCodes[position]
                val flagResource = countryFlagMap[selectedCountryCode]
                countryFlag.setImageResource(flagResource ?: R.drawable.ic_malaysia)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: Handle case when nothing is selected
            }
        }

        // Fetch user data to populate fields
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadUserData(userId)
            Toast.makeText(this, "User logged in." + userId, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }

        // Listener
        cameraIcon.setOnClickListener {
            openGallery()
        }

        // Toggle password visibility
        passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        // Save changes on button click
        saveChange.setOnClickListener {
            if (userId != null) {
                saveUserProfile()
                Toast.makeText(this, "Successfully updated your profile.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupToolbar(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Edit Profile"
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadUserData(userId)
            Toast.makeText(this, "User logged in." + userId, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }

        TTSManager.speak("Edit Profile Activity")
    }

    private fun loadUserData(userId: String) {
        FirebaseHelper.fetchUserProfile(
            userId,
            onSuccess = { profileData, profileImageUri ->
                // Populate UI fields with user data
                findViewById<EditText>(R.id.nameEditText).setText(profileData["name"])
                findViewById<EditText>(R.id.emailEditText).setText(profileData["email"])
                findViewById<EditText>(R.id.phoneEditText).setText(profileData["phone"])

                val countryCodeSpinner = findViewById<Spinner>(R.id.countryCodeSpinner)
                val countryCode = profileData["countryCode"] ?: "+1"
                val adapter = countryCodeSpinner.adapter as ArrayAdapter<String>
                val position = adapter.getPosition(countryCode)
                countryCodeSpinner.setSelection(position)

                // Load profile image
                if (profileImageUri != null) {
                    Glide.with(this)
                        .load(profileImageUri)
                        .placeholder(R.drawable.ic_default_user)
                        .into(findViewById(R.id.profileImage))
                } else {
                    findViewById<ImageView>(R.id.profileImage)
                        .setImageResource(R.drawable.ic_default_user)
                }
            },
            onFailure = { exception ->
                Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditProfileActivity", "Error loading user data", exception)
            }
        )
    }

    private fun saveUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val profileData = mapOf(
            "name" to nameEditText.text.toString(),
            "email" to emailEditText.text.toString(),
            "countryCode" to "+1", // Example country code
            "phone" to phoneEditText.text.toString()
        )

        val profileImageUri = selectedImageUri

        FirebaseHelper.updateUserProfile(
            userId,
            profileData,
            profileImageUri,
            onSuccess = {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                Toast.makeText(this, "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("UserProfileActivity", "Error updating profile", exception)
            }
        )
    }


    private fun updateProfile(userId: String) {
        updateProfileImage(userId)
    }

    private fun updateProfileImage(userId: String) {
        val storageReference = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}.jpg")

        storageReference.putFile(imageUri!!)
            .addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    updateProfileData(userId, uri.toString())
                    Log.d("EditProfileActivity", "Selected Image URI: $uri")

                }
                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateProfileData(userId: String, imageUrl: String) {
        val name = nameEditText.text.toString()
        val email = emailEditText.text.toString()
        val countryCode = findViewById<Spinner>(R.id.countryCodeSpinner).selectedItem.toString()
        val phone = phoneEditText.text.toString()

        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "countryCode" to countryCode,
            "phone" to phone,
            "imageURL" to imageUrl
        )

        firestore.collection("users").document(userId).update(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            Log.d("EditProfileActivity", "Selected Image URI: $selectedImageUri")
            selectedImageUri?.let {
                Glide.with(this).load(it).into(profileImage)
            }

        }
    }


    private fun togglePasswordVisibility() {
        // Toggle the visibility state
        isPasswordVisible = !isPasswordVisible

        // Update the input type based on the visibility state
        passwordEditText.inputType = if (isPasswordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // Set cursor position to the end of the text
        passwordEditText.setSelection(passwordEditText.text.length)

        // Update the icon based on the visibility state
        passwordToggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_visibility_on else R.drawable.ic_visiblity_off
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1001)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
