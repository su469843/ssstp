class ProxyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val socksServer = Socks5Server()
    
    override fun onCreate() {
        super.onCreate()
        setupVpn()
    }
    
    private fun setupVpn() {
        vpnInterface = Builder()
            .setSession("ProxyVPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .establish()
            
        startProxyThread()
    }
    
    private fun startProxyThread() {
        isRunning = true
        Thread {
            try {
                socksServer.start(1080) // SOCKS5默认端口1080
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        socksServer.stop()
        vpnInterface?.close()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val server = it.getStringExtra("server") ?: return START_NOT_STICKY
            val port = it.getIntExtra("port", 1080)
            socksServer.setTargetServer(server, port)
        }
        return super.onStartCommand(intent, flags, startId)
    }
} 