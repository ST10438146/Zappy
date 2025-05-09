import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Data class for a simplified user profile from Firestore
data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null
)

class UserAdapter(private val userList: MutableList<AppUser>, private val onUserClickListener: (AppUser) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(android.R.id.text1) // Uses a simple built-in layout

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // uses a built-in TextView. Consider creating item_user.xml for better UI.
        val itemView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.userNameTextView.text = currentUser.name // Displays the user's name
        holder.itemView.setOnClickListener { onUserClickListener(currentUser) }
    }

    override fun getItemCount() = userList.size

    fun submitList(newUserList: List<AppUser>) {
        userList.clear()
        userList.addAll(newUserList)
        notifyDataSetChanged()
    }
}