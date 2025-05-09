package vcmsa.projects.zappy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.zappy.LoginActivity
import vcmsa.projects.zappy.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextName: EditText
    private lateinit var editTextEmailRegister: EditText
    private lateinit var editTextPasswordRegister: EditText
    private lateinit var buttonRegister: Button
    private lateinit var textViewLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        editTextName = findViewById(R.id.editTextName)
        editTextEmailRegister = findViewById(R.id.editTextEmailRegister)
        editTextPasswordRegister = findViewById(R.id.editTextPasswordRegister)
        buttonRegister = findViewById(R.id.buttonRegister)
        textViewLoginLink = findViewById(R.id.textViewLoginLink)

        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val email = editTextEmailRegister.text.toString().trim()
            val password = editTextPasswordRegister.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Registration success, store additional user details in Firestore
                        val user = auth.currentUser
                        val userDetails = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "profilePic" to "" // You can add logic for profile picture later
                        )

                        if (user != null) {
                            firestore.collection("users").document(user.uid)
                                .set(userDetails)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    // Redirect to login activity or main activity
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error storing user details: ${e.message}", Toast.LENGTH_SHORT).show()
                                    // Optionally, you might want to delete the Firebase Auth user if Firestore fails
                                    user.delete()
                                }
                        }
                    } else {
                        // If registration fails, display a message to the user.
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        textViewLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}