package com.unimarksa.gmv.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.models.DoctorStatus
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
        val dp = ctx.resources.displayMetrics.density
        val scroll = ScrollView(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        root.addView(profileCard(ctx, doctor, dp))
        contactCard(ctx, doctor, dp)?.let { root.addView(it) }
        visitsCard(ctx, doctor, dp)?.let { root.addView(it) }

        scroll.addView(root)
        return scroll
    }

    private fun profileCard(ctx: android.content.Context, doctor: Doctor, dp: Float): MaterialCardView {
        val card = cardFrame(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        val avatarColors = listOf("#4361EE", "#22C55E", "#EF4444", "#F59E0B", "#8B5CF6", "#06B6D4")
        val avatarColor = Color.parseColor(avatarColors[(doctor.nombre.hashCode() and Int.MAX_VALUE) % avatarColors.size])

        val initials = doctor.nombre.replace(Regex("^(Dr|Dra)\\.?\\s*"), "")
            .split(" ").filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }

        val tvAvatar = TextView(ctx).apply {
            text = initials
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            val size = (64 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(avatarColor)
            }
        }
        col.addView(tvAvatar)

        val tvName = TextView(ctx).apply {
            text = doctor.nombre
            textSize = 18f
            setTextColor(ctx.resources.getColor(R.color.text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * dp).toInt() }
        }
        col.addView(tvName)

        val tvSpec = TextView(ctx).apply {
            text = doctor.especialidad
            textSize = 14f
            setTextColor(ctx.resources.getColor(R.color.text_label, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        }
        col.addView(tvSpec)

        val tvStatus = TextView(ctx).apply {
            text = doctor.estado.label
            textSize = 11f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (4 * dp).toInt(), (12 * dp).toInt(), (4 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * dp).toInt() }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = (6 * dp).toFloat()
                setColor(ctx.resources.getColor(
                    when (doctor.estado) {
                        DoctorStatus.ACTIVO -> android.R.color.holo_green_dark
                        DoctorStatus.PENDIENTE_ALTA -> android.R.color.holo_orange_dark
                        DoctorStatus.PENDIENTE_BAJA -> android.R.color.holo_red_dark
                    }, null
                ))
            }
        }
        col.addView(tvStatus)

        card.addView(col)
        return card
    }

    private fun contactCard(ctx: android.content.Context, doctor: Doctor, dp: Float): MaterialCardView? {
        if (doctor.telefono.isEmpty() && doctor.email.isEmpty()) return null
        val card = cardFrame(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
        }

        val tvTitle = TextView(ctx).apply {
            text = "Contacto"
            textSize = 13f
            setTextColor(ctx.resources.getColor(R.color.text_label, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitle)

        if (doctor.telefono.isNotEmpty()) {
            val phoneRow = infoRow(ctx, "Teléfono", doctor.telefono, true, dp)
            phoneRow.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.telefono}")))
            }
            col.addView(phoneRow)
        }

        if (doctor.email.isNotEmpty()) {
            val emailRow = infoRow(ctx, "Email", doctor.email, true, dp)
            emailRow.setOnClickListener {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${doctor.email}")))
            }
            col.addView(emailRow)
        }

        card.addView(col)
        return card
    }

    private fun visitsCard(ctx: android.content.Context, doctor: Doctor, dp: Float): MaterialCardView? {
        val visits = FakeMedicalRepository.getAllVisits().filter { it.doctorId == doctor.id }.take(3)
        if (visits.isEmpty()) return null
        val card = cardFrame(ctx)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
        }

        val tvTitle = TextView(ctx).apply {
            text = "Últimas visitas"
            textSize = 13f
            setTextColor(ctx.resources.getColor(R.color.text_label, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitle)

        visits.forEach { visit ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
            }
            row.addView(TextView(ctx).apply {
                text = visit.fecha
                textSize = 13f
                setTextColor(ctx.resources.getColor(R.color.text_primary, null))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(ctx).apply {
                text = if (visit.efectiva) "Efectiva" else "No efectiva"
                textSize = 11f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding((8 * dp).toInt(), (2 * dp).toInt(), (8 * dp).toInt(), (2 * dp).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = (4 * dp).toFloat()
                    setColor(ctx.resources.getColor(
                        if (visit.efectiva) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                        null
                    ))
                }
            })
            col.addView(row)
        }

        card.addView(col)
        return card
    }

    private fun cardFrame(ctx: android.content.Context): MaterialCardView {
        val dp = ctx.resources.displayMetrics.density
        return MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
            radius = (12 * dp).toFloat()
            cardElevation = 0f
            setContentPadding(0, 0, 0, 0)
        }
    }

    private fun infoRow(
        ctx: android.content.Context,
        label: String,
        value: String,
        clickable: Boolean,
        dp: Float
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * dp).toInt(), 0, 0)
        }
        row.addView(TextView(ctx).apply {
            text = label
            textSize = 13f
            setTextColor(ctx.resources.getColor(R.color.text_label, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f)
        })
        row.addView(TextView(ctx).apply {
            text = value
            textSize = 14f
            setTextColor(ctx.resources.getColor(
                if (clickable) R.color.accent_primary else R.color.text_primary, null
            ))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f)
        })
        return row
    }
}
