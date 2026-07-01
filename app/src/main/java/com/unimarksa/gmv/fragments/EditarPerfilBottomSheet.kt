package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.MedicalApiService
import com.unimarksa.gmv.tools.SessionManager

class EditarPerfilBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_editar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextInputEditText>(R.id.et_nombre).setText(SessionManager.getFullName())
        view.findViewById<TextInputEditText>(R.id.et_email).setText(SessionManager.getEmail())
        view.findViewById<TextInputEditText>(R.id.et_telefono).setText(SessionManager.getPhone())
        view.findViewById<TextInputEditText>(R.id.et_contrasenna).setText(SessionManager.getPassword())

        view.findViewById<Button>(R.id.btn_guardar).setOnClickListener {
            val nombre = view.findViewById<TextInputEditText>(R.id.et_nombre).text?.toString()?.trim() ?: ""
            val email = view.findViewById<TextInputEditText>(R.id.et_email).text?.toString()?.trim() ?: ""
            val telefono = view.findViewById<TextInputEditText>(R.id.et_telefono).text?.toString()?.trim() ?: ""
            val contrasenna = view.findViewById<TextInputEditText>(R.id.et_contrasenna).text?.toString()?.trim() ?: ""

            if (nombre.isEmpty() || email.isEmpty() || telefono.isEmpty()) {
                Snackbar.make(view, "Nombre, email y teléfono son requeridos", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val btn = view.findViewById<Button>(R.id.btn_guardar)
            btn.isEnabled = false
            btn.text = "Guardando..."

            MedicalApiService.updateProfile(
                name = nombre,
                email = email,
                telefono = telefono,
                contrasenna = contrasenna,
                onResult = {
                    dismiss()
                },
                onError = { error ->
                    btn.isEnabled = true
                    btn.text = "Guardar cambios"
                    Snackbar.make(view, error, Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }
}
