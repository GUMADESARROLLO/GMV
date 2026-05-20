package com.unimarksa.gmv.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ViewPagerAdapter

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        val navigation = findViewById<BottomNavigationView>(R.id.navigation)

        viewPager.adapter = ViewPagerAdapter(supportFragmentManager)

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                navigation.menu.getItem(position).isChecked = true
            }
        })

        navigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.articulosFragment -> viewPager.currentItem = 0
                R.id.clientesFragment -> viewPager.currentItem = 1
                R.id.perfilFragment -> viewPager.currentItem = 2
            }
            true
        }
    }
}
