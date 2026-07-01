package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.SessionManager

class PerfilMedicoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil_medico, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = SessionManager.getUsername()
        val fullName = SessionManager.getFullName()
        val email = SessionManager.getEmail()
        val phone = SessionManager.getPhone()

        val initials = fullName.take(2).uppercase().ifEmpty { "VM" }

        view.findViewById<TextView>(R.id.tv_avatar).text = initials
        view.findViewById<TextView>(R.id.tv_nombre).text = fullName.ifEmpty { "Visitador Médico" }
        view.findViewById<TextView>(R.id.tv_email).text = email.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tv_usuario).text = username.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tv_telefono).text = phone.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tv_email_info).text = email.ifEmpty { "-" }

        view.findViewById<Button>(R.id.btn_cambiar_contrasenna).setOnClickListener {
            EditarPerfilBottomSheet().show(childFragmentManager, "EditarPerfil")
        }
    }
}
