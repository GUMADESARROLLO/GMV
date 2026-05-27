package com.unimarksa.gmv.activities

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R

class AdjuntarComprobanteActivity : AppCompatActivity() {

    private val selectedUris = mutableListOf<Uri>()
    private lateinit var adapter: PhotoAdapter

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.addAll(uris)
            adapter.notifyDataSetChanged()
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adjuntar_comprobante)

        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary_dark)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvFotos)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = PhotoAdapter(selectedUris) { index ->
            selectedUris.removeAt(index)
            adapter.notifyDataSetChanged()
            updateUI()
        }
        rv.adapter = adapter

        findViewById<MaterialButton>(R.id.btnAgregar).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        findViewById<MaterialButton>(R.id.btnSubir).setOnClickListener {
            Snackbar.make(findViewById(R.id.btnSubir),
                "${selectedUris.size} comprobante(s) subido(s) correctamente", Snackbar.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateUI() {
        findViewById<TextView>(R.id.tvCounter).text =
            "${selectedUris.size} foto(s) seleccionada(s)"
        findViewById<MaterialButton>(R.id.btnSubir).isEnabled = selectedUris.isNotEmpty()
    }

    class PhotoAdapter(
        private val uris: MutableList<Uri>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
            val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_attachment, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = uris[position]
            Picasso.get().load(uri).resize(300, 200).centerCrop().into(holder.ivPhoto)
            holder.btnRemove.setOnClickListener { onRemove(position) }
        }

        override fun getItemCount() = uris.size
    }
}
