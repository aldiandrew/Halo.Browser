package com.bubble.browser.pro;

import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.browser.browser.pro.R$drawable;
import com.browser.browser.pro.R$id;
import com.browser.browser.pro.R$layout;
import com.browser.browser.pro.R$style;
import com.bubble.browser.pro.BrowserManagerViewController;
import com.bubble.browser.pro.BrowserTab;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.tabs.TabLayout;
import defpackage.C0452w0;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class BrowserManagerViewController {
    public final View a;
    public final ChatHeadService b;
    public TabLayout c;
    public ViewPager2 d;
    public c e;
    public DataManager g;
    public e i;
    public List f = new ArrayList();
    public Mode h = Mode.TABS;
    public final Map j = new HashMap();
    public final Map k = new HashMap();

    public enum Mode {
        TABS,
        BOOKMARKS,
        HISTORY,
        DOWNLOADS
    }

    public class a extends RecyclerView.Adapter {
        public List d;

        public a() {
            this.d = new ArrayList();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int d() {
            return this.d.size();
        }

        public final /* synthetic */ void w(b bVar, View view) {
            BrowserManagerViewController.this.b.w(bVar.b);
            BrowserManagerViewController.this.b.E();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: x, reason: merged with bridge method [inline-methods] */
        public void k(f fVar, int i) {
            final b bVar = (b) this.d.get(i);
            fVar.u.setText(bVar.a);
            fVar.v.setText(bVar.b);
            fVar.w.setVisibility(8);
            fVar.x.setImageResource(bVar.c);
            fVar.x.setColorFilter(Color.parseColor("#B0B0B0"));
            fVar.u.setTextColor(-1);
            fVar.v.setTextColor(Color.parseColor("#B0B0B0"));
            fVar.a.setOnClickListener(new View.OnClickListener() { // from class: C3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.c.w(bVar, view);
                }
            });
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: y, reason: merged with bridge method [inline-methods] */
        public f m(ViewGroup viewGroup, int i) {
            return new f(LayoutInflater.from(viewGroup.getContext()).inflate(R$layout.item_browser_tab, viewGroup, false));
        }

        public void z(List list) {
            this.d = list;
            i();
        }
    }

    public static class b {
        public String a;
        public String b;
        public int c;

        public b(String str, String str2, int i) {
            this.a = str;
            this.b = str2;
            this.c = i;
        }
    }

    public class c extends RecyclerView.Adapter {
        public c() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int d() {
            return Mode.values().length;
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: v, reason: merged with bridge method [inline-methods] */
        public void k(d dVar, int i) {
            Mode mode = Mode.values()[i];
            Mode mode2 = Mode.TABS;
            Object[] objArr = 0;
            if (mode == mode2) {
                if (BrowserManagerViewController.this.i == null) {
                    BrowserManagerViewController browserManagerViewController = BrowserManagerViewController.this;
                    browserManagerViewController.i = new e();
                }
                C0452w0 c0452w0 = (C0452w0) BrowserManagerViewController.this.k.get(mode2);
                if (c0452w0 == null) {
                    c0452w0 = new C0452w0(dVar.a.getContext(), BrowserManagerViewController.this.i, 4);
                    BrowserManagerViewController.this.k.put(mode2, c0452w0);
                } else {
                    c0452w0.y();
                }
                dVar.u.setAdapter(c0452w0);
                return;
            }
            a aVar = (a) BrowserManagerViewController.this.j.get(mode);
            if (aVar == null) {
                aVar = new a();
                BrowserManagerViewController.this.j.put(mode, aVar);
            }
            aVar.z(BrowserManagerViewController.this.n(mode));
            C0452w0 c0452w1 = (C0452w0) BrowserManagerViewController.this.k.get(mode);
            if (c0452w1 == null) {
                c0452w1 = new C0452w0(dVar.a.getContext(), aVar, 4);
                BrowserManagerViewController.this.k.put(mode, c0452w1);
            } else {
                c0452w1.y();
            }
            dVar.u.setAdapter(c0452w1);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: w, reason: merged with bridge method [inline-methods] */
        public d m(ViewGroup viewGroup, int i) {
            RecyclerView recyclerView = new RecyclerView(viewGroup.getContext());
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            recyclerView.setLayoutManager(new LinearLayoutManager(viewGroup.getContext()));
            recyclerView.setPadding(32, 32, 32, 32);
            recyclerView.setClipToPadding(false);
            return new d(recyclerView);
        }
    }

    public static class d extends RecyclerView.E {
        public RecyclerView u;

        public d(View view) {
            super(view);
            this.u = (RecyclerView) view;
        }
    }

    public class e extends RecyclerView.Adapter {
        public e() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: A, reason: merged with bridge method [inline-methods] */
        public f m(ViewGroup viewGroup, int i) {
            return new f(LayoutInflater.from(viewGroup.getContext()).inflate(R$layout.item_browser_tab, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int d() {
            return BrowserManagerViewController.this.f.size();
        }

        public final /* synthetic */ void x(BrowserTab browserTab, View view) {
            BrowserManagerViewController.this.b.s(browserTab.getId());
        }

        public final /* synthetic */ void y(BrowserTab browserTab, View view) {
            BrowserManagerViewController.this.b.j(browserTab.getId());
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        /* JADX INFO: renamed from: z, reason: merged with bridge method [inline-methods] */
        public void k(f fVar, int i) {
            final BrowserTab browserTab = (BrowserTab) BrowserManagerViewController.this.f.get(i);
            boolean zP = BrowserManagerViewController.this.b.p(browserTab.getId());
            fVar.u.setText(browserTab.getTitle() != null ? browserTab.getTitle() : "New Tab");
            fVar.v.setText(browserTab.getUrl());
            fVar.a.setOnClickListener(new View.OnClickListener() { // from class: F3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.c.x(browserTab, view);
                }
            });
            fVar.w.setOnClickListener(new View.OnClickListener() { // from class: G3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.c.y(browserTab, view);
                }
            });
            fVar.a.setAlpha(zP ? 1.0f : 0.6f);
            if (browserTab.isIncognito()) {
                fVar.x.setImageResource(R$drawable.incognito);
                fVar.x.setColorFilter(-1);
                fVar.a.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3C4043")));
            } else {
                fVar.x.setColorFilter((ColorFilter) null);
                if (browserTab.getFavicon() != null) {
                    fVar.x.setImageBitmap(browserTab.getFavicon());
                } else {
                    fVar.x.setImageResource(R$drawable.ic_browser_bubble);
                    fVar.x.setColorFilter(Color.parseColor("#B0B0B0"));
                }
                fVar.a.setBackgroundTintList(null);
            }
            fVar.u.setTextColor(-1);
            fVar.v.setTextColor(Color.parseColor("#B0B0B0"));
        }
    }

    public static class f extends RecyclerView.E {
        public TextView u;
        public TextView v;
        public ImageButton w;
        public ImageView x;

        public f(View view) {
            super(view);
            this.u = (TextView) view.findViewById(R$id.tab_title);
            this.v = (TextView) view.findViewById(R$id.tab_url);
            this.w = (ImageButton) view.findViewById(R$id.btn_close_tab);
            this.x = (ImageView) view.findViewById(R$id.tab_icon);
        }
    }

    public BrowserManagerViewController(View view, ChatHeadService chatHeadService) {
        this.a = view;
        this.b = chatHeadService;
        this.g = new DataManager(view.getContext());
        o();
    }

    public static /* synthetic */ void p(TabLayout.f fVar, int i) {
        int i2;
        if (i == 0) {
            fVar.s("Recent");
            i2 = R$drawable.ic_add_tab_modern;
        } else if (i == 1) {
            fVar.s("Bookmarks");
            i2 = R$drawable.ic_star_modern;
        } else if (i == 2) {
            fVar.s("History");
            i2 = R$drawable.ic_history_modern;
        } else {
            if (i != 3) {
                return;
            }
            fVar.s("Downloads");
            i2 = R$drawable.ic_download_modern;
        }
        fVar.p(i2);
    }

    public static /* synthetic */ int u(BrowserTab browserTab, BrowserTab browserTab2) {
        return browserTab2.getId().compareTo(browserTab.getId());
    }

    public final List n(Mode mode) {
        DownloadManager downloadManager;
        ArrayList arrayList = new ArrayList();
        if (mode == Mode.BOOKMARKS) {
            for (DataManager.BookmarkItem bookmarkItem : this.g.g()) {
                arrayList.add(new b(bookmarkItem.getTitle(), bookmarkItem.getUrl(), R$drawable.ic_star_modern));
            }
        } else if (mode == Mode.HISTORY) {
            for (DataManager.HistoryItem historyItem : this.g.i()) {
                arrayList.add(new b(historyItem.getTitle(), historyItem.getUrl(), R$drawable.ic_history_modern));
            }
        } else if (mode == Mode.DOWNLOADS && (downloadManager = (DownloadManager) this.b.getSystemService("download")) != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterByStatus(11);
            try {
                Cursor cursorQuery = downloadManager.query(query);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            int columnIndex = cursorQuery.getColumnIndex("title");
                            int columnIndex2 = cursorQuery.getColumnIndex("local_uri");
                            do {
                                arrayList.add(new b(cursorQuery.getString(columnIndex), cursorQuery.getString(columnIndex2), R$drawable.ic_download_modern));
                            } while (cursorQuery.moveToNext());
                        }
                    } catch (Throwable th) {
                        try {
                            cursorQuery.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return arrayList;
    }

    public final void o() {
        this.c = this.a.findViewById(R$id.manager_tab_layout);
        this.d = (ViewPager2) this.a.findViewById(R$id.manager_view_pager);
        c cVar = new c();
        this.e = cVar;
        this.d.setAdapter(cVar);
        new com.google.android.material.tabs.b(this.c, this.d, new com.google.android.material.tabs.b.b() { // from class: x3
            public final void a(TabLayout.f fVar, int i) {
                BrowserManagerViewController.p(fVar, i);
            }
        }).a();
        z0.g(this.a.findViewById(R$id.manager_native_template), this.b, (FrameLayout) this.a.findViewById(R$id.manager_native_ad_container));
        ((ImageButton) this.a.findViewById(R$id.btn_add_tab)).setOnClickListener(new View.OnClickListener() { // from class: y3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.q(view);
            }
        });
        ((ImageButton) this.a.findViewById(R$id.btn_add_incognito_tab)).setOnClickListener(new View.OnClickListener() { // from class: z3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.r(view);
            }
        });
        ((ImageButton) this.a.findViewById(R$id.btn_clear_data)).setOnClickListener(new View.OnClickListener() { // from class: A3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.s(view);
            }
        });
    }

    public final /* synthetic */ void q(View view) {
        this.b.w("about:home");
        this.b.E();
    }

    public final /* synthetic */ void r(View view) {
        this.b.x("about:home", true);
        this.b.E();
    }

    public final /* synthetic */ void s(View view) {
        w();
    }

    public final /* synthetic */ void t(MaterialCheckBox materialCheckBox, MaterialCheckBox materialCheckBox2, MaterialCheckBox materialCheckBox3, DialogInterface dialogInterface, int i) {
        boolean z;
        a aVar;
        boolean z2 = true;
        if (materialCheckBox.isChecked()) {
            this.g.e();
            z = true;
        } else {
            z = false;
        }
        if (materialCheckBox2.isChecked()) {
            this.g.d();
            z = true;
        }
        if (materialCheckBox3.isChecked()) {
            CookieManager.getInstance().removeAllCookies(null);
            WebStorage.getInstance().deleteAllData();
        } else {
            z2 = z;
        }
        if (z2) {
            Toast.makeText(this.b, "Data Cleared", 0).show();
            e eVar = this.i;
            if (eVar != null) {
                eVar.i();
            }
            for (Mode mode : Mode.values()) {
                if (mode != Mode.TABS && (aVar = (a) this.j.get(mode)) != null) {
                    aVar.z(n(mode));
                }
            }
            this.b.D();
        }
    }

    public void v(int i) {
        c cVar;
        if (this.d == null || (cVar = this.e) == null || i < 0 || i >= cVar.d()) {
            return;
        }
        this.d.j(i, false);
    }

    public final void w() {
        Window window;
        int i;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.b, R$style.AppTheme);
        Zi zi = new Zi(contextThemeWrapper);
        View viewInflate = LayoutInflater.from(contextThemeWrapper).inflate(R$layout.dialog_clear_data_custom, (ViewGroup) null);
        final MaterialCheckBox materialCheckBoxFindViewById = viewInflate.findViewById(R$id.cb_history);
        final MaterialCheckBox materialCheckBoxFindViewById2 = viewInflate.findViewById(R$id.cb_bookmarks);
        final MaterialCheckBox materialCheckBoxFindViewById3 = viewInflate.findViewById(R$id.cb_cookies);
        zi.A("Clear Browsing Data");
        zi.B(viewInflate);
        zi.x("Clear", new DialogInterface.OnClickListener() { // from class: B3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                this.c.t(materialCheckBoxFindViewById, materialCheckBoxFindViewById2, materialCheckBoxFindViewById3, dialogInterface, i2);
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

    public void x(List list) {
        ArrayList arrayList = new ArrayList(list);
        Collections.sort(arrayList, new Comparator() { // from class: w3
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return BrowserManagerViewController.u((BrowserTab) obj, (BrowserTab) obj2);
            }
        });
        this.f = arrayList;
        e eVar = this.i;
        if (eVar != null) {
            eVar.i();
        }
    }
}

================================================================
