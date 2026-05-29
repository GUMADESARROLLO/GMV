package com.unimarksa.gmv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
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

        allDoctors = FakeMedicalRepository.getAllDoctors()
        adapter.submitList(allDoctors)

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
