package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.DoctorAdapter
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.tools.FakeMedicalRepository

class DoctorListFragment : Fragment() {

    private val adapter = DoctorAdapter { doctor ->
        val bundle = Bundle().apply { putString("doctorId", doctor.id) }
        findNavController().navigate(R.id.doctorDetailFragment, bundle)
    }

    private var allDoctors: List<Doctor> = listOf()
    private var loadingView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_medicos)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        cargarMedicos(view)

        view.findViewById<TextInputEditText>(R.id.et_buscar).setOnTextChangedListener { text ->
            val filtered = if (text.isNullOrEmpty()) {
                allDoctors
            } else {
                allDoctors.filter {
                    it.nombre.contains(text.toString(), ignoreCase = true) ||
                    it.especialidad.contains(text.toString(), ignoreCase = true)
                }
            }
            adapter.submitList(filtered)
            view.findViewById<LinearLayout>(R.id.lyt_empty).visibility =
                if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun cargarMedicos(view: View) {
        mostrarLoading(view)
        FakeMedicalRepository.refreshDoctors(
            onResult = {
                ocultarLoading()
                allDoctors = FakeMedicalRepository.getAllDoctors()
                adapter.submitList(allDoctors)
                view.findViewById<LinearLayout>(R.id.lyt_empty).visibility =
                    if (allDoctors.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { error ->
                ocultarLoading()
                view.findViewById<LinearLayout>(R.id.lyt_empty).visibility = View.VISIBLE
                Snackbar.make(view, error, Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun mostrarLoading(view: View) {
        val container = view.findViewById<RecyclerView>(R.id.rv_medicos).parent as FrameLayout
        if (loadingView != null) return
        val ctx = requireContext()
        val lv = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        content.addView(ProgressBar(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                val px = (16 * ctx.resources.displayMetrics.density + 0.5f).toInt()
                setPadding(0, px, 0, px)
            }
        })
        content.addView(TextView(ctx).apply {
            text = "Cargando médicos..."
            textSize = 14f
            setTextColor(0xFF888888.toInt())
        })
        lv.addView(content)
        container.addView(lv)
        loadingView = lv
    }

    private fun ocultarLoading() {
        loadingView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            loadingView = null
        }
    }

    private fun TextInputEditText.setOnTextChangedListener(listener: (String?) -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                listener(s?.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
}
