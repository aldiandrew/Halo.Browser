package com.flipkart.chatheads.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import defpackage.AbstractC0102a5;
import defpackage.C0185er;
import defpackage.C0219gr;
import defpackage.S4;
import defpackage.T4;
import defpackage.Tq;
import defpackage.X4;
import defpackage.jr;
import defpackage.lr;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class b extends S4 {
    public static int u;
    public static int v;
    public float a;
    public int e;
    public int f;
    public X4 h;
    public C0219gr i;
    public C0219gr j;
    public ChatHead k;
    public Bundle n;
    public boolean q;
    public float b = 0.0f;
    public int c = Integer.MIN_VALUE;
    public int d = Integer.MIN_VALUE;
    public boolean g = false;
    public double l = -1.0d;
    public double m = -1.0d;
    public lr o = new a();
    public lr p = new C0056b();
    public boolean r = false;
    public Handler s = new Handler(Looper.getMainLooper());
    public Runnable t = new c();

    public class a extends Tq {
        public a() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
            if (b.this.q) {
                b.this.q = false;
            }
            b.this.F();
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            b bVar = b.this;
            bVar.b = (float) ((((double) bVar.a) * (((double) (b.this.e / 2)) - c0185er.d())) / ((double) (b.this.e / 2)));
            if (b.this.i != null) {
                b.this.i.h().p(c0185er.d());
            }
        }
    }

    /* JADX INFO: renamed from: com.flipkart.chatheads.ui.b$b, reason: collision with other inner class name */
    public class C0056b extends Tq {
        public C0056b() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
            if (b.this.q) {
                b.this.q = false;
            }
            b.this.F();
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            if (b.this.j != null) {
                b.this.j.h().p(c0185er.d());
            }
        }
    }

    public class c implements Runnable {
        public c() {
        }

        @Override // java.lang.Runnable
        public void run() {
            b.this.I(true);
        }
    }

    public class d extends Tq {
        public final /* synthetic */ ChatHead c;

        public d(ChatHead chatHead) {
            this.c = chatHead;
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            this.c.getHorizontalSpring().p(c0185er.d() + ((double) (((b.this.i.g().indexOf(c0185er) - b.this.i.g().size()) + 1) * b.this.b)));
        }
    }

    public class e extends Tq {
        public final /* synthetic */ ChatHead c;

        public e(ChatHead chatHead) {
            this.c = chatHead;
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            this.c.getVerticalSpring().p(c0185er.d());
        }
    }

    public class f extends Tq {
        public f() {
        }
    }

    public class g extends Tq {
        public g() {
        }
    }

    public b(X4 x4) {
        this.a = 0.0f;
        this.h = x4;
        this.a = AbstractC0102a5.a(x4.getContext(), 5);
    }

    private void w() {
        this.h.t(com.flipkart.chatheads.ui.a.class, y());
    }

    private Bundle y() {
        return x(z().intValue());
    }

    public final Integer A(ChatHead chatHead) {
        Iterator it = this.h.w().iterator();
        int i = 0;
        int i2 = 0;
        while (it.hasNext()) {
            if (chatHead == ((ChatHead) it.next())) {
                i = i2;
            }
            i2++;
        }
        return Integer.valueOf(i);
    }

    public void B(boolean z) {
        H();
        if (z && this.r) {
            I(false);
        }
        if (this.r) {
            return;
        }
        F();
    }

    public void C(int i) {
        this.c = i;
    }

    public void D(int i) {
        this.d = i;
    }

    public final void E(ChatHead chatHead, int i, int i2) {
        int iD;
        C0185er horizontalSpring = chatHead.getHorizontalSpring();
        C0185er verticalSpring = chatHead.getVerticalSpring();
        int i3 = -1;
        if (chatHead.getState() == ChatHead.State.FREE) {
            if (Math.abs(i) < AbstractC0102a5.b(this.h.a(), 50)) {
                i = horizontalSpring.d() < ((double) this.e) - horizontalSpring.d() ? -1 : 1;
            }
            if (i >= 0 ? !(i <= 0 || (iD = (int) (((((double) this.e) - horizontalSpring.d()) - ((double) this.h.f().f())) * jr.c.a)) <= i) : i > (iD = (int) ((-horizontalSpring.d()) * jr.c.a))) {
                i = iD;
            }
        }
        if (Math.abs(i) > 1) {
            i3 = i;
        } else if (i >= 0) {
            i3 = 1;
        }
        if (i2 == 0) {
            i2 = 1;
        }
        horizontalSpring.t(i3);
        verticalSpring.t(i2);
    }

    public void F() {
        H();
        if (this.r || !this.g) {
            return;
        }
        this.s.postDelayed(this.t, 5000L);
    }

    public final int G(int i, int i2, ChatHead chatHead) {
        int iF = this.h.f().f();
        if (i2 - i < i) {
            return this.r ? i2 - ((int) (((double) iF) * 0.4d)) : i2 - iF;
        }
        if (this.r) {
            return -((int) (((double) iF) * 0.6d));
        }
        return 0;
    }

    public void H() {
        this.s.removeCallbacks(this.t);
    }

    public final void I(boolean z) {
        this.r = z;
        for (ChatHead chatHead : this.h.w()) {
            boolean z2 = chatHead.getHorizontalSpring().d() < ((double) (this.e / 2));
            Drawable drawable = chatHead.getDrawable();
            if (drawable != null) {
                try {
                    Class<?> cls = drawable.getClass();
                    Class cls2 = Boolean.TYPE;
                    cls.getMethod("setCollapsed", cls2, cls2).invoke(drawable, Boolean.valueOf(z), Boolean.valueOf(z2));
                } catch (Exception unused) {
                }
            }
        }
        ChatHead chatHead2 = this.k;
        if (chatHead2 != null) {
            int iG = G((int) chatHead2.getHorizontalSpring().d(), this.e, this.k);
            this.k.getHorizontalSpring().s(jr.a);
            this.k.getHorizontalSpring().r(iG);
        }
    }

    @Override // defpackage.S4
    public boolean a(ChatHead chatHead) {
        return true;
    }

    @Override // defpackage.S4
    public Bundle b() {
        return y();
    }

    @Override // defpackage.S4
    public boolean c(ChatHead chatHead, int i, int i2, C0185er c0185er, C0185er c0185er2, boolean z) {
        E(chatHead, i, i2);
        if (z || this.h.B(chatHead)) {
            return true;
        }
        w();
        return false;
    }

    @Override // defpackage.S4
    public void d(X4 x4, Bundle bundle, int i, int i2, boolean z) {
        int i3;
        this.q = true;
        if (this.i != null || this.j != null) {
            i(i, i2);
        }
        v = AbstractC0102a5.b(x4.a(), 600);
        u = AbstractC0102a5.b(x4.a(), 1);
        this.n = bundle;
        if (bundle != null) {
            i3 = bundle.getInt("hero_index", -1);
            this.l = bundle.getDouble("hero_relative_x", -1.0d);
            this.m = bundle.getDouble("hero_relative_y", -1.0d);
        } else {
            i3 = 0;
        }
        List listW = x4.w();
        if (i3 < 0 || i3 > listW.size() - 1) {
            i3 = 0;
        }
        if (i3 < listW.size()) {
            ChatHead chatHead = (ChatHead) listW.get(i3);
            this.k = chatHead;
            chatHead.setHero(true);
            this.i = C0219gr.f();
            this.j = C0219gr.f();
            for (int i4 = 0; i4 < listW.size(); i4++) {
                ChatHead chatHead2 = (ChatHead) listW.get(i4);
                if (chatHead2 != this.k) {
                    chatHead2.setHero(false);
                    this.i.e(new d(chatHead2));
                    ((C0185er) this.i.g().get(this.i.g().size() - 1)).p(chatHead2.getHorizontalSpring().d());
                    this.j.e(new e(chatHead2));
                    ((C0185er) this.j.g().get(this.j.g().size() - 1)).p(chatHead2.getVerticalSpring().d());
                    this.h.k().e(chatHead2);
                }
            }
            double d2 = this.l;
            this.c = d2 == -1.0d ? x4.f().g().x : (int) (d2 * ((double) i));
            double d3 = this.m;
            this.d = d3 == -1.0d ? x4.f().g().y : (int) (d3 * ((double) i2));
            this.c = G(this.c, i, this.k);
            ChatHead chatHead3 = this.k;
            if (chatHead3 != null && chatHead3.getHorizontalSpring() != null && this.k.getVerticalSpring() != null) {
                this.h.k().e(this.k);
                this.i.e(new f());
                this.j.e(new g());
                this.i.i(listW.size() - 1);
                this.j.i(listW.size() - 1);
                this.k.getHorizontalSpring().a(this.o);
                this.k.getVerticalSpring().a(this.p);
                this.k.getHorizontalSpring().s(jr.a);
                if (this.k.getHorizontalSpring().d() == this.c) {
                    this.k.getHorizontalSpring().q(this.c - 1, true);
                }
                C0185er horizontalSpring = this.k.getHorizontalSpring();
                double d4 = this.c;
                if (z) {
                    horizontalSpring.r(d4);
                } else {
                    horizontalSpring.q(d4, true);
                }
                this.k.getVerticalSpring().s(jr.a);
                if (this.k.getVerticalSpring().d() == this.d) {
                    this.k.getVerticalSpring().q(this.d - 1, true);
                }
                C0185er verticalSpring = this.k.getVerticalSpring();
                if (z) {
                    verticalSpring.r(this.d);
                } else {
                    verticalSpring.q(this.d, true);
                }
            }
            this.e = i;
            this.f = i2;
            x4.e().setEnabled(true);
        }
        this.g = true;
        this.r = false;
        F();
    }

    @Override // defpackage.S4
    public void e(X4 x4, ChatHead chatHead) {
        x4.s(true);
    }

    @Override // defpackage.S4
    public void f(ChatHead chatHead, boolean z) {
        ChatHead chatHead2 = this.k;
        if (chatHead2 != null && chatHead2.getHorizontalSpring() != null && this.k.getVerticalSpring() != null) {
            chatHead.getHorizontalSpring().p(this.k.getHorizontalSpring().d() - ((double) this.b));
            chatHead.getVerticalSpring().p(this.k.getVerticalSpring().d());
        }
        d(this.h, b(), this.e, this.f, z);
    }

    @Override // defpackage.S4
    public void g(ChatHead chatHead) {
        X4 x4 = this.h;
        x4.u(chatHead, x4.i());
        X4 x5 = this.h;
        x5.d(chatHead, x5.i());
        if (chatHead == this.k) {
            this.k = null;
        }
        d(this.h, null, this.e, this.f, true);
    }

    @Override // defpackage.S4
    public void i(int i, int i2) {
        this.g = false;
        H();
        if (this.r) {
            I(false);
        }
        ChatHead chatHead = this.k;
        if (chatHead != null) {
            chatHead.getHorizontalSpring().n(this.o);
            this.k.getVerticalSpring().n(this.p);
        }
        C0219gr c0219gr = this.i;
        if (c0219gr != null) {
            Iterator it = c0219gr.g().iterator();
            while (it.hasNext()) {
                ((C0185er) it.next()).c();
            }
        }
        C0219gr c0219gr2 = this.j;
        if (c0219gr2 != null) {
            Iterator it2 = c0219gr2.g().iterator();
            while (it2.hasNext()) {
                ((C0185er) it2.next()).c();
            }
        }
        this.i = null;
        this.j = null;
    }

    @Override // defpackage.S4
    public void j(ChatHead chatHead, boolean z, int i, int i2, C0185er c0185er, C0185er c0185er2, C0185er c0185er3, int i3) {
        c0185er2.i();
        c0185er3.i();
        if (!z && Math.abs(i3) < v && chatHead == this.k) {
            if (Math.abs(i3) < u && chatHead.getState() == ChatHead.State.FREE && this.g) {
                C((int) c0185er2.d());
                D((int) c0185er3.d());
            }
            if (c0185er == c0185er2 && !this.r) {
                double d2 = c0185er2.d();
                if (((double) this.h.f().f()) + d2 > i && c0185er2.i() > 0.0d) {
                    int iF = i - this.h.f().f();
                    c0185er2.s(jr.a);
                    c0185er2.r(iF);
                } else if (d2 < 0.0d && c0185er2.i() < 0.0d) {
                    c0185er2.s(jr.a);
                    c0185er2.r(0.0d);
                }
            } else if (c0185er == c0185er3) {
                double d3 = c0185er3.d();
                if (((double) this.h.f().f()) + d3 > i2 && c0185er3.i() > 0.0d) {
                    c0185er3.s(jr.a);
                    c0185er3.r(i2 - this.h.f().c());
                } else if (d3 < 0.0d && c0185er3.i() < 0.0d) {
                    c0185er3.s(jr.a);
                    c0185er3.r(0.0d);
                }
            }
        }
        if (z || chatHead != this.k) {
            return;
        }
        int[] iArrY = this.h.y(chatHead);
        if (this.h.c(((float) c0185er2.d()) + (this.h.f().f() / 2), ((float) c0185er3.d()) + (this.h.f().c() / 2)) < chatHead.c && c0185er2.h() == jr.c && c0185er3.h() == jr.c) {
            c0185er2.s(jr.a);
            c0185er3.s(jr.a);
            chatHead.setState(ChatHead.State.CAPTURED);
        }
        ChatHead.State state = chatHead.getState();
        ChatHead.State state2 = ChatHead.State.CAPTURED;
        if (state == state2 && c0185er2.h() != jr.b) {
            c0185er2.o();
            c0185er3.o();
            c0185er2.s(jr.b);
            c0185er3.s(jr.b);
            c0185er2.r(iArrY[0]);
            c0185er3.r(iArrY[1]);
        }
        if (chatHead.getState() == state2 && c0185er3.k()) {
            this.h.e().e(false, true);
            this.h.v(chatHead);
        }
        if (c0185er3.k() || this.q) {
            this.h.e().e(true, true);
        } else {
            this.h.e().d();
        }
    }

    @Override // defpackage.S4
    public void k() {
        for (ChatHead chatHead : this.h.w()) {
            if (!chatHead.i()) {
                this.h.h(chatHead.getKey(), false);
                return;
            }
        }
    }

    @Override // defpackage.S4
    public void l(ChatHead chatHead) {
    }

    @Override // defpackage.S4
    public boolean m(ChatHead chatHead) {
        return true;
    }

    public final Bundle x(int i) {
        ChatHead chatHead = this.k;
        if (chatHead != null) {
            this.l = (chatHead.getHorizontalSpring().d() * 1.0d) / ((double) this.e);
            this.m = (this.k.getVerticalSpring().d() * 1.0d) / ((double) this.f);
        }
        Bundle bundle = this.n;
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putInt("hero_index", i);
        bundle.putDouble("hero_relative_x", this.l);
        bundle.putDouble("hero_relative_y", this.m);
        return bundle;
    }

    public Integer z() {
        return A(this.k);
    }

    @Override // defpackage.S4
    public void h(T4 t4) {
    }
}

================================================================
