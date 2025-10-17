package com.lanxin.im

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanxin.im.utils.AnalyticsHelper

class MainActivity : AppCompatActivity() {
    
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        AnalyticsHelper.trackUserActive(this)
        
        setupNavigation()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
        
        bottomNav.setOnItemSelectedListener { item ->
            val feature = when (item.itemId) {
                R.id.messagesFragment -> "tab_messages"
                R.id.contactsFragment -> "tab_contacts"
                R.id.discoverFragment -> "tab_discover"
                R.id.profileFragment -> "tab_profile"
                else -> "unknown"
            }
            AnalyticsHelper.trackFeatureUsage(this, feature)
            navController.navigate(item.itemId)
            true
        }
    }
}

