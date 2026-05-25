package com.unimarksa.gmv.fragments.medical

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.model.Doctor
import com.unimarksa.gmv.data.model.DoctorStatus
import com.unimarksa.gmv.data.repository.FakeMedicalRepository
import java.util.UUID

class AddDoctorBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_doctor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val specialties = FakeMedicalRepository.specialties
        val actv = view.findViewById<AutoCompleteTextView>(R.id.actv_especialidad)
        actv.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, specialties))

        view.findViewById<Button>(R.id.btn_cancelar).setOnClickListener { dismiss() }

        view.findViewById<Button>(R.id.btn_enviar).setOnClickListener {
            val nombre = view.findViewById<TextInputEditText>(R.id.et_nombre).text?.toString()?.trim()
            val especialidad = actv.text?.toString()?.trim()
            val telefono = view.findViewById<TextInputEditText>(R.id.et_telefono).text?.toString()?.trim()
            val email = view.findViewById<TextInputEditText>(R.id.et_email).text?.toString()?.trim()

            var valid = true

            if (nombre.isNullOrEmpty()) {
                view.findViewById<TextInputLayout>(R.id.til_nombre).error = "Campo requerido"
                valid = false
            } else {
                view.findViewById<TextInputLayout>(R.id.til_nombre).error = null
            }

            if (especialidad.isNullOrEmpty()) {
                view.findViewById<TextInputLayout>(R.id.til_especialidad).error = "Campo requerido"
                valid = false
            } else {
                view.findViewById<TextInputLayout>(R.id.til_especialidad).error = null
            }

            if (telefono.isNullOrEmpty()) {
                view.findViewById<TextInputLayout>(R.id.til_telefono).error = "Campo requerido"
                valid = false
            } else {
                view.findViewById<TextInputLayout>(R.id.til_telefono).error = null
            }

            if (valid) {
                val newDoctor = Doctor(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre!!,
                    especialidad = especialidad!!,
                    telefono = telefono!!,
                    email = email ?: "",
                    estado = DoctorStatus.PENDIENTE_ALTA
                )
                FakeMedicalRepository.addDoctor(newDoctor)
                Snackbar.make(view, "Solicitud enviada. Pendiente de aprobación.", Snackbar.LENGTH_LONG).show()
                dismiss()
            }
        }
    }
}
