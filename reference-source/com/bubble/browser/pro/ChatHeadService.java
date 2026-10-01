package com.bubble.browser.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.browser.browser.pro.R$drawable;
import com.browser.browser.pro.R$layout;
import com.browser.browser.pro.R$string;
import com.browser.browser.pro.R$style;
import com.flipkart.chatheads.ui.ChatHead;
import com.google.firebase.analytics.FirebaseAnalytics;
import defpackage.AbstractC0425ul;
import defpackage.C0402sy;
import defpackage.M7;
import defpackage.S4;
import defpackage.W3;
import defpackage.W4;
import defpackage.b5;
import defpackage.b9;
import defpackage.bg;
import defpackage.xl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class ChatHeadService extends Service {
    public static ChatHeadService t;
    public b9 o;
    public C0402sy p;
    public DataManager r;
    public W3 s;
    public final List c = new ArrayList();
    public int m = 0;
    public final IBinder n = new d();
    public Map q = new HashMap();

    public class a implements b5 {
        public a() {
        }

        /* JADX WARN: Code duplicated, block: B:14:0x0048  */
        /* JADX WARN: Code duplicated, block: B:16:0x0050  */
        /* JADX WARN: Code duplicated, block: B:18:0x0056  */
        @Override // defpackage.b5
        /* JADX INFO: renamed from: e, reason: merged with bridge method [inline-methods] */
        public View a(String str, ChatHead chatHead, ViewGroup viewGroup) {
            Object obj;
            c cVarO = ChatHeadService.this.o(str, viewGroup);
            if (cVarO == null) {
                return new View(ChatHeadService.this);
            }
            if (cVarO.a.getParent() != null) {
                ((ViewGroup) cVarO.a.getParent()).removeView(cVarO.a);
            }
            viewGroup.addView(cVarO.a);
            if (str.startsWith("browser_tab_")) {
                Object obj2 = cVarO.b;
                if (obj2 instanceof com.bubble.browser.pro.a) {
                    ((com.bubble.browser.pro.a) obj2).W(ChatHeadService.this.s.a(str));
                } else if ("browser_manager".equals(str)) {
                    obj = cVarO.b;
                    if (obj instanceof BrowserManagerViewController) {
                        ((BrowserManagerViewController) obj).x(ChatHeadService.this.c);
                    }
                }
            } else if ("browser_manager".equals(str)) {
                obj = cVarO.b;
                if (obj instanceof BrowserManagerViewController) {
                    ((BrowserManagerViewController) obj).x(ChatHeadService.this.c);
                }
            }
            return cVarO.a;
        }

        @Override // defpackage.b5
        /* JADX INFO: renamed from: f, reason: merged with bridge method [inline-methods] */
        public void b(String str, ChatHead chatHead, ViewGroup viewGroup) {
            View view;
            c cVar = (c) ChatHeadService.this.q.get(str);
            if (cVar == null || (view = cVar.a) == null) {
                return;
            }
            viewGroup.removeView(view);
        }

        @Override // defpackage.b5
        /* JADX INFO: renamed from: g, reason: merged with bridge method [inline-methods] */
        public Drawable c(String str) {
            return ChatHeadService.this.m(str);
        }

        @Override // defpackage.b5
        /* JADX INFO: renamed from: h, reason: merged with bridge method [inline-methods] */
        public void d(String str, ChatHead chatHead, ViewGroup viewGroup) {
            c cVar = (c) ChatHeadService.this.q.get(str);
            if (cVar != null && cVar.a != null) {
                Object obj = cVar.b;
                if (obj instanceof com.bubble.browser.pro.a) {
                    try {
                        ((com.bubble.browser.pro.a) obj).V();
                    } catch (Exception e) {
                        Log.e("ChatHeadService", "Error destroying controller", e);
                    }
                }
                ChatHeadService.this.q.remove(str);
                viewGroup.removeView(cVar.a);
            }
            ChatHeadService.this.G();
        }
    }

    public static class c {
        public View a;
        public Object b;

        public c(View view, Object obj) {
            this.a = view;
            this.b = obj;
        }
    }

    public class d extends Binder {
        public d() {
        }

        public ChatHeadService a() {
            return ChatHeadService.this;
        }
    }

    public final void A(String str) {
        for (int i = 0; i < this.c.size(); i++) {
            if (((BrowserTab) this.c.get(i)).getId().equals(str)) {
                this.c.remove(i);
                break;
            }
        }
        B();
        G();
    }

    public void B() {
        this.r.p(this.c);
    }

    public void C(boolean z) {
        C0402sy c0402sy = this.p;
        if (c0402sy != null) {
            c0402sy.w(z ? 0 : 8);
        }
    }

    public void D() {
        Intent intent = new Intent(this, (Class<?>) AdDisplayActivity.class);
        intent.addFlags(268435456);
        startActivity(intent);
    }

    public void E() {
        int i = this.m + 1;
        this.m = i;
        if (i >= 1) {
            this.m = 0;
            D();
        }
    }

    public void F() {
        b9 b9Var;
        Class cls = com.flipkart.chatheads.ui.b.class;
        if (this.o.w().isEmpty()) {
            y();
            this.o.t(cls, null);
            return;
        }
        if (this.o.A() instanceof com.flipkart.chatheads.ui.b) {
            b9Var = this.o;
            cls = com.flipkart.chatheads.ui.a.class;
        } else {
            b9Var = this.o;
        }
        b9Var.t(cls, null);
    }

    public final void G() {
        c cVar = (c) this.q.get("browser_manager");
        if (cVar != null) {
            Object obj = cVar.b;
            if (obj instanceof BrowserManagerViewController) {
                ((BrowserManagerViewController) obj).x(this.c);
            }
        }
        B();
    }

    public void H(String str) {
        B();
        G();
    }

    public void i(String str) {
        this.o.h(str, true);
    }

    public void j(String str) {
        i(str);
        A(str);
    }

    public final Bitmap k(Drawable drawable) {
        return l(drawable, 100, 40);
    }

    public final Bitmap l(Drawable drawable, int i, int i2) {
        int i3 = (i - i2) / 2;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        int i4 = i2 + i3;
        drawable.setBounds(i3, i3, i4, i4);
        drawable.draw(canvas);
        return bitmapCreateBitmap;
    }

    public final Drawable m(String str) {
        BrowserTab browserTab;
        com.flipkart.circularImageView.a aVar = new com.flipkart.circularImageView.a();
        int iA = this.s.a(str);
        if ("browser_manager".equals(str)) {
            aVar.d(new bg(l(M7.getDrawable(this, R$drawable.ic_browser_manager), 100, 50), iA));
        } else if (str.startsWith("browser_tab_")) {
            Iterator it = this.c.iterator();
            do {
                if (!it.hasNext()) {
                    browserTab = null;
                    break;
                }
                browserTab = (BrowserTab) it.next();
            } while (!browserTab.getId().equals(str));
            if (browserTab != null && browserTab.isIncognito()) {
                iA = Color.parseColor("#1E1E1E");
            }
            if (browserTab == null || browserTab.getFavicon() == null) {
                aVar.d(new bg(l(M7.getDrawable(this, this.s.b(str)), 100, 50), iA));
            } else {
                aVar.d(new bg(l(new BitmapDrawable(getResources(), browserTab.getFavicon()), 100, 50), iA));
            }
        } else {
            aVar.d(new bg(k(M7.getDrawable(this, this.s.b(str))), iA));
        }
        aVar.e(-1, 3.0f);
        return aVar;
    }

    public final Notification n() {
        int i = Build.VERSION.SDK_INT;
        if (i >= 26) {
            NotificationChannel notificationChannelA = AbstractC0425ul.a("notes_app_channel", "Browser Bubble", 2);
            notificationChannelA.setDescription("Keeps floating browser tabs running");
            ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannelA);
        }
        xl.e eVarH = new xl.e(this, "notes_app_channel").p(R$drawable.ic_browser_manager).j(getString(R$string.app_name)).i("Your floating browser tabs are running.").h(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) FloatingActivity.class), 201326592));
        if (i >= 26) {
            eVarH.g("notes_app_channel");
        }
        return eVarH.b();
    }

    public final c o(String str, ViewGroup viewGroup) {
        c cVar;
        BrowserTab browserTab;
        c cVar2 = (c) this.q.get(str);
        if (cVar2 != null) {
            return cVar2;
        }
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(new ContextThemeWrapper(this, R$style.AppTheme));
        if (str.startsWith("browser_tab_")) {
            View viewInflate = layoutInflaterFrom.inflate(R$layout.browser_fragment, viewGroup, false);
            Iterator it = this.c.iterator();
            do {
                if (!it.hasNext()) {
                    browserTab = null;
                    break;
                }
                browserTab = (BrowserTab) it.next();
            } while (!browserTab.getId().equals(str));
            if (browserTab == null) {
                browserTab = new BrowserTab(str, "about:home");
                this.c.add(browserTab);
            }
            cVar = new c(viewInflate, new com.bubble.browser.pro.a(viewInflate, this, browserTab));
        } else {
            if (!"browser_manager".equals(str)) {
                return cVar2;
            }
            View viewInflate2 = layoutInflaterFrom.inflate(R$layout.browser_manager_fragment, viewGroup, false);
            cVar = new c(viewInflate2, new BrowserManagerViewController(viewInflate2, this));
        }
        this.q.put(str, cVar);
        return cVar;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.n;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        t = this;
        this.s = new W3(this);
        this.r = new DataManager(this);
        C0402sy c0402sy = new C0402sy(this);
        this.p = c0402sy;
        this.o = new b9(this, c0402sy);
        z0.d(this);
        this.o.N(new a());
        q();
        y();
        this.o.M(new b());
        this.o.t(com.flipkart.chatheads.ui.b.class, null);
        u();
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        if (t == this) {
            t = null;
        }
        for (c cVar : this.q.values()) {
            if (cVar != null) {
                Object obj = cVar.b;
                if (obj instanceof com.bubble.browser.pro.a) {
                    try {
                        ((com.bubble.browser.pro.a) obj).V();
                    } catch (Exception e) {
                        Log.e("ChatHeadService", "Error destroying controller in onDestroy", e);
                    }
                }
            }
        }
        this.q.clear();
        this.p.p();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        return super.onStartCommand(intent, i, i2);
    }

    public boolean p(String str) {
        return this.o.F(str) != null;
    }

    public final void q() {
        List listF = this.r.f();
        if (listF == null || listF.isEmpty()) {
            return;
        }
        this.c.clear();
        this.c.addAll(listF);
    }

    public void r(int i) {
        s("browser_manager");
        c cVar = (c) this.q.get("browser_manager");
        if (cVar != null) {
            Object obj = cVar.b;
            if (obj instanceof BrowserManagerViewController) {
                ((BrowserManagerViewController) obj).v(i);
            }
        }
    }

    public void s(String str) {
        if (this.o.F(str) == null) {
            this.o.E(str, false, true);
            this.o.j(str);
        }
        this.o.t(com.flipkart.chatheads.ui.a.class, null);
        this.o.J(str);
    }

    public void t() {
        this.o.t(com.flipkart.chatheads.ui.b.class, null);
    }

    public final void u() {
        Notification notificationN = n();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(1, notificationN, 1073741824);
            } else {
                startForeground(1, notificationN);
            }
        } catch (Exception e) {
            Log.e("ChatHeadService", "Failed to start foreground service", e);
        }
    }

    public void v() {
        if (this.o.A() instanceof com.flipkart.chatheads.ui.a) {
            D();
            t();
        }
    }

    public String w(String str) {
        return x(str, false);
    }

    public String x(String str, boolean z) {
        String str2 = "browser_tab_" + System.currentTimeMillis();
        if (str == null) {
            str = "about:home";
        }
        this.c.add(new BrowserTab(str2, str, z));
        this.o.E(str2, false, true);
        this.o.j(str2);
        o(str2, null);
        if (z) {
            D();
        }
        B();
        G();
        return str2;
    }

    public void y() {
        if (this.o.F("browser_manager") == null) {
            this.o.E("browser_manager", true, true);
            this.o.j("browser_manager");
        }
        int i = 0;
        for (BrowserTab browserTab : this.c) {
            if (i >= 4) {
                break;
            }
            if (this.o.F(browserTab.getId()) == null) {
                this.o.E(browserTab.getId(), false, false);
                this.o.j(browserTab.getId());
                o(browserTab.getId(), null);
                i++;
            }
        }
        u();
        try {
            startService(new Intent(this, (Class<?>) ChatHeadService.class));
        } catch (Exception unused) {
        }
    }

    public void z(String str) {
        b9 b9Var = this.o;
        if (b9Var == null || b9Var.F(str) == null) {
            return;
        }
        this.o.j(str);
    }

    public class b implements W4 {
        public b() {
        }

        @Override // defpackage.W4
        public void c(ChatHead chatHead) {
        }

        @Override // defpackage.W4
        public void d(ChatHead chatHead) {
        }

        @Override // defpackage.W4
        /* JADX INFO: renamed from: g, reason: merged with bridge method [inline-methods] */
        public void f(String str) {
            try {
                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(ChatHeadService.this);
                Bundle bundle = new Bundle();
                bundle.putString("bubble_id", str);
                bundle.putString("bubble_type", str.equals("browser_manager") ? "manager" : "tab");
                firebaseAnalytics.a("bubble_created", bundle);
            } catch (Exception e) {
                Log.e("ChatHeadService", "Error logging bubble_created", e);
            }
        }

        @Override // defpackage.W4
        /* JADX INFO: renamed from: h, reason: merged with bridge method [inline-methods] */
        public void b(String str) {
        }

        @Override // defpackage.W4
        /* JADX INFO: renamed from: i, reason: merged with bridge method [inline-methods] */
        public void a(String str, boolean z) {
            if (str.startsWith("browser_tab_") && z && ChatHeadService.this.o.F("browser_manager") != null) {
                ChatHeadService.this.A(str);
            }
            if (ChatHeadService.this.o.w().isEmpty()) {
                ChatHeadService.this.stopSelf();
            }
        }

        @Override // defpackage.W4
        public void e(S4 s4, S4 s5) {
        }
    }
}

================================================================
