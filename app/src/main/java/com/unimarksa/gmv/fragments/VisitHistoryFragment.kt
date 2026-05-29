package com.unimarksa.gmv.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.unimarksa.gmv.R
import com.unimarksa.gmv.adapters.VisitHistoryAdapter
import com.unimarksa.gmv.models.MedicalVisit
import com.unimarksa.gmv.tools.FakeMedicalRepository
import java.util.Calendar

class VisitHistoryFragment : Fragment() {

    private val adapter = VisitHistoryAdapter()
    private var allVisits: List<MedicalVisit> = listOf()
    private var filterEffective: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_visit_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_visitas)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        allVisits = FakeMedicalRepository.getAllVisits()
        adapter.submitList(allVisits)

        view.findViewById<ChipGroup>(R.id.chip_group_filtro).setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            filterEffective = when (chipId) {
                R.id.chip_efectivas -> true
                R.id.chip_no_efectivas -> false
                else -> null
            }
            applyFilters()
        }

        view.findViewById<Button>(R.id.btn_filtrar_fecha).setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    applyFilters()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun applyFilters() {
        val filtered = if (filterEffective == null) {
            allVisits
        } else {
            allVisits.filter { it.efectiva == filterEffective }
        }
        adapter.submitList(filtered)
        view?.findViewById<LinearLayout>(R.id.lyt_empty)?.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
