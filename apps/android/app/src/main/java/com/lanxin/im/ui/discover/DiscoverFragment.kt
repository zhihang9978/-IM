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
        view.findViewById<View>(R.id.btn_moments)?.setOnClickListener {
            Toast.makeText(requireContext(), "朋友圈功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_scan)?.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.lanxin.im.ui.social.ScanQRCodeActivity::class.java)
            startActivity(intent)
        }
        
        view.findViewById<View>(R.id.btn_shake)?.setOnClickListener {
            Toast.makeText(requireContext(), "摇一摇功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_top_stories)?.setOnClickListener {
            Toast.makeText(requireContext(), "看一看功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_search)?.setOnClickListener {
            Toast.makeText(requireContext(), "搜一搜功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_live)?.setOnClickListener {
            Toast.makeText(requireContext(), "直播和附近功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_shopping)?.setOnClickListener {
            Toast.makeText(requireContext(), "购物功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_games)?.setOnClickListener {
            Toast.makeText(requireContext(), "游戏功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btn_mini_program)?.setOnClickListener {
            Toast.makeText(requireContext(), "小程序功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
}

