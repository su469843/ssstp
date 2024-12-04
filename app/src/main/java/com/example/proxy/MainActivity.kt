class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var vpnService: ProxyVpnService? = null
    private val proxyAdapter = ProxyAdapter()
    private var currentProxyConfig: ProxyConfig? = null
    private var isProxyRunning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        
        binding.btnAdd.setOnClickListener {
            showAddProxyDialog()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)
        }
    }
    
    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_start_proxy -> {
                    if (!isProxyRunning) {
                        currentProxyConfig?.let { config ->
                            startVpnService(config)
                            menuItem.title = "停止代理"
                            menuItem.setIcon(android.R.drawable.ic_media_pause)
                            isProxyRunning = true
                        } ?: run {
                            Toast.makeText(this, "请先选择一个代理服务器", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        stopVpnService()
                        menuItem.title = "开始代理"
                        menuItem.setIcon(android.R.drawable.ic_media_play)
                        isProxyRunning = false
                    }
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_about -> {
                    showAboutDialog()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_exit -> {
                    exitApp()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("代理工具 v1.0\n\n作者邮箱：hanghangjason@qq.com")
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun exitApp() {
        AlertDialog.Builder(this)
            .setTitle("退出")
            .setMessage("确定要退出应用吗？")
            .setPositiveButton("确定") { _, _ ->
                if (isProxyRunning) {
                    stopVpnService()
                }
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        binding.proxyList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = proxyAdapter
        }
        
        proxyAdapter.setOnItemClickListener { proxyConfig ->
            showEditProxyDialog(proxyConfig)
        }
    }
    
    private fun showEditProxyDialog(proxyConfig: ProxyConfig) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_proxy, null)
        
        // 填充现有数据
        dialogView.findViewById<EditText>(R.id.etServer).setText(proxyConfig.serverAddress)
        dialogView.findViewById<EditText>(R.id.etPort).setText(proxyConfig.port.toString())
        dialogView.findViewById<EditText>(R.id.etUsername).setText(proxyConfig.username)
        dialogView.findViewById<EditText>(R.id.etPassword).setText(proxyConfig.password)
        
        AlertDialog.Builder(this)
            .setTitle("编辑代理服务器")
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                val server = dialogView.findViewById<EditText>(R.id.etServer).text.toString()
                val port = dialogView.findViewById<EditText>(R.id.etPort).text.toString().toIntOrNull() ?: 1080
                val username = dialogView.findViewById<EditText>(R.id.etUsername).text.toString()
                val password = dialogView.findViewById<EditText>(R.id.etPassword).text.toString()
                
                if (server.isNotEmpty()) {
                    val updatedConfig = ProxyConfig(
                        id = proxyConfig.id,
                        serverAddress = server,
                        port = port,
                        username = username,
                        password = password
                    )
                    
                    // 如果是当前选中的代理，更新currentProxyConfig
                    if (currentProxyConfig?.id == proxyConfig.id) {
                        currentProxyConfig = updatedConfig
                    }
                    
                    // 更新列表中的数据
                    val position = proxyAdapter.getPosition(proxyConfig)
                    if (position != -1) {
                        proxyAdapter.updateProxy(position, updatedConfig)
                    }
                } else {
                    Toast.makeText(this, "请输入代理服务器地址", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showAddProxyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_proxy, null)
        
        AlertDialog.Builder(this)
            .setTitle("添加代理服务器")
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                val server = dialogView.findViewById<EditText>(R.id.etServer).text.toString()
                val port = dialogView.findViewById<EditText>(R.id.etPort).text.toString().toIntOrNull() ?: 1080
                val username = dialogView.findViewById<EditText>(R.id.etUsername).text.toString()
                val password = dialogView.findViewById<EditText>(R.id.etPassword).text.toString()
                
                if (server.isNotEmpty()) {
                    val proxyConfig = ProxyConfig(
                        serverAddress = server,
                        port = port,
                        username = username,
                        password = password
                    )
                    proxyAdapter.addProxy(proxyConfig)
                    
                    // 返回桌面
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "请输入代理服务器地址", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun startVpnService(proxyConfig: ProxyConfig) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
        } else {
            onVpnPermissionGranted(proxyConfig)
        }
    }
    
    private fun onVpnPermissionGranted(proxyConfig: ProxyConfig) {
        val serviceIntent = Intent(this, ProxyVpnService::class.java).apply {
            putExtra("server", proxyConfig.serverAddress)
            putExtra("port", proxyConfig.port)
        }
        startService(serviceIntent)
    }
    
    private fun stopVpnService() {
        val serviceIntent = Intent(this, ProxyVpnService::class.java)
        stopService(serviceIntent)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_PERMISSION && resultCode == RESULT_OK) {
            currentProxyConfig?.let { onVpnPermissionGranted(it) }
        } else {
            isProxyRunning = false
            val startProxyItem = binding.navView.menu.findItem(R.id.nav_start_proxy)
            startProxyItem.title = "开始代理"
            startProxyItem.setIcon(android.R.drawable.ic_media_play)
        }
    }
    
    companion object {
        private const val REQUEST_VPN_PERMISSION = 1
    }
} 
} 