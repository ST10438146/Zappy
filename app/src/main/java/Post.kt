data class Post(
    val imageUrl: String = "",
    val caption: String = "",
    val userId: String = "",
    val timestamp: String = "",
    val imageString: String = "", // Stores the base64 string as well
    var likeCount: Long = 0 ,// Initializes like count to 0
    val postId: String? = null, // This is the document ID, for updates
    val tags: List<String> = emptyList()
)
