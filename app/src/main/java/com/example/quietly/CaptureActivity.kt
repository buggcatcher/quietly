package com.example.quietly

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import missing.namespace.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import missing.namespace.databinding.ActivityCaptureBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCaptureBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
                getCurrentLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request camera, audio, and location permissions
        if (allPermissionsGranted()) {
            startCamera()
            getCurrentLocation()
        } else {
            requestPermissions()
        }

        // Set up the listener for take photo and record audio
        viewBinding.captureButton.setOnClickListener {
            takePhoto()
            recordAudio()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val metadata = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, generateFileName())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            currentLocation?.let {
                put(MediaStore.Images.ImageColumns.LATITUDE, it.latitude)
                put(MediaStore.Images.ImageColumns.LONGITUDE, it.longitude)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, metadata)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(this@CaptureActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun recordAudio() {
        if (mediaRecorder != null) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Toast.makeText(this@CaptureActivity, "Recording stopped", Toast.LENGTH_SHORT).show()
            return
        }

        val folderPath = getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/Quietly"
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val audioFileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".m4a"
        audioFilePath = folderPath + "/" + audioFileName

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                Toast.makeText(this@CaptureActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e(TAG, "Audio recording failed: ${e.message}", e)
                Toast.makeText(this@CaptureActivity, "Recording failed to start", Toast.LENGTH_SHORT).show()
                mediaRecorder?.release()
                mediaRecorder = null
            }
        }

        // Save location data
        currentLocation?.let { location ->
            val locationFileName = audioFileName.replace(".m4a", "_location.txt")
            val locationFile = File(folderPath, locationFileName)
            locationFile.writeText("Latitude: ${location.latitude}\nLongitude: ${location.longitude}")
        }

        // Stop recording after 3 seconds
        viewBinding.captureButton.postDelayed({
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val msg = "Audio recording succeeded: $audioFilePath"
            Toast.makeText(this@CaptureActivity, msg, Toast.LENGTH_SHORT).show()
            Log.d(TAG, msg)
        }, 3000)
    }


    private fun generateFileName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = it
                }
            }
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
