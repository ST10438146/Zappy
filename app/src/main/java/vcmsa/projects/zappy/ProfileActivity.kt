package vcmsa.projects.zappy

import Post
import PostAdapter
import android.Manifest
import android.app.Activity.RESULT_OK
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var imageViewProfilePic: ImageView
    private lateinit var buttonUpdateProfilePic: Button
    private lateinit var editTextUsername: EditText
    private lateinit var buttonUpdateUsername: Button
    private lateinit var editTextNewPassword: EditText
    private lateinit var buttonUpdatePassword: Button
    private lateinit var recyclerViewUserPosts: RecyclerView
    private lateinit var userPostsAdapter: PostAdapter
    private lateinit var buttonLogout: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference
    private val currentUser = auth.currentUser
    private var profileImageUri: Uri? = null

    private val galleryPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                profileImageUri = result.data?.data
                profileImageUri?.let { uri ->
                    imageViewProfilePic.setImageURI(uri)
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Profile picture update cancelled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to select profile picture.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imageViewProfilePic = findViewById(R.id.imageViewProfilePic)
        buttonUpdateProfilePic = findViewById(R.id.buttonUpdateProfilePic)
        editTextUsername = findViewById(R.id.editTextUsername)
        buttonUpdateUsername = findViewById(R.id.buttonUpdateUsername)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword)
        recyclerViewUserPosts = findViewById(R.id.recyclerViewUserPosts)
        buttonLogout = findViewById(R.id.buttonLogoutProfile)

        recyclerViewUserPosts.layoutManager = LinearLayoutManager(this)
        userPostsAdapter = PostAdapter(mutableListOf()) {} // Empty lambda for comment click in profile
        recyclerViewUserPosts.adapter = userPostsAdapter

        loadUserProfile()
        loadUserPosts()

        imageViewProfilePic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                galleryPermissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        buttonUpdateProfilePic.setOnClickListener {
            profileImageUri?.let { uploadProfilePicture(it) } ?: Toast.makeText(this, "Please select a profile picture.", Toast.LENGTH_SHORT).show()
        }

        buttonUpdateUsername.setOnClickListener {
            val newUsername = editTextUsername.text.toString().trim()
            if (newUsername.isNotEmpty() && currentUser != null) {
                updateUsername(newUsername)
            } else {
                Toast.makeText(this, "Please enter a username.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonUpdatePassword.setOnClickListener {
            val newPassword = editTextNewPassword.text.toString()
            if (newPassword.length >= 6 && currentUser != null) {
                updatePassword(newPassword)
            } else {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUserProfile() {
        currentUser?.let { user ->
            editTextUsername.setText(user.displayName)
            user.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(android.R.color.darker_gray)
                    .into(imageViewProfilePic)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryActivityResult.launch(intent)
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        currentUser?.let { user ->
            val profilePicRef: StorageReference = storageRef.child("profile_pics/${user.uid}")
            val uploadTask = profilePicRef.putFile(imageUri)

            uploadTask.addOnSuccessListener {
                profilePicRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateProfileImageUrl(downloadUri)
                }.addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Failed to get download URL: ${e.message}")
                    Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileActivity", "Profile picture upload failed: ${e.message}")
                Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfileImageUrl(downloadUri: Uri) {
        currentUser?.let { user ->
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(downloadUri)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ProfileActivity", "Failed to update profile image URL: ${task.exception?.message}")
                        Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updateUsername(newUsername: String) {
        currentUser?.let { user ->
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        editTextUsername.setText(newUsername)
                        Toast.makeText(this, "Username updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ProfileActivity", "Failed to update username: ${task.exception?.message}")
                        Toast.makeText(this, "Failed to update username.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updatePassword(newPassword: String) {
        currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    editTextNewPassword.text.clear()
                    Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ProfileActivity", "Failed to update password: ${task.exception?.message}")
                    Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserPosts() {
        currentUser?.uid?.let { userId ->
            firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e("ProfileActivity", "Listen failed for user posts: ${error.message}")
                        Toast.makeText(this, "Failed to load your posts.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val fetchedPosts = mutableListOf<Post>()
                    for (doc in value!!) {
                        val post = doc.toObject<Post>().copy(postId = doc.id)
                        fetchedPosts.add(post)
                    }
                    userPostsAdapter.submitList(fetchedPosts)
                }
        }
    }
}