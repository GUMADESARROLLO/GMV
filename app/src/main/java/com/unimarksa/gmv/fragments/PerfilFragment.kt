package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R

class PerfilFragment : Fragment() {

    private lateinit var nestedScroll: NestedScrollView
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
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nestedScroll = view.findViewById(R.id.nested_content)
        floatingIsland = view.findViewById(R.id.floating_island)
        tvIslandText = floatingIsland.findViewById(R.id.tv_island_text)
        ivIslandIcon = floatingIsland.findViewById(R.id.iv_island_icon)

        tvIslandText.text = "Perfil"
        ivIslandIcon.setImageResource(R.drawable.ic_person)

        nestedScroll.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val floatingNav = activity?.findViewById<View>(R.id.floating_nav)
            if (scrollY > oldScrollY && !isIslandHidden) {
                floatingIsland.animate().translationY(200f).duration = 300
                isIslandHidden = true
                floatingNav?.animate()?.translationY(200f)?.duration = 300
                isNavHidden = true
            } else if (scrollY < oldScrollY && isIslandHidden) {
                floatingIsland.animate().translationY(0f).duration = 300
                isIslandHidden = false
                floatingNav?.animate()?.translationY(0f)?.duration = 300
                isNavHidden = false
            }
        }

        floatingIsland.setOnClickListener {
            nestedScroll.smoothScrollTo(0, 0)
            floatingIsland.animate().translationY(0f).duration = 300
            isIslandHidden = false
        }
    }
}
