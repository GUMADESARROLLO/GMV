package com.unimarksa.gmv.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.unimarksa.gmv.R
import com.unimarksa.gmv.tools.BarrasSistema
import com.unimarksa.gmv.adapters.AdapterSuggestionSearch
import com.unimarksa.gmv.adapters.HistorialLotesAdapter
import com.unimarksa.gmv.databinding.ActivityHistorialLotesBinding
import com.unimarksa.gmv.tools.ApiService
import com.unimarksa.gmv.tools.SearchHistoryStore

class HistorialLotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialLotesBinding
    private val resultAdapter = HistorialLotesAdapter(mutableListOf())
    private var codCliente: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialLotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BarrasSistema.aplicar(this, R.color.umk_mist)

        SearchHistoryStore.init(this)

        codCliente = intent.getStringExtra("codCliente") ?: ""

        binding.rvResultados.layoutManager = LinearLayoutManager(this)
        binding.rvResultados.adapter = resultAdapter

        setupSuggestionPanel()
        setupSearchInput()
        setupListeners()
    }

    private fun setupSuggestionPanel() {
        val adapter = AdapterSuggestionSearch(
            items = mutableListOf(),
            onItemClick = { query ->
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
                hideSuggestion()
                hideKeyboard()
                performSearch()
            },
            onItemDelete = { query ->
                SearchHistoryStore.remove(query)
                val a = binding.recyclerSuggestion.adapter as AdapterSuggestionSearch
                a.update(SearchHistoryStore.getHistory())
                if (SearchHistoryStore.getHistory().isEmpty()) hideSuggestion()
            }
        )
        binding.recyclerSuggestion.layoutManager = LinearLayoutManager(this)
        binding.recyclerSuggestion.adapter = adapter

        binding.btnClearHistory.setOnClickListener {
            SearchHistoryStore.clear()
            adapter.update(SearchHistoryStore.getHistory())
            hideSuggestion()
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideSuggestion()
                hideKeyboard()
                performSearch()
                true
            } else false
        }

        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                hideSuggestion()
                hideKeyboard()
                performSearch()
                true
            } else false
        }

        binding.etSearch.setOnTouchListener { _, _ ->
            showSuggestion()
            false
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnBuscar.setOnClickListener {
            if (binding.etSearch.text.isNullOrBlank()) {
                binding.etSearch.error = "Escribe un código de lote"
                binding.etSearch.requestFocus()
                return@setOnClickListener
            }
            hideSuggestion()
            hideKeyboard()
            performSearch()
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
            binding.btnClear.visibility = View.GONE
            showEmptyState()
        }
    }

    private fun showSuggestion() {
        val history = SearchHistoryStore.getHistory()
        val adapter = binding.recyclerSuggestion.adapter as? AdapterSuggestionSearch ?: return
        if (history.isEmpty()) return
        adapter.update(history)
        binding.lytSuggestion.visibility = View.VISIBLE
    }

    private fun hideSuggestion() {
        binding.lytSuggestion.visibility = View.GONE
    }

    private fun performSearch() {
        val lote = binding.etSearch.text.toString().trim()
        if (lote.isEmpty()) return
        if (codCliente.isEmpty()) return

        SearchHistoryStore.add(lote)

        showLoading()
        ApiService.fetchHistoryLotes(
            lote = lote,
            codCliente = codCliente,
            onResult = { lista ->
                runOnUiThread {
                    hideLoading()
                    if (lista.isEmpty()) {
                        showNoResult()
                    } else {
                        showResults(lista)
                    }
                }
            },
            onError = {
                runOnUiThread {
                    hideLoading()
                    showNoResult()
                }
            }
        )
    }

    private fun showResults(lista: List<com.unimarksa.gmv.models.ItemLoteHistorial>) {
        binding.lytEmpty.visibility = View.GONE
        binding.lytNoResult.visibility = View.GONE
        binding.lytResults.visibility = View.VISIBLE
        binding.tvResultCount.visibility = View.VISIBLE
        binding.tvResultCount.text = "${lista.size} resultado${if (lista.size != 1) "s" else ""}"
        resultAdapter.update(lista)
    }

    private fun showEmptyState() {
        binding.lytResults.visibility = View.GONE
        binding.lytNoResult.visibility = View.GONE
        binding.lytEmpty.visibility = View.VISIBLE
    }

    private fun showNoResult() {
        binding.lytResults.visibility = View.GONE
        binding.lytEmpty.visibility = View.GONE
        binding.lytNoResult.visibility = View.VISIBLE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.lytResults.visibility = View.GONE
        binding.lytEmpty.visibility = View.GONE
        binding.lytNoResult.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
