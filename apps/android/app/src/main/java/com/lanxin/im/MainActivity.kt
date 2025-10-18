package com.lanxin.im

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanxin.im.data.remote.RetrofitClient
import com.lanxin.im.ui.chat.ChatListFragment
import com.lanxin.im.ui.contacts.ContactsFragment
import com.lanxin.im.ui.discover.DiscoverFragment
import com.lanxin.im.ui.profile.ProfileFragment
import com.lanxin.im.ui.search.SearchActivity
import com.lanxin.im.ui.social.AddFriendActivity
import com.lanxin.im.ui.social.ScanQRCodeActivity
import com.lanxin.im.utils.AnalyticsHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 加载保存的Token
        loadSavedToken()
        
        AnalyticsHelper.trackUserActive(this)
        
        setupToolbar()
        setupViewPager()
    }
    
    private fun loadSavedToken() {
        val sharedPrefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("auth_token", null)
        if (token != null) {
            RetrofitClient.setToken(token)
        }
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.more -> {
                showMoreMenu(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showMoreMenu(menuItem: MenuItem) {
        val anchorView = toolbar.findViewById<android.view.View>(R.id.more)
        val popup = PopupMenu(this, anchorView ?: toolbar)
        popup.menuInflater.inflate(R.menu.main_more_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.add_friend -> {
                    startActivity(Intent(this, AddFriendActivity::class.java))
                    true
                }
                R.id.scan_qrcode -> {
                    try {
                        startActivity(Intent(this, ScanQRCodeActivity::class.java))
                    } catch (e: Exception) {
                        Toast.makeText(this, "扫一扫功能暂时不可用", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.create_group -> {
                    Toast.makeText(this, "发起群聊功能开发中", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    private fun setupViewPager() {
        viewPager = findViewById(R.id.contentViewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemId = when (position) {
                    0 -> R.id.navigation_chat
                    1 -> R.id.navigation_contacts
                    2 -> R.id.navigation_discover
                    3 -> R.id.navigation_profile
                    else -> R.id.navigation_chat
                }
                bottomNav.selectedItemId = itemId
            }
        })
        
        bottomNav.setOnItemSelectedListener { item ->
            val feature = when (item.itemId) {
                R.id.navigation_chat -> {
                    viewPager.currentItem = 0
                    "tab_messages"
                }
                R.id.navigation_contacts -> {
                    viewPager.currentItem = 1
                    "tab_contacts"
                }
                R.id.navigation_discover -> {
                    viewPager.currentItem = 2
                    "tab_discover"
                }
                R.id.navigation_profile -> {
                    viewPager.currentItem = 3
                    "tab_profile"
                }
                else -> "unknown"
            }
            AnalyticsHelper.trackFeatureUsage(this, feature)
            true
        }
    }
    
    private inner class MainPagerAdapter(fragmentActivity: FragmentActivity) : 
        FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = 4
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChatListFragment()
                1 -> ContactsFragment()
                2 -> DiscoverFragment()
                3 -> ProfileFragment()
                else -> ChatListFragment()
            }
        }
    }
}

