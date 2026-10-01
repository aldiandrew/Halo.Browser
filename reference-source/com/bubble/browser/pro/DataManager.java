package com.bubble.browser.pro;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class DataManager {
    public SharedPreferences a;
    public Context b;
    public Gson c = new Gson();

    public static class BookmarkItem {
        private String title;
        private String url;

        public BookmarkItem(String str, String str2) {
            this.title = str;
            this.url = str2;
        }

        public String getTitle() {
            return this.title;
        }

        public String getUrl() {
            return this.url;
        }
    }

    public static class HistoryItem {
        private long timestamp;
        private String title;
        private String url;

        public HistoryItem(String str, String str2, long j) {
            this.title = str;
            this.url = str2;
            this.timestamp = j;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public String getTitle() {
            return this.title;
        }

        public String getUrl() {
            return this.url;
        }
    }

    public static class SpeedDialItem {
        private String emoji;
        private String title;
        private String url;

        public SpeedDialItem(String str, String str2, String str3) {
            this.title = str;
            this.url = str2;
            this.emoji = str3;
        }

        public String getEmoji() {
            return this.emoji;
        }

        public String getTitle() {
            return this.title;
        }

        public String getUrl() {
            return this.url;
        }
    }

    public DataManager(Context context) {
        this.b = context;
        this.a = context.getSharedPreferences("NotesAppPrefs", 0);
    }

    public void a(BookmarkItem bookmarkItem) {
        List listG = g();
        Iterator it = listG.iterator();
        while (it.hasNext()) {
            if (((BookmarkItem) it.next()).getUrl().equals(bookmarkItem.getUrl())) {
                return;
            }
        }
        listG.add(bookmarkItem);
        q(listG);
    }

    public void b(HistoryItem historyItem) {
        List listI = i();
        for (int i = 0; i < listI.size(); i++) {
            if (((HistoryItem) listI.get(i)).getUrl().equals(historyItem.getUrl())) {
                listI.remove(i);
                break;
            }
        }
        listI.add(0, historyItem);
        if (listI.size() > 50) {
            listI = listI.subList(0, 50);
        }
        r(listI);
    }

    public void c(SpeedDialItem speedDialItem) {
        List listK = k();
        Iterator it = listK.iterator();
        while (it.hasNext()) {
            if (((SpeedDialItem) it.next()).getUrl().equalsIgnoreCase(speedDialItem.getUrl())) {
                return;
            }
        }
        listK.add(speedDialItem);
        s(listK);
    }

    public void d() {
        this.a.edit().remove("bookmarks").apply();
    }

    public void e() {
        this.a.edit().remove("history").apply();
    }

    public List f() {
        String string = this.a.getString("active_tabs", null);
        if (string == null) {
            return new ArrayList();
        }
        return (List) this.c.fromJson(string, new TypeToken<List<BrowserTab>>() { // from class: com.bubble.browser.pro.DataManager.3
        }.getType());
    }

    public List g() {
        String string = this.a.getString("bookmarks", null);
        if (string == null) {
            return new ArrayList();
        }
        return (List) this.c.fromJson(string, new TypeToken<List<BookmarkItem>>() { // from class: com.bubble.browser.pro.DataManager.1
        }.getType());
    }

    public List h() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new SpeedDialItem("Google", "https://www.google.com", "🔍"));
        arrayList.add(new SpeedDialItem("YouTube", "https://www.youtube.com", "🎥"));
        arrayList.add(new SpeedDialItem("Reddit", "https://www.reddit.com", "💬"));
        arrayList.add(new SpeedDialItem("Gmail", "https://mail.google.com", "📧"));
        arrayList.add(new SpeedDialItem("GitHub", "https://github.com", "💻"));
        arrayList.add(new SpeedDialItem("X", "https://x.com", "🐦"));
        arrayList.add(new SpeedDialItem("Wikipedia", "https://wikipedia.org", "📖"));
        arrayList.add(new SpeedDialItem("Amazon", "https://www.amazon.com", "🛒"));
        return arrayList;
    }

    public List i() {
        String string = this.a.getString("history", null);
        if (string == null) {
            return new ArrayList();
        }
        return (List) this.c.fromJson(string, new TypeToken<List<HistoryItem>>() { // from class: com.bubble.browser.pro.DataManager.2
        }.getType());
    }

    public String j() {
        return this.a.getString("search_engine", "google");
    }

    public List k() {
        String string = this.a.getString("speed_dial_sites", null);
        if (string == null) {
            return h();
        }
        try {
            return (List) this.c.fromJson(string, new TypeToken<List<SpeedDialItem>>() { // from class: com.bubble.browser.pro.DataManager.4
            }.getType());
        } catch (Exception unused) {
            return h();
        }
    }

    public boolean l() {
        return this.a.getBoolean("adblocker_enabled", true);
    }

    public boolean m(String str) {
        List listG = g();
        if (str == null) {
            return false;
        }
        Iterator it = listG.iterator();
        while (it.hasNext()) {
            if (((BookmarkItem) it.next()).getUrl().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public void n(BookmarkItem bookmarkItem) {
        List listG = g();
        for (int i = 0; i < listG.size(); i++) {
            if (((BookmarkItem) listG.get(i)).getUrl().equals(bookmarkItem.getUrl())) {
                listG.remove(i);
                break;
            }
        }
        q(listG);
    }

    public void o(String str) {
        List listK = k();
        for (int i = 0; i < listK.size(); i++) {
            if (((SpeedDialItem) listK.get(i)).getUrl().equalsIgnoreCase(str)) {
                listK.remove(i);
                break;
            }
        }
        s(listK);
    }

    public void p(List list) {
        if (list == null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            BrowserTab browserTab = (BrowserTab) it.next();
            if (browserTab != null && !browserTab.isIncognito()) {
                arrayList.add(browserTab);
            }
        }
        this.a.edit().putString("active_tabs", this.c.toJson(arrayList)).commit();
    }

    public final void q(List list) {
        this.a.edit().putString("bookmarks", this.c.toJson(list)).apply();
    }

    public final void r(List list) {
        this.a.edit().putString("history", this.c.toJson(list)).apply();
    }

    public void s(List list) {
        this.a.edit().putString("speed_dial_sites", this.c.toJson(list)).apply();
    }

    public void t(boolean z) {
        this.a.edit().putBoolean("adblocker_enabled", z).apply();
    }

    public void u(String str) {
        this.a.edit().putString("search_engine", str).apply();
    }
}

================================================================
