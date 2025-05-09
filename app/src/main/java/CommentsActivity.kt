import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import vcmsa.projects.zappy.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Comment(
    val userId: String = "",
    val text: String = "",
    val timestamp: String = ""
)

class CommentsActivity : AppCompatActivity() {

    private lateinit var commentsContainer: LinearLayout
    private lateinit var editTextComment: EditText
    private lateinit var buttonSubmitComment: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        commentsContainer = findViewById(R.id.commentsContainer)
        editTextComment = findViewById(R.id.editTextComment)
        buttonSubmitComment = findViewById(R.id.buttonSubmitComment)

        postId = intent.getStringExtra("postId")
        if (postId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Invalid post ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadComments(postId!!)

        buttonSubmitComment.setOnClickListener {
            val commentText = editTextComment.text.toString().trim()
            if (commentText.isNotEmpty() && currentUserId != null) {
                submitComment(postId!!, commentText, currentUserId)
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadComments(postId: String) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                commentsContainer.removeAllViews() // Clear previous comments
                if (error != null) {
                    Log.e("CommentsActivity", "Listen failed: ${error.message}")
                    Toast.makeText(this, "Failed to load comments.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val comments = value?.toObjects<Comment>()
                if (comments != null) {
                    for (comment in comments) {
                        addCommentToView(comment)
                    }
                }
            }
    }

    private fun addCommentToView(comment: Comment) {
        val commentView = LayoutInflater.from(this).inflate(R.layout.item_comment, commentsContainer, false)
        val commenterIdTextView = commentView.findViewById<TextView>(R.id.textViewCommenterId)
        val commentTextView = commentView.findViewById<TextView>(R.id.textViewCommentText)
        val commentTimestampTextView = commentView.findViewById<TextView>(R.id.textViewCommentTimestamp)

        commenterIdTextView.text = comment.userId
        commentTextView.text = comment.text
        commentTimestampTextView.text = comment.timestamp

        commentsContainer.addView(commentView)
    }

    private fun submitComment(postId: String, commentText: String, userId: String) {
        buttonSubmitComment.isEnabled = false // Disable button during submission
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val comment = Comment(userId, commentText, timestamp)

        firestore.collection("posts").document(postId).collection("comments")
            .add(comment)
            .addOnSuccessListener {
                editTextComment.text.clear()
                buttonSubmitComment.isEnabled = true
            }
            .addOnFailureListener { e ->
                Log.e("CommentsActivity", "Error submitting comment: ${e.message}")
                Toast.makeText(this, "Failed to submit comment.", Toast.LENGTH_SHORT).show()
                buttonSubmitComment.isEnabled = true
            }
    }
}