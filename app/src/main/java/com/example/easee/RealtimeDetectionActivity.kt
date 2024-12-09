package com.example.easee

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easee.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.Manifest
import android.graphics.Rect
import android.util.Size
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.example.easee.utils.FirebaseHelper

class RealtimeDetectionActivity : AppCompatActivity() {

    val paint = Paint()
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler:Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var imageProcessor: ImageProcessor
    lateinit var labels:List<String>
    lateinit var btnSave: Button
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    private var isCameraInitialized = false
    private var lastDetectedBoundingBox: RectF? = null


    // to add cooldown for detection
    private var lastDetectedObject: String? = null
    private var lastDetectionTime: Long = 0
    private val detectionCooldown: Long = 3000 // Cooldown in milliseconds
    private val confidenceThreshold = 0.6 // Minimum confidence level
    private var lastDetectedConfidence: Float? = null

    private lateinit var editText: EditText
    private lateinit var searchButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_realtime_detection)

        val btnSave: Button = findViewById(R.id.btnSave)

        TTSManager.init(this)
        TTSManager.speak("Realtime Detection Activity")

        setupToolbar()

        loadTensorModel()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            captureRealtimeVideo()
        }

        // Set up click listeners for the buttons
        btnSave.setOnClickListener {
            saveDetectedObject()
        }

        editText = findViewById(R.id.etSearchObject)
        searchButton = findViewById(R.id.btnSearchObject)

        STTManager.init(this)
        // Check and request permission when the button is clicked
        searchButton.setOnClickListener {
            checkAndRequestMicrophonePermission {
                startSpeechRecognition()
            }
        }
    }

    private fun checkAndRequestMicrophonePermission(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startSpeechRecognition()
            } else {
                editText.setText("Permission denied. Please enable microphone access.")
            }
        }
    private fun startSpeechRecognition() {
        STTManager.startListening(
            this,
            onResult = { result ->
                editText.setText(result)
            },
            onError = { error ->
                editText.setText("Error: $error")
            }
        )
    }

    // TO CROP IMAGE OF DETECTED OBJECTS
    private fun cropImage(bitmap: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val leftInt = left.toInt().coerceIn(0, bitmap.width)
        val topInt = top.toInt().coerceIn(0, bitmap.height)
        val width = (right.toInt().coerceIn(0, bitmap.width) - leftInt).coerceAtLeast(1)
        val height = (bottom.toInt().coerceIn(0, bitmap.height) - topInt).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, leftInt, topInt, width, height)
    }

    // TO SAVE DETECTED OBJECTS
    private fun saveDetectedObject() {
        val currentUser = FirebaseHelper.auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            if (lastDetectedObject != null && lastDetectedConfidence != null && lastDetectedBoundingBox != null) {
                val croppedImage = cropImage(
                    bitmap,
                    lastDetectedBoundingBox!!.left,
                    lastDetectedBoundingBox!!.top,
                    lastDetectedBoundingBox!!.right,
                    lastDetectedBoundingBox!!.bottom
                )

                val detectedObjectData: Map<String, Any> = mapOf(
                    "label" to lastDetectedObject!!,
                    "confidence" to lastDetectedConfidence!!,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseHelper.saveDetectedObject(
                    userId,
                    detectedObjectData,
                    croppedImage,
                    onSuccess = {
                        Toast.makeText(this, "Detected object and image saved successfully.", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "Error saving detected object: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "No object detected to save.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "You must be logged in to save detected objects.", Toast.LENGTH_SHORT).show()
        }
    }

    // TO SET UP TOOLBAR
    private fun setupToolbar(){
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Real-time Detection"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    // TO LOAD MODEL - TENSORFLOW MODEL
    private fun loadTensorModel(){
        model = SsdMobilenetV11Metadata1.newInstance(this)
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300,300,ResizeOp.ResizeMethod.BILINEAR)).build()
        labels = FileUtil.loadLabels(this, "labels.txt")
    }

    // TO CAPTURE IMAGE AND RUN AS VIDEO THREAD
    private fun captureRealtimeVideo(){
        if (isCameraInitialized) return // Prevent duplicate initialization
        isCameraInitialized = true

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        }

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)

        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    Log.d("TextureView", "SurfaceTexture available")
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                    Log.d("TextureView", "SurfaceTexture size changed")
                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    Log.d("TextureView", "SurfaceTexture destroyed")
                    return false
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                    Log.d("TextureView", "SurfaceTexture updated")
                    bitmap = textureView.bitmap!!

                    // Creates inputs for reference.
                    var image = TensorImage.fromBitmap(bitmap)
                    image = imageProcessor.process(image)

                    // Runs model inference and gets result.
                    val outputs = model.process(image)
                    val locations = outputs.locationsAsTensorBuffer.floatArray
                    val classes = outputs.classesAsTensorBuffer.floatArray
                    val scores = outputs.scoresAsTensorBuffer.floatArray
                    val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray


                    // TESTING
                    var detectedCount = 0
                    scores.forEachIndexed { index, confidence ->
                        if (confidence > confidenceThreshold) {
                            detectedCount++
                        }
                    }

                    // Update the TextView with the count
                    val tvNumOfObject: TextView = findViewById(R.id.tvNumOfObject)
                    tvNumOfObject.text = detectedCount.toString()


                    var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutable)

                    val h = mutable.height
                    val w = mutable.width

                    paint.textSize = h / 15f
                    paint.strokeWidth = h / 85f
                    var x = 0
                    scores.forEachIndexed { index, fl ->
                        x = index
                        x *= 4
                        if (fl > 0.6) {       //if confidence is more than 70%

                            // TO ADD COOLDOWN BETWEEN OBJECTS DETECTED
                            val detectedLabel = labels.get(classes.get(index).toInt())
                            handleObjectDetection(detectedLabel, fl)

                            val x = index * 4
                            val top = locations[x] * h
                            val left = locations[x + 1] * w
                            val bottom = locations[x + 2] * h
                            val right = locations[x + 3] * w

                            lastDetectedBoundingBox = RectF(left, top, right, bottom)

                            paint.setColor(colors.get(index))
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locations.get(x + 1) * w,
                                    locations.get(x) * h,
                                    locations.get(x + 3) * w,
                                    locations.get(x + 2) * h
                                ), paint
                            )
                            paint.style = Paint.Style.FILL
                            canvas.drawText(
                                labels.get(
                                    classes.get(index).toInt()
                                ) + " " + fl.toString(),
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                paint
                            )
                            //draw at top left corner
                        }
                    }
                    imageView.setImageBitmap(mutable)
                }
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager


    }

    // TO SET COOL DOWN TIME FOR INTERVAL BETWEEN OBJECTS DETECTED
    private fun handleObjectDetection(detectedObject: String, confidence: Float) {
        val currentTime = System.currentTimeMillis()

        if (confidence >= confidenceThreshold && (lastDetectedObject != detectedObject || currentTime - lastDetectionTime > detectionCooldown)) {
            lastDetectedObject = detectedObject
            lastDetectedConfidence = confidence
            lastDetectionTime = currentTime
            TTSManager.speak(detectedObject)

        }
    }


    // TO OPEN CAMERA
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission() // Request permission if not granted
            return
        }

        try {
            cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    this@RealtimeDetectionActivity.cameraDevice = cameraDevice

                    val surfaceTexture = textureView.surfaceTexture
                    surfaceTexture?.setDefaultBufferSize(textureView.width, textureView.height)
                    val surface = Surface(surfaceTexture)

                    val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequestBuilder.addTarget(surface)

                    cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            try {
                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, handler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            // Handle failure
                        }
                    }, handler)
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    cameraDevice.close()
                }

                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    cameraDevice.close()
                }
            }, handler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    // TO REQUEST CAMERA PERMISSION
    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("This app requires camera access to perform real-time object detection.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Camera permission denied. App cannot function properly.", Toast.LENGTH_SHORT).show()
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // TO DO WHEN PERMISSION GRANTED
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (textureView.isAvailable) {
                captureRealtimeVideo()
            } else {
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        captureRealtimeVideo()
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                }
            }
        } else {
            Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
        TTSManager.speak("Realtime Detection Activity")
    }


    override fun onDestroy() {

        super.onDestroy()
        // Releases model resources if no longer used.
        model.close()
        cameraDevice.close()
        //handler.looper.quitSafely()
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

}

