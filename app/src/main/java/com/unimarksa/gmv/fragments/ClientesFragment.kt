package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ClientesAdapter
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.tools.ApiService

class ClientesFragment : Fragment() {

    companion object {
        private var cachedClientes: List<ItemCliente>? = null
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingIsland: View
    private lateinit var tvIslandText: TextView
    private lateinit var ivIslandIcon: ImageView
    private var isIslandHidden = false
    private var isNavHidden = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerView = view.findViewById(R.id.recycler_view)
        floatingIsland = view.findViewById(R.id.floating_island)
        tvIslandText = floatingIsland.findViewById(R.id.tv_island_text)
        ivIslandIcon = floatingIsland.findViewById(R.id.iv_island_icon)

        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefresh.setOnRefreshListener {
            cachedClientes = null
            cargarClientes()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val floatingNav = activity?.findViewById<View>(R.id.floating_nav)
                if (dy > 5 && !isIslandHidden) {
                    floatingIsland.animate().translationY(200f).duration = 300
                    isIslandHidden = true
                    floatingNav?.animate()?.translationY(200f)?.duration = 300
                    isNavHidden = true
                } else if (dy < -5 && isIslandHidden) {
                    floatingIsland.animate().translationY(0f).duration = 300
                    isIslandHidden = false
                    floatingNav?.animate()?.translationY(0f)?.duration = 300
                    isNavHidden = false
                }
            }

            override fun onScrollStateChanged(rv: RecyclerView, state: Int) {
                if (!rv.canScrollVertically(-1)) {
                    val floatingNav = activity?.findViewById<View>(R.id.floating_nav)
                    if (isIslandHidden) {
                        floatingIsland.animate().translationY(0f).duration = 300
                        isIslandHidden = false
                    }
                    if (isNavHidden) {
                        floatingNav?.animate()?.translationY(0f)?.duration = 300
                        isNavHidden = false
                    }
                }
            }
        })

        floatingIsland.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
            floatingIsland.animate().translationY(0f).duration = 300
            isIslandHidden = false
        }

        if (cachedClientes == null) {
            cargarClientes()
        } else {
            mostrarClientes(cachedClientes!!)
        }
    }

    private fun cargarClientes() {
        swipeRefresh.isRefreshing = true
        ApiService.fetchClientes(
            onResult = { clientes ->
                cachedClientes = clientes
                mostrarClientes(clientes)
                swipeRefresh.isRefreshing = false
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                swipeRefresh.isRefreshing = false
            }
        )
    }

    private fun mostrarClientes(clientes: List<ItemCliente>) {
        recyclerView.adapter = ClientesAdapter(clientes, requireContext())
        tvIslandText.text = "${clientes.size} clientes"
        ivIslandIcon.setImageResource(R.drawable.ic_people)
        floatingIsland.animate().translationY(0f).duration = 300
        isIslandHidden = false
    }
}
