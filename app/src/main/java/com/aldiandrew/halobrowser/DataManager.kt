package com.aldiandrew.halobrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DataManager(context: Context) {
    private val prefs = context.getSharedPreferences("halo_data_manager", Context.MODE_PRIVATE)

    var searchEngine: String
        get() = prefs.getString("search_engine", "https://www.google.com/search?q=")!!
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean("adblock", true)
        set(value) = prefs.edit().putBoolean("adblock", value).apply()

    fun loadActiveTabs(): MutableList<BrowserTab> {
        val raw = prefs.getString("active_tabs", "[]") ?: "[]"
        val array = JSONArray(raw)
        return MutableList(array.length()) { index ->
            BrowserTab.fromJson(array.getJSONObject(index))
        }
    }

    fun saveActiveTabs(tabs: List<BrowserTab>) {
        val array = JSONArray()
        tabs.forEach { array.put(it.toJson()) }
        prefs.edit().putString("active_tabs", array.toString()).apply()
    }

    fun addHistory(url: String, title: String) {
        val array = JSONArray(prefs.getString("history", "[]") ?: "[]")
        val entry = JSONObject().apply {
            put("url", url)
            put("title", title)
            put("time", System.currentTimeMillis())
        }
        val result = JSONArray()
        result.put(entry)
        for (i in 0 until minOf(array.length(), 199)) result.put(array.getJSONObject(i))
        prefs.edit().putString("history", result.toString()).apply()
    }

    fun history(): List<Pair<String, String>> {
        val array = JSONArray(prefs.getString("history", "[]") ?: "[]")
        return (0 until array.length()).map {
            val o = array.getJSONObject(it)
            o.optString("url") to o.optString("title")
        }
    }

    fun addBookmark(url: String, title: String) {
        val array = JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
        for (i in 0 until array.length()) {
            if (array.getJSONObject(i).optString("url") == url) return
        }
        array.put(JSONObject().apply {
            put("url", url)
            put("title", title)
        })
        prefs.edit().putString("bookmarks", array.toString()).apply()
    }

    fun bookmarks(): List<Pair<String, String>> {
        val array = JSONArray(prefs.getString("bookmarks", "[]") ?: "[]")
        return (0 until array.length()).map {
            val o = array.getJSONObject(it)
            o.optString("url") to o.optString("title")
        }
    }

    fun saveSpeedDial(items: List<Pair<String, String>>) {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().apply {
                put("title", it.first)
                put("url", it.second)
            })
        }
        prefs.edit().putString("speed_dial", array.toString()).apply()
    }

    fun speedDial(): List<Pair<String, String>> {
        val array = JSONArray(prefs.getString("speed_dial", "[]") ?: "[]")
        return (0 until array.length()).map {
            val o = array.getJSONObject(it)
            o.optString("title") to o.optString("url")
        }
    }
}
