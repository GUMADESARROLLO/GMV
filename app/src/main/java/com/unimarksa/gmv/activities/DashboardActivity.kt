package com.unimarksa.gmv.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ViewPagerAdapter
import com.unimarksa.gmv.tools.Searchable

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var lytSearchMode: View
    private lateinit var etToolbarSearch: EditText
    private lateinit var btnSearchClear: ImageButton
    private lateinit var btnSearchIcon: ImageButton
    private lateinit var floatingNav: View
    private var isSearchOpen = false
    private var isNavHidden = false
    private var currentScrollListener: RecyclerView.OnScrollListener? = null
    private var currentScrollableView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        viewPager = findViewById(R.id.viewpager)
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager)

        toolbar = findViewById(R.id.toolbar)
        lytSearchMode = findViewById(R.id.lyt_search_mode)
        etToolbarSearch = findViewById(R.id.et_toolbar_search)
        btnSearchClear = findViewById(R.id.btn_search_clear)
        btnSearchIcon = findViewById(R.id.btn_toolbar_search)

        floatingNav = findViewById(R.id.floating_nav)

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
            btnSearchIcon.visibility = if (tab == 2) View.GONE else View.VISIBLE
        }

        navArticulos.setOnClickListener { viewPager.currentItem = 0 }
        navClientes.setOnClickListener { viewPager.currentItem = 1 }
        navPerfil.setOnClickListener { viewPager.currentItem = 2 }

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                setActive(position)
                if (isSearchOpen) closeSearch()
                attachScrollBehavior(position)
            }
        })

        btnSearchIcon.setOnClickListener { openSearch() }

        findViewById<ImageButton>(R.id.btn_search_back).setOnClickListener { closeSearch() }

        btnSearchClear.setOnClickListener {
            etToolbarSearch.setText("")
            notifySearch("")
        }

        etToolbarSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                btnSearchClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                notifySearch(q)
            }
        })

        setActive(0)
        viewPager.post { attachScrollBehavior(0) }
    }

    private fun openSearch() {
        isSearchOpen = true
        toolbar.title = ""
        btnSearchIcon.visibility = View.GONE
        lytSearchMode.visibility = View.VISIBLE
        etToolbarSearch.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.showSoftInput(etToolbarSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        isSearchOpen = false
        etToolbarSearch.setText("")
        lytSearchMode.visibility = View.GONE
        btnSearchIcon.visibility = View.VISIBLE
        toolbar.title = getString(R.string.app_name)
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(etToolbarSearch.windowToken, 0)
        notifySearch("")
    }

    private fun attachScrollBehavior(position: Int) {
        currentScrollListener?.let {
            (currentScrollableView as? RecyclerView)?.removeOnScrollListener(it)
        }
        currentScrollListener = null
        currentScrollableView = null
        isNavHidden = false
        floatingNav.translationY = 0f

        val adapter = viewPager.adapter as ViewPagerAdapter
        val fragment = adapter.getItem(position)
        val view = fragment.view ?: return

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        if (recyclerView != null) {
            val listener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) hideFloatingNav() else showFloatingNav()
                }
            }
            recyclerView.addOnScrollListener(listener)
            currentScrollListener = listener
            currentScrollableView = recyclerView
            return
        }

        val nestedScroll = view.findViewById<NestedScrollView>(R.id.nested_content)
        if (nestedScroll != null) {
            nestedScroll.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY) hideFloatingNav() else showFloatingNav()
            }
            currentScrollableView = nestedScroll
        }
    }

    private fun showFloatingNav() {
        if (!isNavHidden) return
        isNavHidden = false
        floatingNav.animate().translationY(0f).setDuration(200).start()
    }

    private fun hideFloatingNav() {
        if (isNavHidden) return
        isNavHidden = true
        floatingNav.post {
            val offset = floatingNav.height + 16
            floatingNav.animate().translationY(offset.toFloat()).setDuration(200).start()
        }
    }

    private fun notifySearch(query: String) {
        val adapter = viewPager.adapter as ViewPagerAdapter
        val fragment = adapter.getItem(viewPager.currentItem)
        (fragment as? Searchable)?.onSearchQuery(query)
    }
}
