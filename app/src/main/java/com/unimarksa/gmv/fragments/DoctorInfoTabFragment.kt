package com.unimarksa.gmv.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.tools.FakeMedicalRepository

class DoctorInfoTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val doctorId = arguments?.getString("doctorId") ?: return null
        val doctor = FakeMedicalRepository.getDoctorById(doctorId) ?: return null
        return buildInfoView(doctor)
    }

    private fun buildInfoView(doctor: Doctor): ScrollView {
        val ctx = requireContext()
        val scroll = ScrollView(ctx)
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val card = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }

        addInfoRow(card, "Nombre", doctor.nombre)
        addInfoRow(card, "Especialidad", doctor.especialidad)

        if (doctor.telefono.isNotEmpty()) {
            val phoneRow = addInfoRow(card, "Teléfono", doctor.telefono, clickable = true)
            phoneRow.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.telefono}")))
            }
        }

        if (doctor.email.isNotEmpty()) {
            val emailRow = addInfoRow(card, "Email", doctor.email, clickable = true)
            emailRow.setOnClickListener {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${doctor.email}")))
            }
        }

        layout.addView(card)

        val visits = FakeMedicalRepository.getAllVisits().filter { it.doctorId == doctor.id }.take(3)
        if (visits.isNotEmpty()) {
            layout.addView(android.widget.TextView(ctx).apply {
                text = "Últimas visitas"
                setTextColor(resources.getColor(R.color.text_label, null))
                textSize = 12f
                setPadding(0, 24, 0, 8)
            })

            visits.forEach { visit ->
                layout.addView(android.widget.TextView(ctx).apply {
                    text = "${visit.fecha} - ${if (visit.efectiva) "Efectiva" else "No efectiva"}"
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        scroll.addView(layout)
        return scroll
    }

    private fun addInfoRow(
        parent: android.widget.LinearLayout,
        label: String,
        value: String,
        clickable: Boolean = false
    ): android.widget.LinearLayout {
        val ctx = parent.context
        val row = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        row.addView(android.widget.TextView(ctx).apply {
            text = label
            textSize = 12f
            setTextColor(resources.getColor(R.color.text_label, null))
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
        })

        row.addView(android.widget.TextView(ctx).apply {
            text = value
            textSize = 14f
            setTextColor(if (clickable) resources.getColor(R.color.accent_primary, null) else resources.getColor(R.color.text_primary, null))
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
        })

        parent.addView(row)
        return row
    }
}
