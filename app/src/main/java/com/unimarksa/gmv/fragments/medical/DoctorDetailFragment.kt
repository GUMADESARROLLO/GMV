package com.unimarksa.gmv.fragments.medical

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.model.Doctor
import com.unimarksa.gmv.data.model.DoctorStatus
import com.unimarksa.gmv.data.repository.FakeMedicalRepository

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

        val initials = doctor.nombre.replace(Regex("^(Dr|Dra)\\.?\\s*"), "")
            .split(" ").filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

        view.findViewById<TextView>(R.id.tv_avatar).text = initials
        view.findViewById<TextView>(R.id.tv_nombre).text = doctor.nombre
        view.findViewById<TextView>(R.id.tv_especialidad).text = doctor.especialidad

        val tvEstado = view.findViewById<TextView>(R.id.tv_estado)
        tvEstado.text = doctor.estado.label
        tvEstado.background = when (doctor.estado) {
            DoctorStatus.ACTIVO -> resources.getDrawable(R.drawable.bg_badge_solid_green, null)
            DoctorStatus.PENDIENTE_ALTA -> resources.getDrawable(R.drawable.bg_badge_solid_amber, null)
            DoctorStatus.PENDIENTE_BAJA -> resources.getDrawable(R.drawable.bg_badge_solid_red, null)
        }

        val headerColor = getSpecialtyColor(doctor.especialidad)
        view.findViewById<View>(R.id.view_header_bg).setBackgroundColor(headerColor)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DoctorInfoTabFragment().apply {
                        arguments = Bundle().apply { putString("doctorId", doctor.id) }
                    }
                    else -> DoctorProductsTabFragment()
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

    private fun getSpecialtyColor(especialidad: String): Int {
        return when (especialidad.lowercase()) {
            "cardiología", "cardiologia" -> Color.parseColor("#EF4444")
            "neurología", "neurologia" -> Color.parseColor("#8B5CF6")
            "pediatría", "pediatria" -> Color.parseColor("#22C55E")
            "oncología", "oncologia" -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#4361EE")
        }
    }
}
