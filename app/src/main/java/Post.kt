data class Post(
    val imageUrl: String = "",
    val caption: String = "",
    val userId: String = "",
    val timestamp: String = "",
    val imageString: String = "", // Optional: Store the base64 string as well
    var likeCount: Long = 0 // Initialize like count to 0
)
