package com.example.easee.utils

import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream

object FirebaseHelper {
    // Firebase Auth instance
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance()}
    private lateinit var storage: FirebaseStorage


    // Firebase Database reference
    val database: DatabaseReference by lazy {FirebaseDatabase.getInstance().reference}

    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Check if the email, password, or confirmPassword are empty
    fun isSignUpInputValid(email: String, password: String, confirmPassword: String): String? {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            return "Please fill in all fields."
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address."
        }
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        if (password != confirmPassword) {
            return "Passwords do not match."
        }
        return null
    }

    // Check if the email and password are empty
    fun isLoginInputValid(email: String, password: String): String? {
        if (email.isEmpty() || password.isEmpty()) {
            return "Please fill in both email and password."
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address."
        }
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        return null
    }

    // TO FETCH USER PROFILE (NAME, EMAIL, COUNTRYCODE, PHONE)
    fun fetchUserProfile(
        userId: String,
        onSuccess: (profileData: Map<String, String>, profileImageUri: Uri?) -> Unit,
        onFailure: (Exception) -> Unit)
    {
        storage = FirebaseStorage.getInstance()
        val profileImageRef = storage.reference.child("profile_images/$userId/profile_image.jpg")

        // Fetch user details from Firestore
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileData = mapOf(
                        "name" to (document.getString("name") ?: "N/A"),
                        "email" to (document.getString("email") ?: "N/A"),
                        "countryCode" to (document.getString("countryCode") ?: "N/A"),
                        "phone" to (document.getString("phone") ?: "N/A")
                    )

                    // Fetch profile image URL
                    profileImageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onSuccess(profileData, uri)
                        }
                        .addOnFailureListener { exception ->
                            onSuccess(profileData, null) // Proceed with profile data even if image fails
                        }
                } else {
                    onFailure(Exception("User data not found in Firestore."))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    //TO UPDATE USER PROFILE
    fun updateUserProfile(
        userId: String,
        profileData: Map<String, String>,
        profileImageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        // Reference to the user's Firestore document
        val userDocRef = firestore.collection("users").document(userId)

        // Update Firestore profile data
        userDocRef.update(profileData)
            .addOnSuccessListener {
                if (profileImageUri != null) {
                    // If there's a profile image, upload it to Firebase Storage
                    val profileImageRef = storage.reference.child("profile_images/$userId/profile_image.jpg")
                    val metadata = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build()

                    profileImageRef.putFile(profileImageUri, metadata)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            onFailure(exception)
                        }
                } else {
                    // If no profile image, finish with success
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // TO SAVE DETECTED OBJECT TO FIREBASE
    fun saveDetectedObject(
        userId: String,
        detectedObject: Map<String, Any>,
        croppedImage: Bitmap,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firestoreRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("detected_objects")

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("users/$userId/cropped_objects/${System.currentTimeMillis()}.jpg")

        val baos = ByteArrayOutputStream()
        croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        imageRef.putBytes(imageData, metadata)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val detectedObjectWithImage = detectedObject.toMutableMap().apply {
                        this["imageUrl"] = downloadUri.toString() // Add the image URL to Firestore document
                    }

                    firestoreRef.add(detectedObjectWithImage)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { exception -> onFailure(exception) }
                }.addOnFailureListener { exception -> onFailure(exception) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }



    // TO GET USER DATA REFERENCE
    fun getUserDataReference(userId: String): DatabaseReference {
        return database.child("users").child(userId)
    }
}
