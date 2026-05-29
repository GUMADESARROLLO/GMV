package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.models.DoctorStatus
import com.unimarksa.gmv.models.MedicalVisit
import com.unimarksa.gmv.models.VisitResult
import com.unimarksa.gmv.tools.FakeMedicalRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RegisterVisitBottomSheet : BottomSheetDialogFragment() {

    private var doctor: Doctor? = null
    private var selectedEffective: Boolean? = null

    companion object {
        private const val ARG_DOCTOR_ID = "doctor_id"
        private const val ARG_DOCTOR_NOMBRE = "doctor_nombre"
        private const val ARG_DOCTOR_ESPECIALIDAD = "doctor_especialidad"

        fun newInstance(doctor: Doctor): RegisterVisitBottomSheet {
            return RegisterVisitBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOCTOR_ID, doctor.id)
                    putString(ARG_DOCTOR_NOMBRE, doctor.nombre)
                    putString(ARG_DOCTOR_ESPECIALIDAD, doctor.especialidad)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_register_visit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doctor = Doctor(
            id = arguments?.getString(ARG_DOCTOR_ID) ?: "",
            nombre = arguments?.getString(ARG_DOCTOR_NOMBRE) ?: "",
            especialidad = arguments?.getString(ARG_DOCTOR_ESPECIALIDAD) ?: "",
            telefono = "",
            email = "",
            estado = DoctorStatus.ACTIVO
        )

        view.findViewById<TextView>(R.id.tv_doctor_nombre).text = doctor?.nombre
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        view.findViewById<TextView>(R.id.tv_fecha).text = now

        val cardEfectiva = view.findViewById<MaterialCardView>(R.id.card_efectiva)
        val cardNoEfectiva = view.findViewById<MaterialCardView>(R.id.card_no_efectiva)
        val lytEfectiva = view.findViewById<LinearLayout>(R.id.lyt_efectiva)
        val lytNoEfectiva = view.findViewById<LinearLayout>(R.id.lyt_no_efectiva)

        cardEfectiva.setOnClickListener {
            selectedEffective = true
            cardEfectiva.strokeWidth = 4
            cardNoEfectiva.strokeWidth = 2
            lytEfectiva.visibility = View.VISIBLE
            lytNoEfectiva.visibility = View.GONE
            populateMolecules(view)
        }

        cardNoEfectiva.setOnClickListener {
            selectedEffective = false
            cardNoEfectiva.strokeWidth = 4
            cardEfectiva.strokeWidth = 2
            lytNoEfectiva.visibility = View.VISIBLE
            lytEfectiva.visibility = View.GONE
        }

        view.findViewById<RadioGroup>(R.id.rg_motivos).setOnCheckedChangeListener { _, checkedId ->
            val tilOtro = view.findViewById<TextInputLayout>(R.id.til_otro)
            tilOtro.visibility = if (checkedId == R.id.rb_otro) View.VISIBLE else View.GONE
        }

        view.findViewById<Button>(R.id.btn_guardar_efectiva).setOnClickListener {
            saveEffectiveVisit(view)
        }

        view.findViewById<Button>(R.id.btn_guardar_no_efectiva).setOnClickListener {
            saveNotEffectiveVisit(view)
        }
    }

    private fun populateMolecules(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_moleculas)
        chipGroup.removeAllViews()
        FakeMedicalRepository.getAllMolecules().forEach { molecule ->
            val chip = Chip(requireContext()).apply {
                text = molecule
                isCheckable = true
                isClickable = true
                chipBackgroundColor = resources.getColorStateList(R.color.accent_primary, null)
            }
            chipGroup.addView(chip)
        }
    }

    private fun saveEffectiveVisit(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_moleculas)
        val selectedMolecules = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) selectedMolecules.add(chip.text.toString())
        }

        if (selectedMolecules.isEmpty()) {
            Snackbar.make(view, "Selecciona al menos una molécula.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val observaciones = view.findViewById<TextInputEditText>(R.id.et_observaciones).text?.toString() ?: ""

        val visit = MedicalVisit(
            id = UUID.randomUUID().toString(),
            doctorId = doctor?.id ?: "",
            doctorNombre = doctor?.nombre ?: "",
            doctorEspecialidad = doctor?.especialidad ?: "",
            fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            efectiva = true,
            result = VisitResult.Effective(selectedMolecules, observaciones)
        )

        FakeMedicalRepository.addVisit(visit)
        Snackbar.make(view, "Visita registrada correctamente ✅", Snackbar.LENGTH_LONG).show()
        dismiss()
    }

    private fun saveNotEffectiveVisit(view: View) {
        val rg = view.findViewById<RadioGroup>(R.id.rg_motivos)
        val selectedId = rg.checkedRadioButtonId
        if (selectedId == -1) {
            Snackbar.make(view, "Selecciona un motivo.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val rb = view.findViewById<RadioButton>(selectedId)
        var motivo = rb.text.toString()
        var otro = ""

        if (motivo == "Otro") {
            otro = view.findViewById<TextInputEditText>(R.id.et_otro).text?.toString()?.trim() ?: ""
            if (otro.isEmpty()) {
                view.findViewById<TextInputLayout>(R.id.til_otro).error = "Especifica el motivo"
                return
            }
        }

        val visit = MedicalVisit(
            id = UUID.randomUUID().toString(),
            doctorId = doctor?.id ?: "",
            doctorNombre = doctor?.nombre ?: "",
            doctorEspecialidad = doctor?.especialidad ?: "",
            fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            efectiva = false,
            result = VisitResult.NotEffective(motivo, otro)
        )

        FakeMedicalRepository.addVisit(visit)
        Snackbar.make(view, "Visita registrada correctamente", Snackbar.LENGTH_LONG).show()
        dismiss()
    }
}
