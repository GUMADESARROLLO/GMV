package com.unimarksa.gmv.activities

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import com.unimarksa.gmv.R
import com.unimarksa.gmv.models.ItemAdjunto
import com.unimarksa.gmv.tools.ApiService
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AdjuntarComprobanteActivity : AppCompatActivity() {

    private var idRecibo = ""
    private val existingAttachments = mutableListOf<ItemAdjunto>()
    private val newUris = mutableListOf<Uri>()
    private lateinit var adapter: AttachmentAdapter
    private var progressDialog: ProgressDialog? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            newUris.addAll(uris)
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

        idRecibo = intent.getStringExtra("id_recibo") ?: ""

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvFotos)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = AttachmentAdapter(existingAttachments, newUris) { index ->
            if (index >= existingAttachments.size) {
                newUris.removeAt(index - existingAttachments.size)
                adapter.notifyDataSetChanged()
                updateUI()
            }
        }
        rv.adapter = adapter

        findViewById<MaterialButton>(R.id.btnAgregar).setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        findViewById<MaterialButton>(R.id.btnSubir).setOnClickListener {
            if (newUris.isEmpty()) {
                finish()
                return@setOnClickListener
            }
            uploadAll()
        }

        if (idRecibo.isNotEmpty()) {
            cargarAdjuntos()
        }
        updateUI()
    }

    private fun cargarAdjuntos() {
        ApiService.fetchAdjuntos(
            idRecibo = idRecibo,
            onResult = { items ->
                existingAttachments.clear()
                existingAttachments.addAll(items)
                adapter.notifyDataSetChanged()
                updateUI()
            },
            onError = { }
        )
    }

    private fun uploadAll() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Subiendo comprobantes")
            setMessage("0/${newUris.size}")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = newUris.size
            setCancelable(false)
            show()
        }

        var completed = 0
        for ((i, uri) in newUris.withIndex()) {
            try {
                val inputStream: InputStream = contentResolver.openInputStream(uri) ?: continue
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resized = resizeBitmap(bitmap, 800)
                val base64 = bitmapToBase64(resized)
                resized.recycle()
                bitmap.recycle()

                ApiService.enviarAdjunto(
                    idRecibo = idRecibo,
                    nombre = "img_${System.currentTimeMillis()}_$i",
                    imagenBase64 = base64,
                    onResult = {
                        completed++
                        runOnUiThread {
                            progressDialog?.progress = completed
                            progressDialog?.setMessage("$completed/${newUris.size}")
                            if (completed >= newUris.size) {
                                progressDialog?.dismiss()
                                Snackbar.make(findViewById(R.id.btnSubir),
                                    "$completed comprobante(s) subido(s)", Snackbar.LENGTH_LONG).show()
                                cargarAdjuntos()
                                newUris.clear()
                                adapter.notifyDataSetChanged()
                                updateUI()
                            }
                        }
                    },
                    onError = { error ->
                        runOnUiThread {
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar imagen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = if (width > height) maxSize.toFloat() / width else maxSize.toFloat() / height
        if (ratio >= 1f) return bitmap
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun updateUI() {
        val total = existingAttachments.size + newUris.size
        findViewById<TextView>(R.id.tvCounter).text = "$total comprobante(s)"
        findViewById<MaterialButton>(R.id.btnSubir).isEnabled = newUris.isNotEmpty()
    }

    class AttachmentAdapter(
        private val existing: List<ItemAdjunto>,
        private val uris: List<Uri>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>() {

        override fun getItemCount() = existing.size + uris.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
            val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_attachment, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position < existing.size) {
                val item = existing[position]
                Picasso.get().load(item.imagenUrl)
                    .placeholder(R.drawable.ic_placeholder_product)
                    .resize(300, 200).centerCrop().into(holder.ivPhoto)
                holder.btnRemove.visibility = android.view.View.GONE
            } else {
                val uri = uris[position - existing.size]
                Picasso.get().load(uri).resize(300, 200).centerCrop().into(holder.ivPhoto)
                holder.btnRemove.visibility = android.view.View.VISIBLE
                holder.btnRemove.setOnClickListener { onRemove(position) }
            }
        }
    }
}
