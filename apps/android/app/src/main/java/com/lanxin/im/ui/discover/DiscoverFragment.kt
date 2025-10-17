package com.lanxin.im.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lanxin.im.R

/**
 * 发现Fragment - 朋友圈、扫一扫等（按设计文档实现）
 */
class DiscoverFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners(view)
    }
    
    private fun setupClickListeners(view: View) {
        // 朋友圈（功能待后续版本）
        view.findViewById<View>(R.id.btn_moments).setOnClickListener {
            // 功能待实现
        }
        
        // 扫一扫（完整实现，跳转到扫一扫页面）
        view.findViewById<View>(R.id.btn_scan).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.social.ScanQRCodeActivity::class.java)
            startActivity(intent)
        }
    }
}

