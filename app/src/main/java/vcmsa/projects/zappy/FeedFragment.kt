package vcmsa.projects.zappy

import CommentsActivity
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
import android.content.Intent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // Import SwipeRefreshLayout
import com.google.firebase.firestore.ktx.toObjects

class FeedFragment : Fragment() {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        recyclerViewFeed.layoutManager = LinearLayoutManager(requireContext())
        postAdapter = PostAdapter(postList) { post ->
            // Handles comment button click
            val intent = Intent(requireContext(), CommentsActivity::class.java)
            intent.putExtra("postId", post.postId)
            startActivity(intent)
        }
        recyclerViewFeed.adapter = postAdapter

        swipeRefreshLayout.setOnRefreshListener {
            loadPosts() // Reloads posts on pull-to-refresh
        }

        loadPosts()
    }

    private fun loadPosts() {
        // Manually starts refreshing indicator if not already
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        firestore.collection("posts")
            // Ranking: Order by likeCount (descending) then timestamp (descending)
            // This will show posts with more likes first, and for equally liked posts, newer ones.
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get() // Uses get() for a single fetch
            .addOnSuccessListener { result ->
                val fetchedPosts = mutableListOf<Post>()
                for (doc in result) {
                    // Ensures postId is captured from document ID
                    val post = doc.toObject<Post>().copy(postId = doc.id)
                    fetchedPosts.add(post)
                }
                postAdapter.submitList(fetchedPosts)
                swipeRefreshLayout.isRefreshing = false // Hide srefreshing indicator
            }
            .addOnFailureListener { e ->
                Log.e("FeedFragment", "Error fetching posts: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load posts.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Hides refreshing indicator
            }
    }
}