package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.Searchable

class PerfilFragment : Fragment(), Searchable {

    private lateinit var nestedScroll: NestedScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nestedScroll = view.findViewById(R.id.nested_content)
    }

    override fun onSearchQuery(query: String) {
        val items = listOf(
            R.id.btn_order_history to R.string.txt_profile_history,
            R.id.btn_vineta_history to R.string.txt_profile_history_vineta,
            R.id.Reportes to "Facturación",
            R.id.btn_recibos_history to "Recibos",
            R.id.id_mis_stat to "Mis Estadisticas",
            R.id.id_plan_trabajo to "Plan de trabajo",
            R.id.id_fragment_comisiones to "Comisiónes (Beta)",
            R.id.btn_inteligencia_mercado to R.string.txt_inteligencia_mercado,
            R.id.txt_user_name to "Cambiar Contraseña",
            R.id.AboutUs to "Acerca GMV",
            R.id.btn_edit_user to R.string.btn_edit_profile
        )
        for ((id, label) in items) {
            val v = view?.findViewById<View>(id) ?: continue
            val text = when (label) {
                is Int -> getString(label)
                is String -> label
                else -> ""
            }
            v.visibility = if (query.isEmpty() || text.lowercase().contains(query))
                View.VISIBLE else View.GONE
        }
    }
}
