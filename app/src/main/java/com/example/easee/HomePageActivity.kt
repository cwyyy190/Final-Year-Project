package com.example.easee

import android.content.Intent
import android.gesture.GestureOverlayView
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.easee.utils.GestureMotionHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomePageActivity : AppCompatActivity(){


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        TTSManager.init(this)
        bottomNavBar()

        // Check gesture motion state before initializing gestures
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        val gestureOverlayView = findViewById<GestureOverlayView>(R.id.gestureOverlayView)

        GestureMotionHelper.initialize(this, gestureOverlayView, R.raw.gestures)
        if (isGestureEnabled) {
            GestureMotionHelper.enableGestureMotion()
        } else {
            GestureMotionHelper.disableGestureMotion()
        }

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }
    }


    private fun bottomNavBar() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Set `nav_home` as the selected menu item
        bottomNavigationView.selectedItemId = R.id.nav_home

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_user -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_object_detection -> {
                    val intent = Intent(this, RealtimeDetectionActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_home -> {
                    true
                }
                else -> false
            }
        }
    }
    override fun onResume() {
        super.onResume()
        TTSManager.speak("Home Page Activity")

        // Set `nav_home` as the selected menu item
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isGestureEnabled = sharedPreferences.getBoolean("GestureMotionEnabled", true)
        val gestureOverlayView = findViewById<GestureOverlayView>(R.id.gestureOverlayView)

        if (isGestureEnabled) {
            GestureMotionHelper.enableGestureMotion()
        } else {
            GestureMotionHelper.disableGestureMotion()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        //TTSManager.shutdown()
    }
}
