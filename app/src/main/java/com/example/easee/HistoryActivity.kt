package com.example.easee

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easee.utils.FirebaseHelper
import java.util.Locale
import kotlin.math.log


class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetectedObjectsAdapter
    private lateinit var searchView: SearchView
    private val detectedObjects = mutableListOf<DetectedObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top , systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        TTSManager.init(this)
        TTSManager.speak("History Activity")

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.idSV)

        adapter = DetectedObjectsAdapter(detectedObjects)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchDetectedObjects() // Call to fetch data

        // SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = detectedObjects.filter {
                    it.label.contains(newText ?: "", ignoreCase = true)
                }
                adapter.updateData(filteredList)
                return true
            }
        })

    }

    private fun fetchDetectedObjects() {
        val currentUser = FirebaseHelper.auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            FirebaseHelper.firestore.collection("users")
                .document(userId)
                .collection("detected_objects")
                .get()
                .addOnSuccessListener { result ->
                    val objects = result.mapNotNull { document ->
                        try {
                            document.toObject(DetectedObject::class.java).apply {
                                this.imageUrl = document.getString("imageUrl") ?: ""
                            }
                        } catch (e: Exception) {
                            Log.e("FirestoreError", "Error parsing document: ${document.id}", e)
                            null
                        }
                    }
                    // Sort by timestamp in descending order (most recent first)
                    val sortedObjects = objects.sortedByDescending { it.timestamp }

                    updateRecyclerView(sortedObjects)
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Failed to fetch detected objects", exception)
                    Toast.makeText(this, "Failed to load detected objects: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRecyclerView(objects: List<DetectedObject>) {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        if (recyclerView.adapter == null) {
            recyclerView.adapter = DetectedObjectsAdapter(objects)
            recyclerView.layoutManager = LinearLayoutManager(this)
        } else {
            (recyclerView.adapter as DetectedObjectsAdapter).apply {
                (detectedObjects as MutableList).clear()
                detectedObjects.addAll(objects)
                notifyDataSetChanged()
            }
        }

        val emptyState = findViewById<TextView>(R.id.tvEmptyState)
        emptyState.visibility = if (objects.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        TTSManager.speak("History Activity")
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    private fun setupToolbar(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "History"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

}