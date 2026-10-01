package com.bubble.browser.pro;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.browser.browser.pro.R$drawable;
import com.browser.browser.pro.R$id;
import com.browser.browser.pro.R$layout;
import com.browser.browser.pro.R$style;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.common.internal.ImagesContract;
import com.google.firebase.analytics.FirebaseAnalytics;
import defpackage.M7;
import defpackage.t0;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import us.google.android.ads.nativetemplates2.TemplateView2;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class a {
    public final View a;
    public final ChatHeadService b;
    public final WebView c;
    public final EditText d;
    public final ProgressBar e;
    public final ImageButton f;
    public final ImageButton g;
    public final ImageButton h;
    public final ImageButton i;
    public BrowserTab j;
    public DataManager k;
    public String l;
    public FrameLayout m;
    public TemplateView2 n;
    public long o = 0;
    public String p = null;
    public String q = null;

    /* JADX INFO: renamed from: com.bubble.browser.pro.a$a, reason: collision with other inner class name */
    public class C0053a extends WebViewClient {
        public C0053a() {
        }

        @Override // android.webkit.WebViewClient
        public void onPageFinished(WebView webView, String str) {
            super.onPageFinished(webView, str);
            a.this.e.setVisibility(8);
            String title = webView.getTitle();
            a.this.q = title;
            a.this.a0(str);
            if ("about:home".equals(str) || str.startsWith("https://localhost/")) {
                a.this.j.setUrl("about:home");
                a.this.j.setTitle("New Tab");
            } else {
                a.this.j.setUrl(str);
                a.this.j.setTitle(title);
            }
            if (a.this.b != null) {
                a.this.b.H(a.this.j.getId());
            }
            a.this.b0();
            if (a.this.k != null && str != null && !str.isEmpty() && !"about:home".equals(str) && !str.startsWith("https://localhost/") && !a.this.j.isIncognito()) {
                DataManager dataManager = a.this.k;
                if (title == null) {
                    title = str;
                }
                dataManager.b(new DataManager.HistoryItem(title, str, System.currentTimeMillis()));
            }
            if (a.this.j.isNightMode()) {
                a.this.D(webView);
            }
        }

        @Override // android.webkit.WebViewClient
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            super.onPageStarted(webView, str, bitmap);
            a.this.U();
            a.this.o = System.currentTimeMillis();
            a.this.p = str;
            a.this.q = null;
            a.this.e.setVisibility(0);
            a.this.a0(str);
            if ("about:home".equals(str) || str.startsWith("https://localhost/")) {
                a.this.d.setText(RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED);
                a.this.j.setUrl("about:home");
            } else {
                a.this.d.setText(str);
                a.this.j.setUrl(str);
            }
            if (a.this.b != null) {
                a.this.b.H(a.this.j.getId());
            }
            a.this.b0();
        }

        @Override // android.webkit.WebViewClient
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
            return (a.this.k != null && a.this.k.l() && t0.a(webResourceRequest.getUrl().toString())) ? new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED.getBytes())) : super.shouldInterceptRequest(webView, webResourceRequest);
        }

        @Override // android.webkit.WebViewClient
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
            return false;
        }
    }

    public class b extends WebChromeClient {
        public b() {
        }

        @Override // android.webkit.WebChromeClient
        public void onProgressChanged(WebView webView, int i) {
            a.this.e.setProgress(i);
        }

        @Override // android.webkit.WebChromeClient
        public void onReceivedIcon(WebView webView, Bitmap bitmap) {
            super.onReceivedIcon(webView, bitmap);
            if (a.this.j == null || bitmap == null) {
                return;
            }
            a.this.j.setFavicon(bitmap);
            if (a.this.b != null) {
                a.this.b.z(a.this.j.getId());
            }
        }
    }

    public class c implements Runnable {
        public final /* synthetic */ String c;

        public c(String str) {
            this.c = str;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (a.this.m == null || a.this.n == null) {
                return;
            }
            String str = this.c;
            if (str != null && !str.isEmpty() && !"about:home".equals(this.c) && !this.c.startsWith("https://localhost/")) {
                a.this.m.setVisibility(8);
            } else {
                a.this.m.setVisibility(0);
                z0.g(a.this.n, a.this.a.getContext(), a.this.m);
            }
        }
    }

    public class d {
        public d() {
        }

        public final /* synthetic */ void d(String str) {
            a.this.T(str);
        }

        public final /* synthetic */ void e() {
            a.this.X();
        }

        public final /* synthetic */ void f(String str, String str2) {
            a.this.Y(str, str2);
        }

        @JavascriptInterface
        public void openUrl(final String str) {
            a.this.c.post(new Runnable() { // from class: S3
                @Override // java.lang.Runnable
                public final void run() {
                    this.c.d(str);
                }
            });
        }

        @JavascriptInterface
        public void showAddDialog() {
            a.this.c.post(new Runnable() { // from class: T3
                @Override // java.lang.Runnable
                public final void run() {
                    this.c.e();
                }
            });
        }

        @JavascriptInterface
        public void showDeleteDialog(final String str, final String str2) {
            a.this.c.post(new Runnable() { // from class: U3
                @Override // java.lang.Runnable
                public final void run() {
                    this.c.f(str, str2);
                }
            });
        }
    }

    public a(View view, final ChatHeadService chatHeadService, BrowserTab browserTab) {
        String url;
        View viewFindViewById;
        this.a = view;
        this.b = chatHeadService;
        this.j = browserTab;
        this.k = new DataManager(view.getContext());
        this.c = (WebView) view.findViewById(R$id.webview);
        this.d = (EditText) view.findViewById(R$id.edit_url);
        this.e = (ProgressBar) view.findViewById(R$id.progress_bar);
        this.f = (ImageButton) view.findViewById(R$id.btn_back);
        this.g = (ImageButton) view.findViewById(R$id.btn_forward);
        this.h = (ImageButton) view.findViewById(R$id.btn_refresh);
        this.i = (ImageButton) view.findViewById(R$id.btn_more);
        this.m = (FrameLayout) view.findViewById(R$id.home_native_ad_container);
        this.n = view.findViewById(R$id.home_native_ad_template);
        if (browserTab.isIncognito() && (viewFindViewById = view.findViewById(R$id.browser_toolbar)) != null) {
            viewFindViewById.setBackgroundColor(Color.parseColor("#202124"));
        }
        H();
        G();
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R$id.banner_ad_container);
        if (linearLayout != null) {
            z0.e(view.getContext(), linearLayout);
        }
        if (browserTab.getUrl() == null || browserTab.getUrl().isEmpty()) {
            url = "about:home";
            T("about:home");
        } else {
            T(browserTab.getUrl());
            url = browserTab.getUrl();
        }
        a0(url);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() { // from class: I3
            @Override // android.view.View.OnKeyListener
            public final boolean onKey(View view2, int i, KeyEvent keyEvent) {
                return this.c.O(chatHeadService, view2, i, keyEvent);
            }
        });
    }

    public final void D(WebView webView) {
        webView.evaluateJavascript("javascript:(function() { var css = 'html { filter: invert(1) hue-rotate(180deg) !important; } img, video, iframe { filter: invert(1) hue-rotate(180deg) !important; }'; var head = document.head || document.getElementsByTagName('head')[0]; var style = document.getElementById('night-mode-style'); if (!style) {   style = document.createElement('style');   style.type = 'text/css';   style.id = 'night-mode-style';   style.appendChild(document.createTextNode(css));   head.appendChild(style); }})()", null);
    }

    public final void E(WebView webView) {
        webView.evaluateJavascript("javascript:(function() { var style = document.getElementById('night-mode-style'); if (style) { style.parentNode.removeChild(style); } })()", null);
    }

    public final String F(String str) {
        if (str == null) {
            return RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED;
        }
        try {
            String host = new URI(str).getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception unused) {
        }
        int iIndexOf = str.indexOf("//");
        if (iIndexOf != -1) {
            str = str.substring(iIndexOf + 2);
        }
        int iIndexOf2 = str.indexOf("/");
        if (iIndexOf2 != -1) {
            str = str.substring(0, iIndexOf2);
        }
        int iIndexOf3 = str.indexOf(":");
        if (iIndexOf3 != -1) {
            str = str.substring(0, iIndexOf3);
        }
        return str.startsWith("www.") ? str.substring(4) : str;
    }

    public final void G() {
        this.d.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: K3
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return this.a.I(textView, i, keyEvent);
            }
        });
        this.f.setOnClickListener(new View.OnClickListener() { // from class: L3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.J(view);
            }
        });
        this.g.setOnClickListener(new View.OnClickListener() { // from class: M3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.K(view);
            }
        });
        this.h.setOnClickListener(new View.OnClickListener() { // from class: N3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.L(view);
            }
        });
        this.i.setOnClickListener(new View.OnClickListener() { // from class: O3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.M(view);
            }
        });
        b0();
    }

    public final void H() {
        WebSettings settings = this.c.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        String userAgentString = settings.getUserAgentString();
        this.l = userAgentString;
        if (userAgentString.contains("Windows")) {
            this.l = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
        }
        this.c.setDownloadListener(new DownloadListener() { // from class: J3
            @Override // android.webkit.DownloadListener
            public final void onDownloadStart(String str, String str2, String str3, String str4, long j) {
                this.c.N(str, str2, str3, str4, j);
            }
        });
        this.c.setWebViewClient(new C0053a());
        if (this.j.isIncognito()) {
            settings.setCacheMode(2);
            settings.setDatabaseEnabled(false);
            settings.setDomStorageEnabled(false);
            settings.setSaveFormData(false);
            settings.setSavePassword(false);
        }
        if (this.j.isDesktopSite()) {
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
        } else {
            settings.setUserAgentString(this.l);
            settings.setLoadWithOverviewMode(false);
            settings.setUseWideViewPort(false);
        }
        this.c.setWebChromeClient(new b());
        this.c.addJavascriptInterface(new d(), "Android");
    }

    public final /* synthetic */ boolean I(TextView textView, int i, KeyEvent keyEvent) {
        if (i != 2 && i != 6 && (keyEvent == null || keyEvent.getKeyCode() != 66)) {
            return false;
        }
        T(this.d.getText().toString());
        return true;
    }

    public final /* synthetic */ void J(View view) {
        if (this.c.canGoBack()) {
            this.c.goBack();
        }
    }

    public final /* synthetic */ void K(View view) {
        if (this.c.canGoForward()) {
            this.c.goForward();
        }
    }

    public final /* synthetic */ void L(View view) {
        this.c.reload();
    }

    public final /* synthetic */ void M(View view) {
        Z();
    }

    public final /* synthetic */ void N(String str, String str2, String str3, String str4, long j) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(str));
        request.setMimeType(str4);
        request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(str));
        request.addRequestHeader("User-Agent", str2);
        request.setDescription("Downloading file...");
        request.setTitle(URLUtil.guessFileName(str, str3, str4));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(1);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(str, str3, str4));
        DownloadManager downloadManager = (DownloadManager) this.b.getSystemService("download");
        if (downloadManager != null) {
            try {
                downloadManager.enqueue(request);
                Toast.makeText(this.b, "Download Started", 0).show();
            } catch (SecurityException unused) {
                Intent intent = new Intent(this.b, (Class<?>) PermissionActivity.class);
                intent.addFlags(268435456);
                this.b.startActivity(intent);
            }
        }
    }

    public final /* synthetic */ boolean O(ChatHeadService chatHeadService, View view, int i, KeyEvent keyEvent) {
        if (i != 4 || keyEvent.getAction() != 1) {
            return false;
        }
        if (this.c.canGoBack()) {
            this.c.goBack();
        } else {
            chatHeadService.v();
        }
        return true;
    }

    public final /* synthetic */ void P(EditText editText, EditText editText2, EditText editText3, DialogInterface dialogInterface, int i) {
        String strTrim = editText.getText().toString().trim();
        String strTrim2 = editText2.getText().toString().trim();
        String strTrim3 = editText3.getText().toString().trim();
        if (strTrim.isEmpty() || strTrim2.isEmpty()) {
            Toast.makeText(this.b, "Title and URL are required", 0).show();
            return;
        }
        if (strTrim3.isEmpty()) {
            strTrim3 = "🌐";
        }
        this.k.c(new DataManager.SpeedDialItem(strTrim, strTrim2, strTrim3));
        S();
    }

    public final /* synthetic */ void Q(String str, DialogInterface dialogInterface, int i) {
        this.k.o(str);
        S();
    }

    public final /* synthetic */ boolean R(String str, boolean z, MenuItem menuItem) {
        ChatHeadService chatHeadService;
        String str2;
        ChatHeadService chatHeadService2;
        String str3;
        ChatHeadService chatHeadService3;
        ChatHeadService chatHeadService4;
        String str4;
        ChatHeadService chatHeadService5;
        String str5;
        switch (menuItem.getItemId()) {
            case 1:
                ChatHeadService chatHeadService6 = this.b;
                if (chatHeadService6 != null) {
                    chatHeadService6.w("about:home");
                    this.b.E();
                }
                return true;
            case 2:
                String title = this.c.getTitle();
                if (str != null && !str.isEmpty()) {
                    if (z) {
                        this.k.n(new DataManager.BookmarkItem(title, str));
                        chatHeadService = this.b;
                        str2 = "Bookmark Removed";
                    } else {
                        DataManager dataManager = this.k;
                        if (title == null) {
                            title = str;
                        }
                        dataManager.a(new DataManager.BookmarkItem(title, str));
                        chatHeadService = this.b;
                        str2 = "Bookmark Added";
                    }
                    Toast.makeText(chatHeadService, str2, 0).show();
                }
                ChatHeadService chatHeadService7 = this.b;
                if (chatHeadService7 != null) {
                    chatHeadService7.E();
                }
                return true;
            case 3:
                WebSettings settings = this.c.getSettings();
                boolean z2 = !this.j.isDesktopSite();
                this.j.setDesktopSite(z2);
                if (z2) {
                    settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    settings.setLoadWithOverviewMode(true);
                    settings.setUseWideViewPort(true);
                    chatHeadService2 = this.b;
                    str3 = "Desktop View";
                } else {
                    settings.setUserAgentString(this.l);
                    settings.setLoadWithOverviewMode(false);
                    settings.setUseWideViewPort(false);
                    chatHeadService2 = this.b;
                    str3 = "Mobile View";
                }
                Toast.makeText(chatHeadService2, str3, 0).show();
                ChatHeadService chatHeadService8 = this.b;
                if (chatHeadService8 != null) {
                    chatHeadService8.H(this.j.getId());
                    chatHeadService3 = this.b;
                    chatHeadService3.E();
                }
                this.c.reload();
                return true;
            case 4:
                ChatHeadService chatHeadService9 = this.b;
                if (chatHeadService9 != null) {
                    chatHeadService9.x("about:home", true);
                    this.b.E();
                }
                return true;
            case 5:
                boolean z3 = !this.k.l();
                this.k.t(z3);
                if (z3) {
                    chatHeadService4 = this.b;
                    str4 = "Ad Blocker Enabled";
                } else {
                    chatHeadService4 = this.b;
                    str4 = "Ad Blocker Disabled";
                }
                Toast.makeText(chatHeadService4, str4, 0).show();
                chatHeadService3 = this.b;
                if (chatHeadService3 != null) {
                    chatHeadService3.E();
                }
                this.c.reload();
                return true;
            case 6:
                boolean z4 = !this.j.isNightMode();
                this.j.setNightMode(z4);
                if (z4) {
                    D(this.c);
                    chatHeadService5 = this.b;
                    str5 = "Night Mode Enabled";
                } else {
                    E(this.c);
                    chatHeadService5 = this.b;
                    str5 = "Night Mode Disabled";
                }
                Toast.makeText(chatHeadService5, str5, 0).show();
                ChatHeadService chatHeadService10 = this.b;
                if (chatHeadService10 != null) {
                    chatHeadService10.H(this.j.getId());
                    this.b.E();
                }
                return true;
            default:
                return false;
        }
    }

    public final void S() {
        DataManager dataManager = this.k;
        if (dataManager == null) {
            return;
        }
        List<DataManager.SpeedDialItem> listK = dataManager.k();
        StringBuilder sb = new StringBuilder();
        for (DataManager.SpeedDialItem speedDialItem : listK) {
            String strReplace = speedDialItem.getTitle().replace("\"", "&quot;");
            String strReplace2 = speedDialItem.getUrl().replace("\"", "&quot;");
            String emoji = speedDialItem.getEmoji() != null ? speedDialItem.getEmoji() : "🌐";
            String str = "https://www.google.com/s2/favicons?sz=128&domain=" + F(strReplace2);
            sb.append("<div class=\"card\" data-url=\"");
            sb.append(strReplace2);
            sb.append("\" data-title=\"");
            sb.append(strReplace);
            sb.append("\" onclick=\"openSite('");
            sb.append(strReplace2);
            sb.append("')\">\n");
            sb.append("  <div class=\"icon-container\">\n");
            sb.append("    <img class=\"favicon-img\" src=\"");
            sb.append(str);
            sb.append("\" onerror=\"this.style.display='none'; this.nextElementSibling.style.display='inline';\" />\n");
            sb.append("    <span class=\"favicon-fallback\" style=\"display: none;\">");
            sb.append(emoji);
            sb.append("</span>\n");
            sb.append("  </div>\n");
            sb.append("  <div class=\"title\">");
            sb.append(strReplace);
            sb.append("</div>\n");
            sb.append("</div>\n");
        }
        this.c.loadDataWithBaseURL("https://localhost/", "<!DOCTYPE html>\n<html>\n<head>\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n    <style>\n        * {\n            box-sizing: border-box;\n            margin: 0;\n            padding: 0;\n            user-select: none;\n            -webkit-user-select: none;\n        }\n        body {\n            background: linear-gradient(135deg, #0f172a, #1e1b4b);\n            color: #f8fafc;\n            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;\n            min-height: 100vh;\n            padding: 24px 16px;\n            display: flex;\n            flex-direction: column;\n            align-items: center;\n        }\n        .header {\n            margin-top: 30px;\n            margin-bottom: 30px;\n            text-align: center;\n        }\n        .header h1 {\n            font-size: 32px;\n            font-weight: 800;\n            background: linear-gradient(to right, #38bdf8, #818cf8);\n            -webkit-background-clip: text;\n            -webkit-text-fill-color: transparent;\n            margin-bottom: 8px;\n        }\n        .header p {\n            font-size: 14px;\n            color: #94a3b8;\n        }\n        .grid {\n            display: grid;\n            grid-template-columns: repeat(4, 1fr);\n            gap: 16px;\n            width: 100%;\n            max-width: 480px;\n            padding: 10px;\n        }\n        @media (max-width: 400px) {\n            .grid {\n                grid-template-columns: repeat(3, 1fr);\n                gap: 12px;\n            }\n        }\n        .card {\n            background: rgba(30, 41, 59, 0.7);\n            border: 1px solid rgba(255, 255, 255, 0.05);\n            backdrop-filter: blur(10px);\n            border-radius: 16px;\n            aspect-ratio: 1 / 1;\n            display: flex;\n            flex-direction: column;\n            align-items: center;\n            justify-content: center;\n            padding: 12px;\n            cursor: pointer;\n            transition: transform 0.2s, background 0.2s;\n            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);\n        }\n        .card:active {\n            transform: scale(0.95);\n            background: rgba(30, 41, 59, 0.9);\n        }\n        .card .icon-container {\n            width: 48px;\n            height: 48px;\n            border-radius: 12px;\n            background: rgba(255, 255, 255, 0.05);\n            display: flex;\n            align-items: center;\n            justify-content: center;\n            font-size: 24px;\n            margin-bottom: 8px;\n            overflow: hidden;\n        }\n        .favicon-img {\n            width: 30px;\n            height: 30px;\n            object-fit: contain;\n        }\n        .favicon-fallback {\n            font-size: 24px;\n        }\n        .card .title {\n            font-size: 12px;\n            font-weight: 500;\n            color: #e2e8f0;\n            text-align: center;\n            white-space: nowrap;\n            overflow: hidden;\n            text-overflow: ellipsis;\n            width: 100%;\n        }\n        .card.add-card {\n            background: rgba(255, 255, 255, 0.02);\n            border: 1px dashed rgba(255, 255, 255, 0.15);\n        }\n        .card.add-card:active {\n            background: rgba(255, 255, 255, 0.05);\n        }\n        .card.add-card .icon-container {\n            background: none;\n            color: #94a3b8;\n            font-size: 28px;\n        }\n        .card.add-card .title {\n            color: #94a3b8;\n        }\n    </style>\n</head>\n<body>\n    <div class=\"header\">\n        <h1>Bubble Browser</h1>\n        <p>Your Favorite Pinned Sites</p>\n    </div>\n    <div class=\"grid\">\n        " + sb.toString() + "\n        <div class=\"card add-card\" onclick=\"Android.showAddDialog()\">\n            <div class=\"icon-container\">+</div>\n            <div class=\"title\">Add Site</div>\n        </div>\n    </div>\n    <script>\n        function openSite(url) {\n            Android.openUrl(url);\n        }\n        function onLongPress(url, title) {\n            Android.showDeleteDialog(url, title);\n        }\n        \n        var cards = document.querySelectorAll('.card:not(.add-card)');\n        cards.forEach(function(card) {\n            var pressTimer = null;\n            var url = card.getAttribute('data-url');\n            var title = card.getAttribute('data-title');\n            \n            function start(e) {\n                if (pressTimer !== null) clearTimeout(pressTimer);\n                pressTimer = window.setTimeout(function() {\n                    onLongPress(url, title);\n                    pressTimer = null;\n                }, 800);\n            }\n            \n            function cancel(e) {\n                if (pressTimer !== null) {\n                    clearTimeout(pressTimer);\n                    pressTimer = null;\n                }\n            }\n            \n            card.addEventListener('mousedown', start);\n            card.addEventListener('touchstart', start);\n            card.addEventListener('click', function(e) {\n                if (pressTimer !== null) {\n                    clearTimeout(pressTimer);\n                    pressTimer = null;\n                }\n            });\n            card.addEventListener('mouseout', cancel);\n            card.addEventListener('touchend', cancel);\n            card.addEventListener('touchmove', cancel);\n        });\n    </script>\n</body>\n</html>", "text/html", "UTF-8", null);
        this.d.setText(RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED);
        this.j.setUrl("about:home");
        this.j.setTitle("New Tab");
        ChatHeadService chatHeadService = this.b;
        if (chatHeadService != null) {
            chatHeadService.H(this.j.getId());
        }
    }

    public final void T(String str) {
        StringBuilder sb;
        if (str == null || str.isEmpty() || str.equals("about:home") || str.startsWith("https://localhost/")) {
            S();
            return;
        }
        if (!str.startsWith("http://")) {
            String str2 = "https://";
            if (!str.startsWith("https://")) {
                if (!str.contains(".") || str.contains(" ")) {
                    DataManager dataManager = this.k;
                    String strJ = dataManager != null ? dataManager.j() : "google";
                    try {
                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this.a.getContext());
                        Bundle bundle = new Bundle();
                        bundle.putString("search_term", str);
                        bundle.putString("search_engine", strJ);
                        firebaseAnalytics.a("search", bundle);
                    } catch (Exception e) {
                        Log.e("BrowserViewController", "Error logging search event", e);
                    }
                    if ("duckduckgo".equals(strJ)) {
                        str2 = "https://duckduckgo.com/?q=";
                    } else if ("bing".equals(strJ)) {
                        str2 = "https://www.bing.com/search?q=";
                    } else {
                        str2 = "yahoo".equals(strJ) ? "https://search.yahoo.com/search?p=" : "https://www.google.com/search?q=";
                    }
                    try {
                        str = str2 + URLEncoder.encode(str, "UTF-8");
                    } catch (UnsupportedEncodingException unused) {
                        sb = new StringBuilder();
                        sb.append(str2);
                        sb.append(str);
                        str = sb.toString();
                    }
                } else {
                    sb = new StringBuilder();
                }
                sb.append(str2);
                sb.append(str);
                str = sb.toString();
            }
        }
        this.c.loadUrl(str);
    }

    public final void U() {
        String str;
        if (this.o > 0 && (str = this.p) != null && !str.isEmpty() && !"about:home".equals(this.p) && !this.p.startsWith("https://localhost/")) {
            long jCurrentTimeMillis = System.currentTimeMillis() - this.o;
            if (jCurrentTimeMillis > 100) {
                try {
                    FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this.a.getContext());
                    Bundle bundle = new Bundle();
                    bundle.putString(ImagesContract.URL, this.p);
                    String str2 = this.q;
                    if (str2 == null) {
                        str2 = RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED;
                    }
                    bundle.putString("title", str2);
                    bundle.putLong("duration_ms", jCurrentTimeMillis);
                    firebaseAnalytics.a("page_view_duration", bundle);
                } catch (Exception e) {
                    Log.e("BrowserViewController", "Error logging page_view_duration", e);
                }
            }
        }
        this.o = 0L;
        this.p = null;
        this.q = null;
    }

    public void V() {
        U();
        WebView webView = this.c;
        if (webView != null) {
            try {
                webView.stopLoading();
                this.c.clearHistory();
                this.c.destroy();
            } catch (Exception e) {
                Log.e("BrowserViewController", "Error destroying WebView", e);
            }
        }
    }

    public void W(int i) {
        if (this.e.getProgressDrawable() != null) {
            this.e.getProgressDrawable().setTint(i);
        }
    }

    public final void X() {
        Window window;
        int i;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.a.getContext(), R$style.AppTheme);
        Zi zi = new Zi(contextThemeWrapper);
        zi.A("Add Pinned Site");
        View viewInflate = LayoutInflater.from(contextThemeWrapper).inflate(R$layout.dialog_add_speed_dial, (ViewGroup) null);
        final EditText editText = (EditText) viewInflate.findViewById(R$id.edit_site_title);
        final EditText editText2 = (EditText) viewInflate.findViewById(R$id.edit_site_url);
        final EditText editText3 = (EditText) viewInflate.findViewById(R$id.edit_site_emoji);
        zi.B(viewInflate);
        zi.x("Add", new DialogInterface.OnClickListener() { // from class: R3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                this.c.P(editText, editText2, editText3, dialogInterface, i2);
            }
        });
        zi.v("Cancel", (DialogInterface.OnClickListener) null);
        androidx.appcompat.app.a aVarA = zi.a();
        if (Build.VERSION.SDK_INT >= 26) {
            window = aVarA.getWindow();
            i = 2038;
        } else {
            window = aVarA.getWindow();
            i = 2003;
        }
        window.setType(i);
        aVarA.show();
    }

    public final void Y(final String str, String str2) {
        Window window;
        int i;
        Zi zi = new Zi(new ContextThemeWrapper(this.a.getContext(), R$style.AppTheme));
        zi.A("Remove Pinned Site");
        zi.u("Are you sure you want to remove " + str2 + "?");
        zi.x("Remove", new DialogInterface.OnClickListener() { // from class: Q3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                this.c.Q(str, dialogInterface, i2);
            }
        });
        zi.v("Cancel", (DialogInterface.OnClickListener) null);
        androidx.appcompat.app.a aVarA = zi.a();
        if (Build.VERSION.SDK_INT >= 26) {
            window = aVarA.getWindow();
            i = 2038;
        } else {
            window = aVarA.getWindow();
            i = 2003;
        }
        window.setType(i);
        aVarA.show();
    }

    public final void Z() {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this.a.getContext(), R$style.CustomPopupMenuTheme), this.i);
        final String url = this.c.getUrl();
        final boolean zM = this.k.m(url);
        Context context = this.a.getContext();
        Drawable drawable = M7.getDrawable(context, R$drawable.ic_add_tab_modern);
        if (drawable != null) {
            drawable.setTint(-1);
        }
        Drawable drawable2 = M7.getDrawable(context, R$drawable.ic_star_modern);
        if (drawable2 != null) {
            drawable2.setTint(zM ? Color.parseColor("#FFD700") : -1);
        }
        Drawable drawable3 = M7.getDrawable(context, R$drawable.ic_desktop_modern);
        if (drawable3 != null) {
            drawable3.setTint(-1);
        }
        Drawable drawable4 = M7.getDrawable(context, R$drawable.incognito);
        if (drawable4 != null) {
            drawable4.setTint(-1);
        }
        Drawable drawable5 = M7.getDrawable(context, R$drawable.ic_privacy_modern);
        if (drawable5 != null) {
            drawable5.setTint(this.k.l() ? Color.parseColor("#4CAF50") : -1);
        }
        Drawable drawable6 = M7.getDrawable(context, R$drawable.ic_night_mode);
        if (drawable6 != null) {
            drawable6.setTint(this.j.isNightMode() ? Color.parseColor("#9C27B0") : -1);
        }
        popupMenu.getMenu().add(0, 1, 0, "New Tab").setIcon(drawable);
        popupMenu.getMenu().add(0, 4, 0, "New Incognito Tab").setIcon(drawable4);
        popupMenu.getMenu().add(0, 2, 0, zM ? "Remove Bookmark" : "Add Bookmark").setIcon(drawable2);
        MenuItem menuItemAdd = popupMenu.getMenu().add(0, 3, 0, "Desktop Site");
        menuItemAdd.setIcon(drawable3);
        menuItemAdd.setCheckable(true);
        menuItemAdd.setChecked(this.j.isDesktopSite());
        if (this.j.isDesktopSite() && drawable3 != null) {
            drawable3.setTint(Color.parseColor("#4CAF50"));
        }
        MenuItem menuItemAdd2 = popupMenu.getMenu().add(0, 5, 0, "Block Ads");
        menuItemAdd2.setIcon(drawable5);
        menuItemAdd2.setCheckable(true);
        menuItemAdd2.setChecked(this.k.l());
        MenuItem menuItemAdd3 = popupMenu.getMenu().add(0, 6, 0, "Night Mode");
        menuItemAdd3.setIcon(drawable6);
        menuItemAdd3.setCheckable(true);
        menuItemAdd3.setChecked(this.j.isNightMode());
        try {
            Field declaredField = popupMenu.getClass().getDeclaredField("mPopup");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(popupMenu);
            Class.forName(obj.getClass().getName()).getMethod("setForceShowIcon", Boolean.TYPE).invoke(obj, Boolean.TRUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: P3
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return this.a.R(url, zM, menuItem);
            }
        });
        popupMenu.show();
    }

    public final void a0(String str) {
        View view = this.a;
        if (view == null) {
            return;
        }
        view.post(new c(str));
    }

    public final void b0() {
        if (this.f != null) {
            boolean zCanGoBack = this.c.canGoBack();
            this.f.setEnabled(zCanGoBack);
            this.f.setAlpha(zCanGoBack ? 1.0f : 0.4f);
        }
        if (this.g != null) {
            boolean zCanGoForward = this.c.canGoForward();
            this.g.setEnabled(zCanGoForward);
            this.g.setAlpha(zCanGoForward ? 1.0f : 0.4f);
        }
    }
}

================================================================
