package com.unimarksa.gmv.activities

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.unimarksa.gmv.R
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.OrderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderPrintActivity : AppCompatActivity() {

    private lateinit var binding: com.unimarksa.gmv.databinding.ActivityOrderPrintBinding
    private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase
    private var contentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.unimarksa.gmv.databinding.ActivityOrderPrintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 35) {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val scrim = View(this)
            scrim.setBackgroundColor(ContextCompat.getColor(this, R.color.colorUmkPrimaryDark))
            val content = findViewById<ViewGroup>(android.R.id.content)
            scrim.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            content.addView(scrim)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorUmkPrimaryDark)
        }
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = false

        db = AppDatabase.getInstance(this)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val orderId = intent.getLongExtra("orderId", -1L)
        if (orderId != -1L) {
            cargarDatos(orderId)
        } else {
            finish()
        }
    }

    private fun cargarDatos(orderId: Long) {
        scope.launch {
            val order = db.orderDao().getOrderById(orderId)
            val items = if (order != null) db.orderDao().getItemsByOrderId(orderId) else emptyList()
            withContext(Dispatchers.Main) {
                if (order != null) {
                    mostrarVista(order, items)
                }
            }
        }
    }

    private fun mostrarVista(order: OrderEntity, items: List<com.unimarksa.gmv.data.local.OrderItemEntity>) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.txtFecha.text = dateFmt.format(Date(order.fechaCreacion))
        binding.txtCodePedido.text = "NUM: ${order.codigo}"
        binding.txtName.text = "${order.clienteCodigo} - ${order.clienteNombre}"
        binding.txtCodeClient.text = order.clienteCodigo
        binding.txtDir.text = order.clienteDireccion.ifEmpty { "Sin dirección" }

        var subtotal = 0.0
        for (item in items) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_print_row, binding.lytItems, false) as View
            row.findViewById<TextView>(R.id.tv_desc).text = item.nombre
            row.findViewById<TextView>(R.id.tv_cant).text = "${item.cantidad}"
            row.findViewById<TextView>(R.id.tv_precio).text = "C$ ${fmt.format(item.precio)}"
            binding.lytItems.addView(row)
            subtotal += item.lineaSubtotal
        }

        val iva = order.total - subtotal
        val taxPct = if (subtotal > 0) (iva / subtotal * 100).toInt() else 0
        val sb = StringBuilder()
        sb.appendLine("Subtotal: C$ ${fmt.format(subtotal)}")
        sb.appendLine("IVA ($taxPct%): C$ ${fmt.format(iva)}")
        sb.appendLine("Total: C$ ${fmt.format(order.total)}")
        binding.idResumen.text = sb.toString()

        binding.txtTotal.text = "C$ ${fmt.format(order.total)}"

        binding.btnPrint.setOnClickListener { printOrder() }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        view.draw(canvas)
        return bitmap
    }

    private fun printOrder() {
        val scrollContent = binding.idNestedScrollView
        if (scrollContent.childCount == 0) return

        val contentView = scrollContent.getChildAt(0) as? ViewGroup ?: return
        contentBitmap = getBitmapFromView(contentView)

        val printManager = getSystemService(PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Pedido_${System.currentTimeMillis()}"
        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                val info = PrintDocumentInfo.Builder("pedido")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pageRanges: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val bitmap = contentBitmap ?: run {
                    callback?.onWriteFailed("Bitmap no disponible")
                    return@onWrite
                }

                val bmWidth = bitmap.width
                val bmHeight = bitmap.height
                val pdfWidth = 595
                val pdfHeight = 842
                val scaleX = pdfWidth.toFloat() / bmWidth.toFloat()
                val scaledHeight = (bmHeight * scaleX).toInt()

                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, pdfWidth, scaledHeight.coerceAtMost(pdfHeight)), null)
                pdfDocument.finishPage(page)

                try {
                    destination?.let { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            pdfDocument.writeTo(fos)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }, PrintAttributes.Builder().build())
    }
}
