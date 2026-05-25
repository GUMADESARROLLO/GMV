package com.unimarksa.gmv.fragments.medical

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.medical.ProductAdapter
import com.unimarksa.gmv.data.repository.FakeMedicalRepository

class DoctorProductsTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ctx = requireContext()
        val rv = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, 2)
            setPadding(12, 12, 12, 12)
            clipToPadding = false
        }

        val productAdapter = ProductAdapter { }
        rv.adapter = productAdapter
        productAdapter.submitList(FakeMedicalRepository.getAllProducts())

        return rv
    }
}
