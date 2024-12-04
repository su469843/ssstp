class Socks5Server {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start(port: Int) {
        serverSocket = ServerSocket(port)
        isRunning = true
        
        while (isRunning) {
            try {
                val socket = serverSocket?.accept() ?: continue
                Thread { handleConnection(socket) }.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleConnection(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // SOCKS5认证阶段
            handleAuthentication(input, output)
            
            // SOCKS5请求阶段
            val request = handleRequest(input, output)
            
            // 建立目标连接
            val targetSocket = createTargetConnection(request)
            
            // 开始转发数据
            forwardData(socket, targetSocket)
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleAuthentication(input: InputStream, output: OutputStream) {
        // 读取客户端支持的认证方法
        val version = input.read()
        val methodCount = input.read()
        val methods = ByteArray(methodCount)
        input.read(methods)

        // 回复使用无认证方法
        output.write(byteArrayOf(0x05, 0x00))
    }

    private fun handleRequest(input: InputStream, output: OutputStream): SocksRequest {
        val version = input.read()
        val command = input.read()
        val reserved = input.read()
        val addressType = input.read()

        val targetAddress = when (addressType) {
            0x01 -> readIPv4Address(input)
            0x03 -> readDomainAddress(input)
            0x04 -> readIPv6Address(input)
            else -> throw Exception("Unsupported address type")
        }

        val targetPort = (input.read() shl 8) or input.read()

        return SocksRequest(command, targetAddress, targetPort)
    }

    private fun createTargetConnection(request: SocksRequest): Socket {
        val targetSocket = Socket(request.address, request.port)
        return targetSocket
    }

    private fun forwardData(clientSocket: Socket, targetSocket: Socket) {
        val clientToTarget = Thread {
            try {
                val buffer = ByteArray(4096)
                val input = clientSocket.getInputStream()
                val output = targetSocket.getOutputStream()
                
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    output.write(buffer, 0, length)
                    output.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val targetToClient = Thread {
            try {
                val buffer = ByteArray(4096)
                val input = targetSocket.getInputStream()
                val output = clientSocket.getOutputStream()
                
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    output.write(buffer, 0, length)
                    output.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        clientToTarget.start()
        targetToClient.start()
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
    }

    private fun readIPv4Address(input: InputStream): String {
        val addr = ByteArray(4)
        input.read(addr)
        return InetAddress.getByAddress(addr).hostAddress
    }

    private fun readDomainAddress(input: InputStream): String {
        val length = input.read()
        val addr = ByteArray(length)
        input.read(addr)
        return String(addr)
    }

    private fun readIPv6Address(input: InputStream): String {
        val addr = ByteArray(16)
        input.read(addr)
        return InetAddress.getByAddress(addr).hostAddress
    }
}

data class SocksRequest(
    val command: Int,
    val address: String,
    val port: Int
) 