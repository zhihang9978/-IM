package com.lanxin.im.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lanxin.im.R
import com.lanxin.im.widget.OptionItemView

/**
 * 发现Fragment - 野火IM风格UI
 */
class DiscoverFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover_new, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }
    
    private fun setupClickListeners(view: View) {
        // 朋友圈
        view.findViewById<OptionItemView>(R.id.momentOptionItemView).setOnClickListener {
            Toast.makeText(requireContext(), "朋友圈功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        // 扫一扫
        view.findViewById<OptionItemView>(R.id.scanOptionItemView).setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.social.ScanQRCodeActivity::class.java)
            startActivity(intent)
        }
        
        // 搜索
        view.findViewById<OptionItemView>(R.id.searchOptionItemView).setOnClickListener {
            val intent = Intent(requireContext(), com.lanxin.im.ui.search.SearchActivity::class.java)
            startActivity(intent)
        }
    }
}
