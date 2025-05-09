package vcmsa.projects.zappy

import Post
import PostAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.toObject

class FeedFragment : Fragment() {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed)
        recyclerViewFeed.layoutManager = LinearLayoutManager(requireContext())
        postAdapter = PostAdapter(postList) { post ->
            // Handle comment button click here (e.g., navigate to a comment screen)
            showComments(post)
        }
        recyclerViewFeed.adapter = postAdapter

        loadPosts()
    }

    private fun loadPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("FeedFragment", "Listen failed: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to load posts.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val fetchedPosts = mutableListOf<Post>()
                for (doc in value!!) {
                    val post = doc.toObject<Post>().copy(postId = doc.id) // Include document ID as postId
                    fetchedPosts.add(post)
                }
                postAdapter.submitList(fetchedPosts)
            }
    }

    private fun showComments(post: Post) {
        // Implement logic to show comments for the selected post
        // This could involve navigating to a new activity/fragment or showing a bottom sheet
        Toast.makeText(requireContext(), "Show comments for: ${post.postId}", Toast.LENGTH_SHORT).show()
        // Example:
        // val intent = Intent(requireContext(), CommentsActivity::class.java)
        // intent.putExtra("postId", post.postId)
        // startActivity(intent)

    }
}