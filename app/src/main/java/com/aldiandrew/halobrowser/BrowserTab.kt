package com.aldiandrew.halobrowser

import org.json.JSONObject

data class BrowserTab(
    val id: String,
    var url: String,
    var title: String = "",
    var favicon: String? = null,
    var incognito: Boolean = false,
    var desktopSite: Boolean = false,
    var nightMode: Boolean = false,
    var x: Int = 0,
    var y: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("title", title)
        put("favicon", favicon ?: JSONObject.NULL)
        put("incognito", incognito)
        put("desktopSite", desktopSite)
        put("nightMode", nightMode)
        put("x", x)
        put("y", y)
    }

    companion object {
        fun fromJson(json: JSONObject): BrowserTab {
            return BrowserTab(
                id = json.optString("id"),
                url = json.optString("url", "about:blank"),
                title = json.optString("title"),
                favicon = if (json.isNull("favicon")) null else json.optString("favicon"),
                incognito = json.optBoolean("incognito"),
                desktopSite = json.optBoolean("desktopSite"),
                nightMode = json.optBoolean("nightMode"),
                x = json.optInt("x"),
                y = json.optInt("y")
            )
        }
    }
}
