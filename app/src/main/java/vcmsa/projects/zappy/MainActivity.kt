package vcmsa.projects.zappy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var buttonLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        buttonLogout = findViewById(R.id.buttonLogout)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fabCreatePost = findViewById<FloatingActionButton>(R.id.fab_create_post)

        // Load the default fragment
        loadFragment(FeedFragment())
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(FeedFragment()) // Replace with feed fragment
                    true
                }
                R.id.navigation_create_post -> {
                    loadFragment(CreatePostFragment()) // Creates fragment
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment()) // Replace with profile fragment
                    true
                }
                else -> false
            }

        }
        fabCreatePost.setOnClickListener {
            loadFragment(CreatePostFragment())
            }
        fun loadFragment(fragment: Fragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
        buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        
    }
}