package com.flipkart.chatheads.ui;

import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import com.flipkart.chatheads.R$drawable;
import defpackage.C0185er;
import defpackage.Tq;
import defpackage.X4;
import defpackage.jr;
import defpackage.nr;
import defpackage.or;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class ChatHeadCloseButton extends ImageView {
    public int c;
    public int m;
    public C0185er n;
    public C0185er o;
    public C0185er p;
    public boolean q;
    public e r;
    public X4 s;
    public int t;
    public int u;

    public class a extends Tq {
        public final /* synthetic */ X4 c;

        public a(X4 x4) {
            this.c = x4;
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            super.d(c0185er);
            this.c.k().i(ChatHeadCloseButton.this, ChatHeadCloseButton.this.g(c0185er));
        }
    }

    public class b extends Tq {
        public final /* synthetic */ X4 c;

        public b(X4 x4) {
            this.c = x4;
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            super.d(c0185er);
            this.c.k().h(ChatHeadCloseButton.this, ChatHeadCloseButton.this.h(c0185er));
        }
    }

    public class c extends Tq {
        public c() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            float fD = (float) c0185er.d();
            ChatHeadCloseButton.this.setScaleX(fD);
            ChatHeadCloseButton.this.setScaleY(fD);
        }
    }

    public class d extends Tq {
        public d() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
            ChatHeadCloseButton.this.p.n(this);
        }
    }

    public interface e {
        void o();

        void q();
    }

    public ChatHeadCloseButton(Context context, X4 x4, int i, int i2) {
        super(context);
        i(x4, i, i2);
    }

    public void d() {
        if (isEnabled()) {
            this.p.s(jr.a);
            this.o.s(jr.a);
            this.n.r(0.800000011920929d);
            ViewParent parent = getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) parent;
                if (viewGroup.indexOfChild(this) != viewGroup.getChildCount() - 1) {
                    bringToFront();
                }
            }
            this.q = false;
        }
    }

    public void e(boolean z, boolean z2) {
        this.p.r((this.m - this.u) + this.s.f().a());
        this.p.s(jr.a);
        this.o.r(0.0d);
        this.p.a(new d());
        this.n.r(0.10000000149011612d);
        if (!z2) {
            this.p.q(this.m, true);
            this.o.q(0.0d, true);
        }
        this.q = true;
        e eVar = this.r;
        if (eVar != null) {
            eVar.q();
        }
    }

    public final double f(double d2, float f, int i) {
        float f2 = f * i;
        return or.a(d2, 0.0d, i, (-f2) / 2.0f, f2 / 2.0f);
    }

    public final int g(C0185er c0185er) {
        return (this.t + ((int) c0185er.d())) - (getMeasuredWidth() / 2);
    }

    public int getEndValueX() {
        return g(this.o);
    }

    public int getEndValueY() {
        return h(this.p);
    }

    public final int h(C0185er c0185er) {
        return (this.u + ((int) c0185er.d())) - (getMeasuredHeight() / 2);
    }

    public final void i(X4 x4, int i, int i2) {
        this.s = x4;
        setImageResource(R$drawable.dismiss_big);
        nr nrVarI = nr.i();
        C0185er c0185erC = nrVarI.c();
        this.o = c0185erC;
        c0185erC.a(new a(x4));
        C0185er c0185erC2 = nrVarI.c();
        this.p = c0185erC2;
        c0185erC2.a(new b(x4));
        C0185er c0185erC3 = nrVarI.c();
        this.n = c0185erC3;
        c0185erC3.a(new c());
    }

    public boolean j() {
        return this.q;
    }

    public void k() {
        this.n.r(1.0d);
    }

    public void l() {
        this.c = this.s.C();
        this.m = this.s.z();
    }

    public void m() {
        this.n.r(0.8d);
    }

    public void n(float f, float f2) {
        if (isEnabled()) {
            double dF = f(f, 0.1f, this.c);
            double dF2 = f(f2, 0.05f, this.m);
            if (this.q) {
                return;
            }
            this.o.r(dF);
            this.p.r(dF2);
            e eVar = this.r;
            if (eVar != null) {
                eVar.o();
            }
        }
    }

    public void o(int i, int i2) {
        if (i == this.t && i2 == this.u) {
            return;
        }
        this.t = i;
        this.u = i2;
        this.o.q(0.0d, false);
        this.p.q(0.0d, false);
    }

    @Override // android.view.View
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        e(true, false);
    }

    public void setListener(e eVar) {
        this.r = eVar;
    }
}

================================================================
