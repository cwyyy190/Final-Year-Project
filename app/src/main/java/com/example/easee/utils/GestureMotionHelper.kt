package com.example.easee.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.gesture.Gesture
import android.gesture.GestureLibraries
import android.gesture.GestureLibrary
import android.gesture.GestureOverlayView
import android.util.Log
import com.example.easee.ImageDetectionActivity
import com.example.easee.RealtimeDetectionActivity
import com.example.easee.UserProfileActivity

@SuppressLint("StaticFieldLeak")
object GestureMotionHelper : GestureOverlayView.OnGesturePerformedListener {

    private var gLibrary: GestureLibrary? = null
    var isGestureMotionEnabled: Boolean = true
    private var gestureOverlayView: GestureOverlayView? = null // Make it mutable and set during initialization

    fun initialize(context: Context, gestureOverlayView: GestureOverlayView, rawResourceId: Int) {
        gLibrary = GestureLibraries.fromRawResource(context, rawResourceId)
        if (gLibrary?.load() == false) {
            Log.e("GestureMotionHelper", "Failed to load gesture library")
            return
        }

        this.gestureOverlayView = gestureOverlayView // Assign the overlay view
        updateGestureOverlayState() // Apply the initial state
        gestureOverlayView.addOnGesturePerformedListener(this) // Add listener initially if needed
    }

    fun enableGestureMotion() {
        isGestureMotionEnabled = true
        updateGestureOverlayState()
    }

    fun disableGestureMotion() {
        isGestureMotionEnabled = false
        updateGestureOverlayState()
    }

    private fun updateGestureOverlayState() {
        gestureOverlayView?.let { overlay ->
            if (isGestureMotionEnabled) {
                overlay.isEnabled = true
                overlay.isGestureVisible = true
                overlay.addOnGesturePerformedListener(this) // Ensure listener is added when enabled
            } else {
                overlay.isEnabled = false
                overlay.isGestureVisible = false
                overlay.removeOnGesturePerformedListener(this) // Ensure listener is removed when disabled
            }
        }
    }

    override fun onGesturePerformed(overlay: GestureOverlayView?, gesture: Gesture?) {
        if (!isGestureMotionEnabled || gLibrary == null) return

        val predictions = gLibrary?.recognize(gesture)
        if (predictions != null && predictions.isNotEmpty()) {
            val prediction = predictions[0]
            if (prediction.score > 1.0) {
                Log.d("GestureMotionHelper", "Gesture recognized: ${prediction.name}")
                overlay?.context?.let { context -> handleGesture(prediction.name, context) }
            }
        }
    }

    private fun handleGesture(action: String, context: Context) {
        when (action) {
            "swipeRight", "swipeLeft" -> GestureActionHandler.navigateToHomePage(context)
            "swipeUp" -> GestureActionHandler.navigateToUserProfile(context)
            "tick" -> GestureActionHandler.saveObject(context)
            "circle" -> GestureActionHandler.navigateToDetection(context)
            "zGesture" -> GestureActionHandler.toggleSpeechToText(context)
            else -> Log.d("GestureMotionHelper", "Unknown gesture: $action")
        }
    }

    object GestureActionHandler {
        fun navigateToHomePage(context: Context){
            val intent = Intent(context, RealtimeDetectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        fun backtoPreviousPage() {
            // Logic for navigating back
        }

        fun saveObject(context: Context) {
            // Logic for saving objects
        }

        fun navigateToDetection(context: Context) {
            val intent = Intent(context, RealtimeDetectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun navigateToUserProfile(context: Context) {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun toggleSpeechToText(context: Context) {
            STTManager.startListening(
                context,
                onResult = { result ->
                   // editText.setText(result)
                },
                onError = { error ->
                   // editText.setText("Error: $error")
                }
            )
        }
    }
}
