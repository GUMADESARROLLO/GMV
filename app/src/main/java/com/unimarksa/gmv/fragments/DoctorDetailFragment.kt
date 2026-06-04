package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.tools.FakeMedicalRepository

class DoctorDetailFragment : Fragment() {

    private var doctorId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doctorId = arguments?.getString("doctorId")
        val doctor = FakeMedicalRepository.getDoctorById(doctorId ?: "") ?: return

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.inflateMenu(R.menu.menu_doctor_detail)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DoctorInfoTabFragment().apply {
                        arguments = Bundle().apply { putString("doctorId", doctor.id) }
                    }
                    else -> DoctorProductsTabFragment().apply {
                        arguments = Bundle().apply { putString("doctorId", doctor.id) }
                    }
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Información" else "Productos"
        }.attach()

        view.findViewById<FloatingActionButton>(R.id.fab_registrar_visita).setOnClickListener {
            RegisterVisitBottomSheet.newInstance(doctor).show(childFragmentManager, "RegisterVisit")
        }

        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_baja) {
                showDowngradeDialog(doctor)
                true
            } else false
        }
    }

    private fun showDowngradeDialog(doctor: Doctor) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Solicitar baja")
            .setMessage("¿Deseas solicitar la baja de ${doctor.nombre}? La solicitud quedará pendiente de aprobación.")
            .setPositiveButton("Confirmar") { _, _ ->
                FakeMedicalRepository.requestDoctorDowngrade(doctor.id)
                Snackbar.make(requireView(), "Solicitud de baja enviada. Pendiente de aprobación.", Snackbar.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
