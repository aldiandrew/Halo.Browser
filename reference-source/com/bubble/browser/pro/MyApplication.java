package com.bubble.browser.pro;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import defpackage.S1;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class MyApplication extends Application {
    public static S1 c;

    public final /* synthetic */ void b(InitializationStatus initializationStatus) {
        S1 s1 = new S1(this);
        c = s1;
        s1.k();
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        z0.d(this);
        MobileAds.initialize(this, new OnInitializationCompleteListener() { // from class: Jk
            @Override // com.google.android.gms.ads.initialization.OnInitializationCompleteListener
            public final void onInitializationComplete(InitializationStatus initializationStatus) {
                this.a.b(initializationStatus);
            }
        });
    }
}

================================================================
