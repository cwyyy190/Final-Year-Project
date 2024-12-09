package com.example.easee


import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class ImageDetectionActivity : AppCompatActivity() {

    private lateinit var imageViewSelected: ImageView
    private lateinit var buttonSelect: Button
    private lateinit var textViewResults: TextView

    private lateinit var imageUri: Uri
    private lateinit var bitmap: Bitmap
    private lateinit var gptAnalyzer: GPTAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_detection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top / 2, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()

        TTSManager.init(this)
        TTSManager.speak("Image Detection Activity")

        // HELP BUTTON LISTENERS
        val btnHelp: ImageButton = findViewById(R.id.btnHelp)
        btnHelp.setOnClickListener{
            TTSManager.speakHelp()
        }

        imageViewSelected = findViewById(R.id.imageViewSelected)
        buttonSelect = findViewById(R.id.buttonSelect)
        textViewResults = findViewById(R.id.textViewResults)

        val apiKey = "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"

        val gptAnalyzer = GPTAnalyzer()
        lifecycleScope.launch {
            try {
                val response = gptAnalyzer.analyzeImage(bitmap, "$apiKey")
                textViewResults.text = response
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ImageDetectionActivity, "Failed to analyze image", Toast.LENGTH_SHORT).show()
                textViewResults.text ="Failed to analyze image. Error: ${e.message}"
            }
        }



    }



    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }



    data class DetectionResult(
        val name: String,
        val description: String,
        val color: String,
        val category: String
    )


    private fun setupToolbar(){

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Image Detection"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

    }



    override fun onResume() {
        super.onResume()
        TTSManager.speak("Image Detection Activity")
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
