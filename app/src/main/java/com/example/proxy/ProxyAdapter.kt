class ProxyAdapter : RecyclerView.Adapter<ProxyAdapter.ProxyViewHolder>() {
    private val proxyList = mutableListOf<ProxyConfig>()
    private var onItemClickListener: ((ProxyConfig) -> Unit)? = null
    
    fun setOnItemClickListener(listener: (ProxyConfig) -> Unit) {
        onItemClickListener = listener
    }
    
    fun addProxy(proxy: ProxyConfig) {
        proxyList.add(proxy)
        notifyItemInserted(proxyList.size - 1)
    }
    
    fun updateProxy(position: Int, proxy: ProxyConfig) {
        proxyList[position] = proxy
        notifyItemChanged(position)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProxyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ProxyViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProxyViewHolder, position: Int) {
        holder.bind(proxyList[position], position)
    }
    
    override fun getItemCount() = proxyList.size
    
    fun getPosition(proxy: ProxyConfig): Int {
        return proxyList.indexOfFirst { it.id == proxy.id }
    }
    
    inner class ProxyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        
        init {
            itemView.setOnClickListener {
                onItemClickListener?.invoke(proxyList[adapterPosition])
            }
        }
        
        fun bind(proxy: ProxyConfig, position: Int) {
            text1.text = "${proxy.serverAddress}:${proxy.port}"
        }
    }
} 