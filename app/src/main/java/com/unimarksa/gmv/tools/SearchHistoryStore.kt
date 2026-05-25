package com.unimarksa.gmv.tools

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SearchHistoryStore {
    private const val PREFS_NAME = "lote_search_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_ITEMS = 10

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHistory(): MutableList<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return mutableListOf()
        val type = object : TypeToken<List<String>>() {}.type
        return (gson.fromJson(json, type) as? List<String>)?.toMutableList() ?: mutableListOf()
    }

    fun add(query: String) {
        val list = getHistory()
        list.remove(query)
        list.add(0, query)
        if (list.size > MAX_ITEMS) list.removeAt(list.lastIndex)
        save(list)
    }

    fun remove(query: String) {
        val list = getHistory()
        list.remove(query)
        save(list)
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }
}
