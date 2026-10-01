package com.bubble.browser.pro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.browser.browser.pro.R$drawable;
import com.browser.browser.pro.R$id;
import com.browser.browser.pro.R$layout;
import com.google.android.material.button.MaterialButton;
import defpackage.C0452w0;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class IntroActivity extends Activity {
    public ViewPager2 c;
    public MaterialButton m;
    public MaterialButton n;
    public LinearLayout o;
    public ImageView[] p;
    public com.bubble.browser.pro.b.a[] q;

    public class a extends RecyclerView.i {
        public a() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.i
        public void a() {
            IntroActivity.this.j();
        }
    }

    public class b extends ViewPager2.i {
        public b() {
        }

        @Override // androidx.viewpager2.widget.ViewPager2.i
        public void c(int i) {
            IntroActivity.this.o(i);
            IntroActivity.this.n(i);
        }
    }

    public final void f() {
        getSharedPreferences("IntroPrefs", 0).edit().putBoolean("intro_shown", true).apply();
        m();
    }

    public final /* synthetic */ void g(View view) {
        int currentItem = this.c.getCurrentItem();
        if (currentItem > 0) {
            this.c.setCurrentItem(currentItem - 1);
        }
    }

    public final /* synthetic */ void h(View view) {
        int currentItem = this.c.getCurrentItem();
        if (currentItem < (this.c.getAdapter() != null ? this.c.getAdapter().d() : this.q.length) - 1) {
            this.c.setCurrentItem(currentItem + 1);
        } else {
            f();
        }
    }

    public final void i() {
        this.m.setOnClickListener(new View.OnClickListener() { // from class: Mg
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.g(view);
            }
        });
        this.n.setOnClickListener(new View.OnClickListener() { // from class: Ng
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.c.h(view);
            }
        });
        n(0);
    }

    public final void j() {
        int iD = this.c.getAdapter() != null ? this.c.getAdapter().d() : this.q.length;
        this.p = new ImageView[iD];
        this.o.removeAllViews();
        for (int i = 0; i < iD; i++) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(24, 24);
            layoutParams.setMargins(8, 0, 8, 0);
            imageView.setLayoutParams(layoutParams);
            imageView.setImageResource(R$drawable.circle_background);
            imageView.setColorFilter(-7829368);
            this.p[i] = imageView;
            this.o.addView(imageView);
        }
        o(this.c.getCurrentItem());
    }

    public final void k() {
        this.q = new com.bubble.browser.pro.b.a[]{new com.bubble.browser.pro.b.a(R$drawable.intro_welcome_new, "Welcome to Bubble: Fast Floating Browser", "Fast, floating, and built for true multitasking."), new com.bubble.browser.pro.b.a(R$drawable.intro_loading_new, "Instant Background Loading", "New tabs start loading automatically in the background. No more waiting!"), new com.bubble.browser.pro.b.a(R$drawable.intro_persistence_new, "Smart Session Persistence", "Your tabs stay alive even if you close the app. Pick up exactly where you left off."), new com.bubble.browser.pro.b.a(R$drawable.intro_permission_new, "Permission Required", "To display floating bubbles, we need permission to draw over other apps. Tap 'Get Started' to grant permission.")};
    }

    public final void l() {
        C0452w0 c0452w0 = new C0452w0(this, new com.bubble.browser.pro.b(this.q), 4);
        this.c.setAdapter(c0452w0);
        c0452w0.s(new a());
        this.c.g(new b());
    }

    public final void m() {
        Intent intent = new Intent(this, (Class<?>) FloatingActivity.class);
        if (getIntent() != null && "android.intent.action.VIEW".equals(getIntent().getAction())) {
            intent.setAction("android.intent.action.VIEW");
            intent.setData(getIntent().getData());
        }
        startActivity(intent);
        finish();
    }

    public final void n(int i) {
        int iD = this.c.getAdapter() != null ? this.c.getAdapter().d() : this.q.length;
        this.m.setVisibility(i == 0 ? 4 : 0);
        this.n.setText(i == iD + (-1) ? "Get Started" : "Next");
    }

    public final void o(int i) {
        ImageView imageView;
        int color;
        int i2 = 0;
        while (true) {
            ImageView[] imageViewArr = this.p;
            if (i2 >= imageViewArr.length) {
                return;
            }
            if (i2 == i) {
                imageView = imageViewArr[i2];
                color = -1;
            } else {
                imageView = imageViewArr[i2];
                color = Color.parseColor("#505050");
            }
            imageView.setColorFilter(color);
            i2++;
        }
    }

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getSharedPreferences("IntroPrefs", 0).getBoolean("intro_shown", false)) {
            m();
            return;
        }
        setContentView(R$layout.activity_intro);
        this.c = (ViewPager2) findViewById(R$id.viewPager);
        this.m = findViewById(R$id.btn_previous);
        this.n = findViewById(R$id.btn_next);
        this.o = (LinearLayout) findViewById(R$id.indicator_container);
        k();
        l();
        j();
        i();
    }
}

================================================================
