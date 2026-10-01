package com.bubble.browser.pro;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.browser.browser.pro.R$color;
import com.browser.browser.pro.R$drawable;
import com.browser.browser.pro.R$id;
import com.browser.browser.pro.R$layout;
import com.bubble.browser.pro.FloatingActivity;
import defpackage.AbstractC0320nd;
import defpackage.S1;
import defpackage.W3;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class FloatingActivity extends Activity implements View.OnClickListener {
    public Button c;
    public TextView m;
    public ImageView n;
    public ChatHeadService o;
    public boolean p;
    public String q = null;
    public ServiceConnection r = new a();
    public W3 s;

    public class a implements ServiceConnection {
        public a() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FloatingActivity.this.o = ((ChatHeadService.d) iBinder).a();
            FloatingActivity.this.p = true;
            if (FloatingActivity.this.q != null) {
                FloatingActivity.this.o.s(FloatingActivity.this.o.w(FloatingActivity.this.q));
                FloatingActivity.this.q = null;
            } else {
                FloatingActivity.this.o.t();
            }
            FloatingActivity.this.P();
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            FloatingActivity.this.p = false;
            FloatingActivity.this.P();
        }
    }

    public class b implements Kk {
        public b() {
        }

        public void a() {
            if (FloatingActivity.this.o != null) {
                FloatingActivity.this.o.C(true);
                FloatingActivity.this.o.F();
            }
        }
    }

    public static /* synthetic */ void x(int[] iArr, DialogInterface dialogInterface, int i) {
        iArr[0] = i;
    }

    public final /* synthetic */ void A(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        M();
    }

    public final /* synthetic */ void B(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        J();
    }

    public final /* synthetic */ void C(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        H();
    }

    public final /* synthetic */ void D(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        K();
    }

    public final /* synthetic */ void E(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        L();
    }

    public final void F() {
        RoleManager roleManagerA;
        if (Build.VERSION.SDK_INT < 29 || (roleManagerA = AbstractC0320nd.a(getSystemService("role"))) == null || !roleManagerA.isRoleAvailable("android.app.role.BROWSER")) {
            G();
        } else if (roleManagerA.isRoleHeld("android.app.role.BROWSER")) {
            Toast.makeText(this, "Bubble Browser is already your default browser", 0).show();
        } else {
            startActivityForResult(roleManagerA.createRequestRoleIntent("android.app.role.BROWSER"), 200);
        }
    }

    public final void G() {
        try {
            try {
                startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
                Toast.makeText(this, "Please select Bubble Browser as your default browser in settings", 1).show();
            } catch (Exception unused) {
                Toast.makeText(this, "Could not open settings", 0).show();
            }
        } catch (Exception unused2) {
            startActivity(new Intent("android.settings.SETTINGS"));
            Toast.makeText(this, "Please search for 'Default apps' in settings and set Bubble Browser", 1).show();
        }
    }

    public final void H() {
        try {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException unused) {
            startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    public final void I() {
        setContentView(R$layout.activity_main);
        z0.d(this);
        this.c = (Button) findViewById(R$id.btn_toggle);
        this.m = (TextView) findViewById(R$id.tv_status);
        this.n = (ImageView) findViewById(R$id.iv_status_icon);
        this.c.setOnClickListener(this);
        findViewById(R$id.btn_settings).setOnClickListener(new View.OnClickListener() { // from class: yd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.t(view);
            }
        });
        findViewById(R$id.card_new_tab).setOnClickListener(new View.OnClickListener() { // from class: zd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.u(view);
            }
        });
        findViewById(R$id.card_bookmarks).setOnClickListener(new View.OnClickListener() { // from class: Ad
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.v(view);
            }
        });
        findViewById(R$id.card_history).setOnClickListener(new View.OnClickListener() { // from class: Bd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.w(view);
            }
        });
        z0.g(findViewById(R$id.my_template), this, (FrameLayout) findViewById(R$id.Native));
        z0.e(this, (LinearLayout) findViewById(R$id.banner_ad_container));
        P();
    }

    public final void J() {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", "Check out Browser Bubble! The ultimate multi-window floating browser. https://play.google.com/store/apps/details?id=" + getPackageName());
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    public final void K() {
        new Zi(this).A("About Us").u("Bubble: Fast Floating Browser\nVersion 1.0.0\n\nA professional floating multi-bubble browser for true multitasking on Android.").x("OK", (DialogInterface.OnClickListener) null).n();
    }

    public final void L() {
        new Zi(this).A("Privacy Policy").u("Privacy Policy for Bubble: Fast Floating Browser\n\n1. Introduction\nBubble: Fast Floating Browser (\"we\", \"us\", or \"our\") is committed to protecting your privacy.\n\n2. Information Collection\n- Personal Data: We do not collect personally identifiable information directly.\n- Browsing Data: Your history and tabs are stored locally on your device. We do not access this data.\n- Ads: We use Google AdMob which may collect device identifiers for personalized advertising.\n\n3. Permissions\n- Overlay: Required for floating browser bubbles.\n- Internet: Required to browse the web.\n- Foreground Service: Keeps your sessions active.\n- Notifications: Used for service controls.\n- Storage: Used for downloads/uploads.\n\n4. Data Security\nWe prioritize the security of your local data. Please keep your device secured.\n\nFull policy available at: [Your URL]").x("Close", (DialogInterface.OnClickListener) null).n();
    }

    public final void M() {
        final DataManager dataManager = new DataManager(this);
        final String[] strArr = {"Google", "DuckDuckGo", "Bing", "Yahoo"};
        final String[] strArr2 = {"google", "duckduckgo", "bing", "yahoo"};
        String strJ = dataManager.j();
        int i = 0;
        for (int i2 = 0; i2 < 4; i2++) {
            if (strArr2[i2].equals(strJ)) {
                i = i2;
                break;
            }
        }
        final int[] iArr = {i};
        new Zi(this).A("Default Search Engine").z(strArr, i, new DialogInterface.OnClickListener() { // from class: wd
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i3) {
                FloatingActivity.x(iArr, dialogInterface, i3);
            }
        }).x("Apply", new DialogInterface.OnClickListener() { // from class: xd
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i3) {
                this.c.y(dataManager, strArr2, iArr, strArr, dialogInterface, i3);
            }
        }).v("Cancel", (DialogInterface.OnClickListener) null).n();
    }

    public final void N() {
        final com.google.android.material.bottomsheet.a aVar = new com.google.android.material.bottomsheet.a(this);
        View viewInflate = LayoutInflater.from(this).inflate(R$layout.layout_settings_bottom_sheet, (ViewGroup) null);
        aVar.setContentView(viewInflate);
        View view = (View) viewInflate.getParent();
        if (view != null) {
            view.setBackgroundColor(0);
        }
        viewInflate.findViewById(R$id.option_default_browser).setOnClickListener(new View.OnClickListener() { // from class: Cd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.z(aVar, view2);
            }
        });
        viewInflate.findViewById(R$id.option_search_engine).setOnClickListener(new View.OnClickListener() { // from class: rd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.A(aVar, view2);
            }
        });
        viewInflate.findViewById(R$id.option_share).setOnClickListener(new View.OnClickListener() { // from class: sd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.B(aVar, view2);
            }
        });
        viewInflate.findViewById(R$id.option_rate).setOnClickListener(new View.OnClickListener() { // from class: td
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.C(aVar, view2);
            }
        });
        viewInflate.findViewById(R$id.option_about).setOnClickListener(new View.OnClickListener() { // from class: ud
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.D(aVar, view2);
            }
        });
        viewInflate.findViewById(R$id.option_privacy).setOnClickListener(new View.OnClickListener() { // from class: vd
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                this.c.E(aVar, view2);
            }
        });
        aVar.show();
    }

    public final void O() {
        Intent intent = new Intent(this, (Class<?>) ChatHeadService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, this.r, 1);
    }

    public final void P() {
        ImageView imageView;
        int i;
        if (this.p) {
            this.m.setText("Service Active");
            this.m.setTextColor(getResources().getColor(R$color.status_active));
            imageView = this.n;
            if (imageView == null) {
                return;
            } else {
                i = R$drawable.ic_status_indicator_active;
            }
        } else {
            this.m.setText("Starting service...");
            this.m.setTextColor(getResources().getColor(R$color.status_pending));
            imageView = this.n;
            if (imageView == null) {
                return;
            } else {
                i = R$drawable.ic_status_pending;
            }
        }
        imageView.setImageResource(i);
    }

    @Override // android.app.Activity
    public void onActivityResult(int i, int i2, Intent intent) {
        String str;
        if (i == 100) {
            if (Settings.canDrawOverlays(this)) {
                O();
                return;
            }
            str = "Permission denied";
        } else if (i != 200) {
            return;
        } else {
            str = i2 == -1 ? "Bubble Browser is now your default browser!" : "Default browser not set";
        }
        Toast.makeText(this, str, 0).show();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (!this.p || this.o == null) {
            Toast.makeText(this, "Service not ready", 0).show();
        } else if (view.getId() == R$id.btn_toggle) {
            ChatHeadService chatHeadService = this.o;
            if (chatHeadService != null) {
                chatHeadService.C(false);
            }
            z0.f(this, new b());
        }
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.s = new W3(this);
        s(getIntent());
        if (Settings.canDrawOverlays(this)) {
            O();
        } else {
            startActivityForResult(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName())), 100);
        }
        I();
        z0.d(this);
        S1 s1 = MyApplication.c;
        if (s1 != null) {
            s1.k();
        }
    }

    @Override // android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        if (this.p) {
            unbindService(this.r);
            this.p = false;
        }
    }

    @Override // android.app.Activity
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        s(intent);
    }

    public final void s(Intent intent) {
        Uri data;
        ChatHeadService chatHeadService;
        if (intent == null || !"android.intent.action.VIEW".equals(intent.getAction()) || (data = intent.getData()) == null) {
            return;
        }
        String string = data.toString();
        if (!this.p || (chatHeadService = this.o) == null) {
            this.q = string;
        } else {
            this.o.s(chatHeadService.w(string));
        }
    }

    public final /* synthetic */ void t(View view) {
        N();
    }

    public final /* synthetic */ void u(View view) {
        String str;
        ChatHeadService chatHeadService;
        if (!this.p || (chatHeadService = this.o) == null) {
            str = "Service not ready";
        } else {
            this.o.s(chatHeadService.w("about:home"));
            this.o.E();
            str = "Opening new tab...";
        }
        Toast.makeText(this, str, 0).show();
    }

    public final /* synthetic */ void v(View view) {
        ChatHeadService chatHeadService;
        if (!this.p || (chatHeadService = this.o) == null) {
            Toast.makeText(this, "Service not ready", 0).show();
        } else {
            chatHeadService.r(1);
            this.o.E();
        }
    }

    public final /* synthetic */ void w(View view) {
        ChatHeadService chatHeadService;
        if (!this.p || (chatHeadService = this.o) == null) {
            Toast.makeText(this, "Service not ready", 0).show();
        } else {
            chatHeadService.r(2);
            this.o.E();
        }
    }

    public final /* synthetic */ void y(DataManager dataManager, String[] strArr, int[] iArr, String[] strArr2, DialogInterface dialogInterface, int i) {
        dataManager.u(strArr[iArr[0]]);
        Toast.makeText(this, strArr2[iArr[0]] + " set as default", 0).show();
    }

    public final /* synthetic */ void z(com.google.android.material.bottomsheet.a aVar, View view) {
        aVar.dismiss();
        F();
    }
}

================================================================
