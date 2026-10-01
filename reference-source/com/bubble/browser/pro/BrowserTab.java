package com.bubble.browser.pro;

import android.graphics.Bitmap;
import java.io.Serializable;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class BrowserTab implements Serializable {
    private transient Bitmap favicon;
    private String id;
    private boolean isDesktopSite;
    private boolean isIncognito;
    private boolean isNightMode;
    private String title;
    private String url;

    public BrowserTab() {
    }

    public Bitmap getFavicon() {
        return this.favicon;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public boolean isDesktopSite() {
        return this.isDesktopSite;
    }

    public boolean isIncognito() {
        return this.isIncognito;
    }

    public boolean isNightMode() {
        return this.isNightMode;
    }

    public void setDesktopSite(boolean z) {
        this.isDesktopSite = z;
    }

    public void setFavicon(Bitmap bitmap) {
        this.favicon = bitmap;
    }

    public void setId(String str) {
        this.id = str;
    }

    public void setIncognito(boolean z) {
        this.isIncognito = z;
    }

    public void setNightMode(boolean z) {
        this.isNightMode = z;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public void setUrl(String str) {
        this.url = str;
    }

    public BrowserTab(String str, String str2) {
        this(str, str2, false);
    }

    public BrowserTab(String str, String str2, boolean z) {
        this.id = str;
        this.url = str2;
        this.isIncognito = z;
        this.title = "New Tab";
    }
}

================================================================
