import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import vcmsa.projects.zappy.R
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var editTextSearchUsers: TextInputEditText
    private lateinit var buttonSearchUsers: Button
    private lateinit var recyclerViewUserSearchResults: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<AppUser>()

    private lateinit var editTextSearchPosts: TextInputEditText
    private lateinit var buttonSearchPosts: Button
    private lateinit var recyclerViewPostSearchResults: RecyclerView
    private lateinit var postAdapter: PostAdapter // Re-uses existing PostAdapter
    private val postList = mutableListOf<Post>()

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextSearchUsers = view.findViewById(R.id.editTextSearchUsers)
        buttonSearchUsers = view.findViewById(R.id.buttonSearchUsers)
        recyclerViewUserSearchResults = view.findViewById(R.id.recyclerViewUserSearchResults)

        userAdapter = UserAdapter(userList) { user ->
            // Handles user click
            Toast.makeText(requireContext(), "Clicked user: ${user.name}", Toast.LENGTH_SHORT).show()

        }
        recyclerViewUserSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewUserSearchResults.adapter = userAdapter

        editTextSearchPosts = view.findViewById(R.id.editTextSearchPosts)
        buttonSearchPosts = view.findViewById(R.id.buttonSearchPosts)
        recyclerViewPostSearchResults = view.findViewById(R.id.recyclerViewPostSearchResults)

        postAdapter = PostAdapter(postList) { post ->
            // Handles post click
            Toast.makeText(requireContext(), "Clicked post by: ${post.userId}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewPostSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPostSearchResults.adapter = postAdapter

        buttonSearchUsers.setOnClickListener {
            searchUsers()
        }

        // Listen for 'Enter' key press on username search field
        editTextSearchUsers.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchUsers()
                true
            } else {
                false
            }
        }

        buttonSearchPosts.setOnClickListener {
            searchPostsByTag()
        }

        // Listen for 'Enter' key press on tag search field
        editTextSearchPosts.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPostsByTag()
                true
            } else {
                false
            }
        }
    }

    private fun searchUsers() {
        val queryText = editTextSearchUsers.text.toString().trim()
        if (queryText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a username to search", Toast.LENGTH_SHORT).show()
            userAdapter.submitList(emptyList()) // Clears previous results
            return
        }

        // Firestore query for username search
        firestore.collection("users")
            .orderBy("name") // Order by the username field
            .startAt(queryText)
            .endAt(queryText + "\uf8ff") // Unicode character to make it an "ends with" query
            .get()
            .addOnSuccessListener { result ->
                val fetchedUsers = result.toObjects<AppUser>()
                if (fetchedUsers.isEmpty()) {
                    Toast.makeText(requireContext(), "No users found.", Toast.LENGTH_SHORT).show()
                }
                userAdapter.submitList(fetchedUsers)
            }
            .addOnFailureListener { e ->
                Log.e("SearchFragment", "Error searching users: ${e.message}")
                Toast.makeText(requireContext(), "Error searching users.", Toast.LENGTH_SHORT).show()
                userAdapter.submitList(emptyList())
            }
    }

    private fun searchPostsByTag() {
        val tagInput = editTextSearchPosts.text.toString().trim()
        if (tagInput.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a tag to search", Toast.LENGTH_SHORT).show()
            postAdapter.submitList(emptyList()) // Clears previous results
            return
        }

        // Ensures tag is lowercase for consistent search as saved in CreatePostFragment
        val tag = tagInput.lowercase(Locale.getDefault())

        firestore.collection("posts")
            .whereArrayContains("tags", tag) // Searchs for posts containing this tag
            .orderBy("timestamp", Query.Direction.DESCENDING) // order results
            .get()
            .addOnSuccessListener { result ->
                val fetchedPosts = mutableListOf<Post>()
                for (doc in result) {
                    val post = doc.toObject<Post>().copy(postId = doc.id)
                    fetchedPosts.add(post)
                }
                if (fetchedPosts.isEmpty()) {
                    Toast.makeText(requireContext(), "No posts found for this tag.", Toast.LENGTH_SHORT).show()
                }
                postAdapter.submitList(fetchedPosts)
            }
            .addOnFailureListener { e ->
                Log.e("SearchFragment", "Error searching posts by tag: ${e.message}")
                Toast.makeText(requireContext(), "Error searching posts.", Toast.LENGTH_SHORT).show()
                postAdapter.submitList(emptyList())
            }
    }
}