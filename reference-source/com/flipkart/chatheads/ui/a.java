package com.flipkart.chatheads.ui;

import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import defpackage.AbstractC0102a5;
import defpackage.C0173e2;
import defpackage.C0185er;
import defpackage.S4;
import defpackage.T4;
import defpackage.Tq;
import defpackage.X4;
import defpackage.jr;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class a extends S4 {
    public static double l;
    public static int m;
    public X4 b;
    public int c;
    public int d;
    public UpArrowLayout f;
    public int g;
    public int h;
    public Bundle k;
    public final Map a = new C0173e2();
    public ChatHead e = null;
    public boolean i = false;
    public boolean j = false;

    /* JADX INFO: renamed from: com.flipkart.chatheads.ui.a$a, reason: collision with other inner class name */
    public class ViewOnClickListenerC0055a implements View.OnClickListener {
        public ViewOnClickListenerC0055a() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            a.this.r();
        }
    }

    public class b extends Tq {
        public b() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
            if (a.this.j) {
                a.this.j = false;
            }
            a.this.e.getVerticalSpring().n(this);
        }
    }

    public class c extends Tq {
        public c() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
            if (a.this.j) {
                a.this.j = false;
            }
            a.this.e.getHorizontalSpring().n(this);
        }
    }

    public a(X4 x4) {
        this.b = x4;
    }

    public static void B(View view) {
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if (viewGroup == null || viewGroup.indexOfChild(view) == 0) {
            return;
        }
        viewGroup.removeView(view);
        viewGroup.addView(view, 0);
    }

    public final void A(ChatHead chatHead) {
        ChatHead chatHead2 = this.e;
        if (chatHead2 != chatHead) {
            s(chatHead2);
            this.e = chatHead;
        }
        y(chatHead);
        C(chatHead);
    }

    public final void C(ChatHead chatHead) {
        Point point = (Point) this.a.get(chatHead);
        if (point != null) {
            double d = chatHead.getHorizontalSpring().d() - ((double) point.x);
            double d2 = chatHead.getVerticalSpring().d() - ((double) point.y);
            double dHypot = Math.hypot(d, d2);
            if (dHypot < this.g) {
                D(chatHead, d, d2, dHypot);
            } else {
                x();
            }
        }
    }

    public final void D(ChatHead chatHead, double d, double d2, double d3) {
        UpArrowLayout upArrowLayoutT = t();
        upArrowLayoutT.setVisibility(0);
        upArrowLayoutT.setTranslationX((float) d);
        upArrowLayoutT.setTranslationY((float) d2);
        upArrowLayoutT.setAlpha(1.0f - (((float) d3) / this.g));
    }

    @Override // defpackage.S4
    public boolean a(ChatHead chatHead) {
        return !chatHead.i();
    }

    @Override // defpackage.S4
    public Bundle b() {
        return u();
    }

    @Override // defpackage.S4
    public boolean c(ChatHead chatHead, int i, int i2, C0185er c0185er, C0185er c0185er2, boolean z) {
        if (i == 0 && i2 == 0) {
            i = 1;
            i2 = 1;
        }
        c0185er.t(i);
        c0185er2.t(i2);
        if (z) {
            return true;
        }
        if (chatHead != this.e && !this.b.B(chatHead)) {
            A(chatHead);
            return true;
        }
        boolean zB = this.b.B(chatHead);
        if (!zB) {
            r();
        }
        return zB;
    }

    @Override // defpackage.S4
    public void d(X4 x4, Bundle bundle, int i, int i2, boolean z) {
        this.j = true;
        this.b = x4;
        this.c = i;
        this.d = i2;
        m = AbstractC0102a5.b(x4.a(), 50);
        l = AbstractC0102a5.a(x4.getContext(), 10);
        this.i = true;
        List listW = x4.w();
        this.k = bundle;
        int iIntValue = bundle != null ? bundle.getInt("hero_index", -1) : 0;
        if (iIntValue < 0 && this.e != null) {
            iIntValue = v().intValue();
        }
        if (iIntValue < 0 || iIntValue > listW.size() - 1) {
            iIntValue = 0;
        }
        if (listW.size() <= 0 || iIntValue >= listW.size()) {
            return;
        }
        this.e = (ChatHead) listW.get(iIntValue);
        this.g = (int) l;
        int iD = x4.f().d(i, i2);
        int iF = x4.f().f();
        this.h = AbstractC0102a5.a(x4.getContext(), 5);
        int i3 = iF + iD;
        int size = i - (listW.size() * i3);
        for (int i4 = 0; i4 < listW.size(); i4++) {
            ChatHead chatHead = (ChatHead) listW.get(i4);
            C0185er horizontalSpring = chatHead.getHorizontalSpring();
            int i5 = (i4 * i3) + size;
            this.a.put(chatHead, new Point(i5, this.h));
            horizontalSpring.o();
            horizontalSpring.s(jr.a);
            double d = i5;
            horizontalSpring.r(d);
            if (!z) {
                horizontalSpring.p(d);
            }
            C0185er verticalSpring = chatHead.getVerticalSpring();
            verticalSpring.o();
            verticalSpring.s(jr.a);
            verticalSpring.r(this.h);
            if (!z) {
                verticalSpring.p(this.h);
            }
        }
        x4.e().setEnabled(true);
        x4.m().setOnClickListener(new ViewOnClickListenerC0055a());
        x4.g(z);
        l(this.e);
        this.e.getVerticalSpring().a(new b());
        this.e.getHorizontalSpring().a(new c());
    }

    @Override // defpackage.S4
    public void e(X4 x4, ChatHead chatHead) {
        if (chatHead.i()) {
            return;
        }
        x4.h(chatHead.getKey(), true);
    }

    @Override // defpackage.S4
    public void f(ChatHead chatHead, boolean z) {
        chatHead.getHorizontalSpring().p(this.c).o();
        chatHead.getVerticalSpring().p(this.h).o();
        d(this.b, u(), this.c, this.d, z);
    }

    @Override // defpackage.S4
    public void g(ChatHead chatHead) {
        this.b.u(chatHead, t());
        this.b.d(chatHead, t());
        this.a.remove(chatHead);
        if (this.e == chatHead) {
            ChatHead chatHeadW = w();
            if (chatHeadW == null) {
                r();
                return;
            }
            A(chatHeadW);
        }
        d(this.b, u(), this.c, this.d, true);
    }

    @Override // defpackage.S4
    public void i(int i, int i2) {
        ChatHead chatHead = this.e;
        if (chatHead != null) {
            this.b.u(chatHead, t());
        }
        x();
        this.b.n(true);
        this.a.clear();
        this.i = false;
    }

    /* JADX WARN: Code duplicated, block: B:34:0x007b  */
    @Override // defpackage.S4
    public void j(ChatHead chatHead, boolean z, int i, int i2, C0185er c0185er, C0185er c0185er2, C0185er c0185er3, int i3) {
        if (c0185er == c0185er2 && !z) {
            double d = c0185er2.d();
            if (((double) this.b.f().f()) + d > i && c0185er2.h() != jr.a && !c0185er2.l()) {
                z(chatHead, c0185er2, c0185er3);
            }
            if (d < 0.0d && c0185er2.h() != jr.a && !c0185er2.l()) {
                z(chatHead, c0185er2, c0185er3);
            }
        } else if (c0185er == c0185er3 && !z) {
            double d2 = c0185er3.d();
            if (((double) this.b.f().c()) + d2 > i2 && c0185er2.h() != jr.a && !c0185er2.l()) {
                z(chatHead, c0185er2, c0185er3);
            }
            if (d2 < 0.0d && c0185er2.h() != jr.a && !c0185er2.l()) {
                z(chatHead, c0185er2, c0185er3);
            }
        }
        if (!z && i3 < m && c0185er2.h() == jr.c) {
            z(chatHead, c0185er2, c0185er3);
        }
        if (chatHead == this.e) {
            C(chatHead);
        }
        if (z) {
            return;
        }
        int[] iArrY = this.b.y(chatHead);
        if (this.b.c(((float) c0185er2.d()) + (this.b.f().f() / 2), ((float) c0185er3.d()) + (this.b.f().c() / 2)) < chatHead.c && c0185er2.h() == jr.c && c0185er3.h() == jr.c && !chatHead.i()) {
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
            this.b.e().e(false, true);
            this.b.v(chatHead);
        }
        if (c0185er3.k() || this.j) {
            this.b.e().e(true, true);
        } else {
            this.b.e().d();
        }
    }

    @Override // defpackage.S4
    public void k() {
        for (ChatHead chatHead : this.b.w()) {
            if (!chatHead.i() && chatHead != this.e) {
                this.b.h(chatHead.getKey(), false);
                return;
            }
        }
    }

    @Override // defpackage.S4
    public void l(ChatHead chatHead) {
        A(chatHead);
    }

    @Override // defpackage.S4
    public boolean m(ChatHead chatHead) {
        return !chatHead.i();
    }

    public final void r() {
        this.b.t(com.flipkart.chatheads.ui.b.class, u());
        x();
    }

    public final void s(ChatHead chatHead) {
        this.b.u(chatHead, t());
    }

    public final UpArrowLayout t() {
        if (this.f == null) {
            this.f = this.b.i();
        }
        return this.f;
    }

    public final Bundle u() {
        Bundle bundle = this.k;
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putInt("hero_index", v().intValue());
        return bundle;
    }

    public Integer v() {
        Iterator it = this.b.w().iterator();
        int i = 0;
        int i2 = 0;
        while (it.hasNext()) {
            if (this.e == ((ChatHead) it.next())) {
                i = i2;
            }
            i2++;
        }
        return Integer.valueOf(i);
    }

    public final ChatHead w() {
        ChatHead chatHead = null;
        for (ChatHead chatHead2 : this.b.w()) {
            if (chatHead == null || chatHead2.getUnreadCount() >= chatHead.getUnreadCount()) {
                chatHead = chatHead2;
            }
        }
        return chatHead;
    }

    public final void x() {
        t().setVisibility(8);
    }

    public final void y(ChatHead chatHead) {
        UpArrowLayout upArrowLayoutT = t();
        t().removeAllViews();
        this.b.l(chatHead, upArrowLayoutT);
        B(this.b.m());
        Point point = (Point) this.a.get(chatHead);
        if (point != null) {
            upArrowLayoutT.c(point.x + (this.b.f().f() / 2), point.y + this.b.f().c() + this.b.f().e(this.c, this.d));
        }
    }

    public final void z(ChatHead chatHead, C0185er c0185er, C0185er c0185er2) {
        Point point;
        Point point2;
        if (chatHead.i() && (point2 = (Point) this.a.get(chatHead)) != null && Math.hypot(((double) point2.x) - c0185er.d(), ((double) point2.y) - c0185er2.d()) > l) {
            r();
            return;
        }
        if (chatHead.getState() != ChatHead.State.FREE || (point = (Point) this.a.get(chatHead)) == null) {
            return;
        }
        c0185er.s(jr.a);
        c0185er.t(0.0d);
        c0185er.r(point.x);
        c0185er2.s(jr.a);
        c0185er2.t(0.0d);
        c0185er2.r(point.y);
    }

    @Override // defpackage.S4
    public void h(T4 t4) {
    }
}

================================================================
