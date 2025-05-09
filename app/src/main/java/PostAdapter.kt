import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.zappy.R

class PostAdapter(private val postList: MutableList<Post>, private val onCommentClickListener: (Post) -> Unit) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userIdTextView: TextView = itemView.findViewById(R.id.textViewUserId)
        val imageViewPost: ImageView = itemView.findViewById(R.id.imageViewPostItem)
        val captionTextView: TextView = itemView.findViewById(R.id.textViewCaption)
        val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)
        val likeButton: ImageView = itemView.findViewById(R.id.buttonLike)
        val likeCountTextView: TextView = itemView.findViewById(R.id.textViewLikeCount)
        val commentsButton: TextView = itemView.findViewById(R.id.buttonComments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]
        holder.userIdTextView.text = currentPost.userId
        holder.captionTextView.text = currentPost.caption
        holder.timestampTextView.text = currentPost.timestamp
        holder.likeCountTextView.text = currentPost.likeCount.toString()

        // Load image
        if (!currentPost.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(currentPost.imageUrl)
                .apply(RequestOptions().placeholder(android.R.color.darker_gray))
                .into(holder.imageViewPost)
        } else if (!currentPost.imageString.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(currentPost.imageString, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.imageViewPost.setImageBitmap(decodedBitmap)
            } catch (e: IllegalArgumentException) {
                holder.imageViewPost.setImageResource(android.R.color.darker_gray)
            }
        } else {
            holder.imageViewPost.setImageResource(android.R.color.darker_gray)
        }

        // Set like button state and listener
        currentUserId?.let { userId ->
            firestore.collection("posts").document(currentPost.postId ?: "")
                .collection("likes").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        holder.likeButton.setImageResource(R.drawable.ic_heart_filled)
                    } else {
                        holder.likeButton.setImageResource(R.drawable.ic_heart_outline)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PostAdapter", "Error checking like status: ${e.message}")
                }

            holder.likeButton.setOnClickListener {
                val postId = currentPost.postId
                if (postId != null) {
                    likePost(postId, holder)
                }
            }
        }

        // Set comment button listener
        holder.commentsButton.setOnClickListener {
            onCommentClickListener(currentPost)
        }
    }

    private fun likePost(postId: String, holder: PostViewHolder) {
        currentUserId?.let { userId ->
            val likeDocumentRef = firestore.collection("posts").document(postId).collection("likes").document(userId)

            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(firestore.collection("posts").document(postId))
                val currentLikeCount = postSnapshot.getLong("likeCount") ?: 0

                if (transaction.get(likeDocumentRef).exists()) {
                    // Unlike the post
                    transaction.delete(likeDocumentRef)
                    transaction.update(firestore.collection("posts").document(postId), "likeCount", currentLikeCount - 1)
                    null
                } else {
                    // Like the post
                    transaction.set(likeDocumentRef, mapOf("userId" to userId))
                    transaction.update(firestore.collection("posts").document(postId), "likeCount", currentLikeCount + 1)
                    null
                }
            }.addOnSuccessListener {
                // Update UI locally
                val currentLikeCount = holder.likeCountTextView.text.toString().toLong()
                if (holder.likeButton.drawable.constantState == ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_heart_outline)?.constantState) {
                    holder.likeButton.setImageResource(R.drawable.ic_heart_filled)
                    holder.likeCountTextView.text = (currentLikeCount + 1).toString()
                } else {
                    holder.likeButton.setImageResource(R.drawable.ic_heart_outline)
                    holder.likeCountTextView.text = (currentLikeCount - 1).toString()
                }
                // Update the postList to reflect the change locally (optional, but good for immediate UI feedback)
                val position = postList.indexOfFirst { it.postId == postId }
                if (position != -1) {
                    postList[position].likeCount = holder.likeCountTextView.text.toString().toLong()
                }
            }.addOnFailureListener { e ->
                Log.e("PostAdapter", "Error liking/unliking post: ${e.message}")
                // Optionally show a Toast message
            }
        }
    }

    override fun getItemCount() = postList.size

    fun submitList(newPostList: List<Post>) {
        postList.clear()
        postList.addAll(newPostList)
        notifyDataSetChanged()
    }
}