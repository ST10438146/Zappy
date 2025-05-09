import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import vcmsa.projects.zappy.R
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePost: Fragment() {
    private lateinit var imageViewPost: ImageView
    private lateinit var buttonCaptureImage: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var editTextCaption: EditText
    private lateinit var buttonPost: Button

    private var capturedImageUri: Uri? = null
    private var selectedImageBitmap: Bitmap? = null

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                openGallery()
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraActivityResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                capturedImageUri?.let { uri ->
                    imageViewPost.setImageURI(uri)
                    selectedImageBitmap = uriToBitmap(uri)
                } else {
                    Toast.makeText(requireContext(), "Failed to capture image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Image capture cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                imageUri?.let { uri ->
                    imageViewPost.setImageURI(uri)
                    selectedImageBitmap = uriToBitmap(uri)
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to select image.", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(requireContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to get image from gallery.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("CreatePostFragment", "Error converting URI to Bitmap: ${e.message}")
            Toast.makeText(requireContext(), "Error loading image.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageViewPost = view.findViewById(R.id.imageViewPost)
        buttonCaptureImage = view.findViewById(R.id.buttonCaptureImage)
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage)
        editTextCaption = view.findViewById(R.id.editTextCaption)
        buttonPost = view.findViewById(R.id.buttonPost)

        buttonCaptureImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                cameraPermissionResult.launch(Manifest.permission.CAMERA)
            }
        }

        buttonSelectImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        buttonPost.setOnClickListener {
            uploadPost()
        }
    }

    private fun startCamera() {
        val fileName = "JPEG_${System.currentTimeMillis()}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp") // Optional subfolder
            }
        }

        capturedImageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        capturedImageUri?.let { uri ->
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            try {
                cameraActivityResult.launch(takePictureIntent)
            } catch (e: Exception) {
                Log.e("CreatePostFragment", "Error starting camera: ${e.message}")
                Toast.makeText(requireContext(), "Error starting camera.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Failed to create image file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            galleryActivityResult.launch(intent)
        } catch (e: Exception) {
            Log.e("CreatePostFragment", "Error opening gallery: ${e.message}")
            Toast.makeText(requireContext(), "Error opening gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPost() {
        val caption = editTextCaption.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (selectedImageBitmap == null) {
            Toast.makeText(requireContext(), "Please select an image.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        buttonPost.isEnabled = false // Disable button to prevent multiple clicks
        Toast.makeText(requireContext(), "Uploading post...", Toast.LENGTH_SHORT).show()

        // Convert Bitmap to String (Base64)
        val byteArrayOutputStream = ByteArrayOutputStream()
        selectedImageBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val imageString = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

        // Generate a unique filename for Firestore Storage
        val imageName = "post_${System.currentTimeMillis()}.jpg"
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child("images/$userId/$imageName")

        // Upload the image to Firebase Storage
        val uploadTask = storageRef.putBytes(byteArrayOutputStream.toByteArray())
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Get the download URL
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                savePostToFirestore(downloadUri.toString(), caption, userId, imageString)
            }.addOnFailureListener { e ->
                buttonPost.isEnabled = true
                Toast.makeText(requireContext(), "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreatePostFragment", "Failed to get download URL: ${e.message}")
            }
        }.addOnFailureListener { e ->
            buttonPost.isEnabled = true
            Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("CreatePostFragment", "Image upload failed: ${e.message}")
        }
    }

    private fun savePostToFirestore(imageUrl: String, caption: String, userId: String, imageString: String) {
        val firestore = FirebaseFirestore.getInstance()
        val post = hashMapOf(
            "imageUrl" to imageUrl,
            "caption" to caption,
            "userId" to userId,
            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "imageString" to imageString // Saving the base64 string as requested
        )

        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener { documentReference ->
                buttonPost.isEnabled = true
                Toast.makeText(requireContext(), "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                // Clear the UI after successful post
                imageViewPost.setImageResource(android.R.color.darker_gray)
                editTextCaption.text.clear()
                selectedImageBitmap = null
            }
            .addOnFailureListener { e ->
                buttonPost.isEnabled = true
                Toast.makeText(requireContext(), "Failed to save post data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CreatePostFragment", "Failed to save post data: ${e.message}")
            }
    }
}