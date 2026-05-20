package com.unimarksa.gmv

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PerfilFragment : Fragment() {

    private val datos = listOf(
        ItemLista("Mi Cuenta", "Usuario: admin@gmv.com", "", 0, Color.parseColor("#BBDEFB")),
        ItemLista("Notificaciones", "Push, Email, SMS activados", "", 2, Color.parseColor("#E1BEE7")),
        ItemLista("Seguridad", "2FA habilitado", "", 0, Color.parseColor("#C8E6C9")),
        ItemLista("Tema", "Modo claro activo", "", 0, Color.parseColor("#FFCCBC")),
        ItemLista("Idioma", "Español (México)", "", 0, Color.parseColor("#B2EBF2")),
        ItemLista("Almacenamiento", "2.3 GB usados de 10 GB", "", 0, Color.parseColor("#BBDEFB")),
        ItemLista("Acerca de", "GMV v1.0.0", "", 0, Color.parseColor("#E1BEE7")),
        ItemLista("Cerrar Sesión", "Volver a pantalla de login", "", 0, Color.parseColor("#FFCCBC")),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ListaAdapter(datos) { /* click handler */ }
    }
}
