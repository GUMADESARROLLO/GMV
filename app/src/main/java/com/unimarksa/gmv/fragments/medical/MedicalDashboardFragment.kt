package com.unimarksa.gmv.fragments.medical

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.medical.ActionTile
import com.unimarksa.gmv.adapters.medical.ActionTileAdapter
import com.unimarksa.gmv.data.repository.FakeMedicalRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicalDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_medical_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stats = FakeMedicalRepository.getTodayStats()
        val totalDoctors = stats["total_doctors"] ?: 0
        val visitsDone = stats["visits_done"] ?: 0
        val visitsEffective = stats["visits_effective"] ?: 0
        val visitsNotEffective = stats["visits_not_effective"] ?: 0

        view.findViewById<TextView>(R.id.tv_nombre_visitador).text = "Visitador Médico"
        view.findViewById<TextView>(R.id.tv_zona).text = "Zona Central"

        val today = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date())
        view.findViewById<Chip>(R.id.chip_fecha).text = today.uppercase()

        view.findViewById<TextView>(R.id.tv_total_medicos).text = totalDoctors.toString()
        view.findViewById<TextView>(R.id.tv_visitas_realizadas).text = visitsDone.toString()
        view.findViewById<TextView>(R.id.tv_visitas_efectivas).text = "$visitsEffective Efectivas"
        view.findViewById<TextView>(R.id.tv_visitas_no_efectivas).text = "$visitsNotEffective No efectiva"

        val progress = if (totalDoctors > 0) (visitsDone * 100 / totalDoctors) else 0
        view.findViewById<ProgressBar>(R.id.progress_visitas).progress = progress
        view.findViewById<TextView>(R.id.tv_progress_label).text = "$progress% completado"

        val rv = view.findViewById<RecyclerView>(R.id.rv_acciones)
        rv.layoutManager = GridLayoutManager(requireContext(), 2)
        rv.isNestedScrollingEnabled = false

        val tiles = listOf(
            ActionTile(R.drawable.ic_person, "Mis Médicos", Color.parseColor("#4361EE")) {
                findNavController().navigate(R.id.doctorListFragment)
            },
            ActionTile(R.drawable.ic_add, "Nuevo Médico", Color.parseColor("#22C55E")) {
                AddDoctorBottomSheet().show(childFragmentManager, "AddDoctor")
            },
            ActionTile(R.drawable.ic_premium, "Productos", Color.parseColor("#8B5CF6")) {
                findNavController().navigate(R.id.productCatalogFragment)
            },
            ActionTile(R.drawable.ic_history, "Mis Visitas", Color.parseColor("#F59E0B")) {
                findNavController().navigate(R.id.visitHistoryFragment)
            }
        )

        rv.adapter = ActionTileAdapter(tiles)
    }
}
