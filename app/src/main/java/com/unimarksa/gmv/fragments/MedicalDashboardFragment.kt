package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.FakeMedicalRepository
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

        setupHeader(view)
        loadMetrics(view)
        setupActionCards(view)
    }

    private fun setupHeader(view: View) {
        view.findViewById<TextView>(R.id.tv_name).text = "Visitador Médico"
        view.findViewById<TextView>(R.id.tv_zone).text = "Zona Central"

        val today = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date())
        view.findViewById<TextView>(R.id.tv_date).text = today.uppercase()
    }

    private fun loadMetrics(view: View) {
        val stats = FakeMedicalRepository.getTodayStats()
        val totalDoctors = stats["total_doctors"] ?: 0
        val visitsDone = stats["visits_done"] ?: 0
        val pending = stats["visits_not_effective"] ?: 0

        view.findViewById<TextView>(R.id.tv_metric_doctors).text = totalDoctors.toString()
        view.findViewById<TextView>(R.id.tv_metric_visits).text = visitsDone.toString()
        view.findViewById<TextView>(R.id.tv_metric_pending).text = pending.toString()
    }

    private fun setupActionCards(view: View) {
        configureCard(
            view, R.id.card_mis_medicos,
            R.drawable.ic_person, R.drawable.bg_icon_purple, R.color.purple_700,
            "Mis médicos", "12 registrados"
        ) {
            findNavController().navigate(R.id.doctorListFragment)
        }

        configureCard(
            view, R.id.card_nuevo_medico,
            R.drawable.ic_add, R.drawable.bg_icon_green, R.color.green_700,
            "Nuevo médico", "Agregar registro"
        ) {
            AddDoctorBottomSheet().show(childFragmentManager, "AddDoctor")
        }

        configureCard(
            view, R.id.card_productos,
            R.drawable.ic_premium, R.drawable.bg_icon_amber, R.color.amber_700,
            "Productos", "Catálogo activo"
        ) {
            findNavController().navigate(R.id.productCatalogFragment)
        }

        configureCard(
            view, R.id.card_mis_visitas,
            R.drawable.ic_history, R.drawable.bg_icon_blue, R.color.blue_700,
            "Mis visitas", "5 realizadas hoy"
        ) {
            findNavController().navigate(R.id.visitHistoryFragment)
        }
    }

    private fun configureCard(
        view: View,
        cardId: Int,
        iconRes: Int,
        iconBgRes: Int,
        iconTintRes: Int,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        val card = view.findViewById<View>(cardId)
        card.findViewById<ImageView>(R.id.iv_action_icon).apply {
            setImageResource(iconRes)
            background = ContextCompat.getDrawable(context, iconBgRes)
            imageTintList = ContextCompat.getColorStateList(context, iconTintRes)
        }
        card.findViewById<TextView>(R.id.tv_action_title).text = title
        card.findViewById<TextView>(R.id.tv_action_subtitle).text = subtitle
        card.setOnClickListener { onClick() }
    }
}
