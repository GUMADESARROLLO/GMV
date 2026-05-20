package com.unimarksa.gmv.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ViewPagerAdapter

class DashboardActivity : AppCompatActivity() {

    companion object {
        private const val COLOR_ACTIVE = 0
        private const val COLOR_INACTIVE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager)

        val navArticulos = findViewById<android.widget.LinearLayout>(R.id.nav_articulos)
        val navClientes = findViewById<android.widget.LinearLayout>(R.id.nav_clientes)
        val navPerfil = findViewById<android.widget.LinearLayout>(R.id.nav_perfil)
        val ivArticulos = findViewById<android.widget.ImageView>(R.id.iv_nav_articulos)
        val ivClientes = findViewById<android.widget.ImageView>(R.id.iv_nav_clientes)
        val ivPerfil = findViewById<android.widget.ImageView>(R.id.iv_nav_perfil)
        val tvArticulos = findViewById<android.widget.TextView>(R.id.tv_nav_articulos_title)
        val tvClientes = findViewById<android.widget.TextView>(R.id.tv_nav_clientes_title)
        val tvPerfil = findViewById<android.widget.TextView>(R.id.tv_nav_perfil_title)

        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.grey_40)

        fun setActive(tab: Int) {
            val views = listOf(
                listOf(ivArticulos, tvArticulos),
                listOf(ivClientes, tvClientes),
                listOf(ivPerfil, tvPerfil)
            )
            views.forEachIndexed { i, v ->
                val tint = if (i == tab) activeColor else inactiveColor
                (v[0] as android.widget.ImageView).setColorFilter(tint)
                (v[1] as android.widget.TextView).setTextColor(tint)
            }
        }

        navArticulos.setOnClickListener { viewPager.currentItem = 0 }
        navClientes.setOnClickListener { viewPager.currentItem = 1 }
        navPerfil.setOnClickListener { viewPager.currentItem = 2 }

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                setActive(position)
            }
        })

        setActive(0)
    }
}
