data class ProxyConfig(
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val port: Int,
    val username: String = "",
    val password: String = ""
) 