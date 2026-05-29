package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.ProductAdapter
import com.unimarksa.gmv.tools.ApiService

class DoctorProductsTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ctx = requireContext()
        val root = FrameLayout(ctx)
        root.setPadding(12, 12, 12, 12)

        val rv = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, 2)
            clipToPadding = false
            visibility = View.GONE
        }

        val loading = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val loadingContent = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        loadingContent.addView(ProgressBar(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(100, 100)
            indeterminateTintList = ctx.resources.getColorStateList(R.color.accent_primary, null)
        })

        loadingContent.addView(TextView(ctx).apply {
            text = "Cargando productos..."
            setTextColor(ctx.resources.getColor(R.color.text_label, null))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
        })

        loading.addView(loadingContent)

        val productAdapter = ProductAdapter()
        rv.adapter = productAdapter

        ApiService.fetchProductos(
            onResult = { productos ->
                productAdapter.submitList(productos.take(25))
                loading.visibility = View.GONE
                rv.visibility = View.VISIBLE
            },
            onError = {
                loading.visibility = View.GONE
                rv.visibility = View.VISIBLE
            }
        )

        root.addView(rv)
        root.addView(loading)
        return root
    }
}
