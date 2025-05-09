package vcmsa.projects.zappy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PostsActivity : AppCompatActivity() {

    private lateinit var imageViewPost: ImageView
    private lateinit var buttonCaptureImage: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var editTextCaption: EditText
    private lateinit var buttonPost: Button

    private var imageUri: Uri? = null
    private var capturedBitmap: Bitmap? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var storageRef: StorageReference

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    companion object {
        private const val TAG = "CreatePostActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    imageUri = uri
                    imageViewPost.setImageURI(uri)
                    capturedBitmap = null // Reset bitmap if a new image is selected
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_posts)

        imageViewPost = findViewById(R.id.imageViewPost)
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        editTextCaption = findViewById(R.id.editTextCaption)
        buttonPost = findViewById(R.id.buttonPost)

        storageRef = storage.reference.child("posts")
        cameraExecutor = Executors.newSingleThreadExecutor()

        buttonCaptureImage.setOnClickListener {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                cameraPermissionResult.launch(Manifest.permission.CAMERA)
            }
        }

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityResult.launch(intent)
        }

        buttonPost.setOnClickListener {
            uploadPost()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val previewView = PreviewView(this) // Create a PreviewView programmatically
        // For simplicity, we won't display the live camera feed in this basic example.
        // In a real app, you'd add this PreviewView to your layout.

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                takePhoto() // Immediately take a photo after starting the camera for this example
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed: $exc", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        // Create a temporary file (you might want to handle this differently in a production app)
        // val contentValues = ContentValues().apply {
        //     put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        //     put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        //     if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        //         put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        //     }
        // }
        // val outputOptions = ImageCapture.OutputFileOptions
        //     .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        //     .build()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@PostsActivity, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                 fun onImageCaptured(bitmap: Bitmap) {
                    super.onImageCaptured(bitmap)
                    capturedBitmap = bitmap
                    imageViewPost.setImageBitmap(bitmap)
                    imageUri = null // Reset URI as we have the bitmap
                    Toast.makeText(this@PostsActivity, "Photo captured!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadPost() {
        val caption = editTextCaption.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (capturedBitmap == null && imageUri == null) {
            Toast.makeText(this, "Please select or capture an image", Toast.LENGTH_SHORT).show()
            return
        }

        buttonPost.isEnabled = false
        Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUrl = if (imageUri != null) {
                    uploadImageToStorage(imageUri!!)
                } else if (capturedBitmap != null) {
                    uploadImageBitmapToStorage(capturedBitmap!!)
                } else {
                    null
                }

                if (imageUrl != null) {
                    savePostData(imageUrl, caption, userId)
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@PostsActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        buttonPost.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@PostsActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    buttonPost.isEnabled = true
                }
            }
        }
    }

    private suspend fun uploadImageToStorage(imageUri: Uri): String? {
        val filename = "post_${System.currentTimeMillis()}"
        val ref = storageRef.child(filename)
        return try {
            val uploadTask = ref.putFile(imageUri).await()
            if (uploadTask.task.isSuccessful) {
                ref.downloadUrl.await().toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to storage: ${e.message}")
            null
        }
    }

    private suspend fun uploadImageBitmapToStorage(bitmap: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()
        val filename = "post_${System.currentTimeMillis()}"
        val ref = storageRef.child(filename)
        return try {
            val uploadTask = ref.putBytes(data).await()
            if (uploadTask.task.isSuccessful) {
                ref.downloadUrl.await().toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading bitmap to storage: ${e.message}")
            null
        }
    }

    private suspend fun savePostData(imageUrl: String?, caption: String, userId: String) {
        val timestamp = Date()
        val post = hashMapOf(
            "imageUrl" to imageUrl,
            "imageBitmapString" to null, // We are uploading the URL, so this is null
            "caption" to caption,
            "userId" to userId,
            "timestamp" to timestamp
        )

        try {
            firestore.collection("posts").add(post).await()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@PostsActivity, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                finish() // Go back to the feed
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@PostsActivity, "Error saving post data: ${e.message}", Toast.LENGTH_SHORT).show()
                buttonPost.isEnabled = true
            }
        }
    }
}