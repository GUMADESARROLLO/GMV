package com.unimarksa.gmv.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.unimarksa.gmv.fragments.ArticulosFragment
import com.unimarksa.gmv.fragments.ClientesFragment
import com.unimarksa.gmv.fragments.PerfilFragment

class ViewPagerAdapter(fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments: List<Fragment> = listOf(
        ArticulosFragment(),
        ClientesFragment(),
        PerfilFragment()
    )

    override fun getItem(position: Int): Fragment = fragments[position]
    override fun getCount(): Int = fragments.size
}
