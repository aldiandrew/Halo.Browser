package com.flipkart.chatheads.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import com.google.android.gms.internal.ads.zzbbn;
import defpackage.AbstractC0102a5;
import defpackage.C0185er;
import defpackage.Tq;
import defpackage.X4;
import defpackage.jr;
import defpackage.lr;
import defpackage.nr;
import java.io.Serializable;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class ChatHead<T extends Serializable> extends ImageView implements lr {
    public lr A;
    public lr B;
    public C0185er C;
    public C0185er D;
    public C0185er E;
    public Bundle F;
    public boolean G;
    public GestureDetector H;
    public final int c;
    public final int m;
    public final float n;
    public X4 o;
    public nr p;
    public boolean q;
    public State r;
    public Serializable s;
    public float t;
    public float u;
    public VelocityTracker v;
    public boolean w;
    public float x;
    public float y;
    public int z;

    public enum State {
        FREE,
        CAPTURED
    }

    public class a extends Tq {
        public a() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            super.d(c0185er);
            ChatHead.this.o.k().i(ChatHead.this, (int) c0185er.d());
        }
    }

    public class b extends Tq {
        public b() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void b(C0185er c0185er) {
            super.b(c0185er);
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            super.d(c0185er);
            ChatHead.this.o.k().h(ChatHead.this, (int) c0185er.d());
        }
    }

    public class c extends Tq {
        public c() {
        }

        @Override // defpackage.Tq, defpackage.lr
        public void d(C0185er c0185er) {
            super.d(c0185er);
            float fD = (float) c0185er.d();
            ChatHead.this.setScaleX(fD);
            ChatHead.this.setScaleY(fD);
        }
    }

    public class d extends GestureDetector.SimpleOnGestureListener {
        public d() {
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public void onLongPress(MotionEvent motionEvent) {
            if (ChatHead.this.w) {
                return;
            }
            ChatHead.this.o.x(ChatHead.this);
        }
    }

    public ChatHead(X4 x4, nr nrVar, Context context, boolean z) {
        super(context);
        this.c = AbstractC0102a5.a(getContext(), 110);
        this.m = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.n = AbstractC0102a5.a(getContext(), 10);
        this.t = -1.0f;
        this.u = -1.0f;
        this.z = 0;
        this.o = x4;
        this.p = nrVar;
        this.q = z;
        g();
    }

    @Override // defpackage.lr
    public void b(C0185er c0185er) {
        if (this.o.r() != null) {
            this.o.r().c(this);
        }
    }

    @Override // defpackage.lr
    public void c(C0185er c0185er) {
        if (this.o.r() != null) {
            this.o.r().d(this);
        }
    }

    @Override // defpackage.lr
    public void d(C0185er c0185er) {
        C0185er c0185er2;
        C0185er c0185er3 = this.D;
        if (c0185er3 == null || (c0185er2 = this.E) == null) {
            return;
        }
        if (c0185er == c0185er3 || c0185er == c0185er2) {
            int iHypot = (int) Math.hypot(c0185er3.i(), c0185er2.i());
            if (this.o.A() != null) {
                this.o.A().j(this, this.w, this.o.C(), this.o.z(), c0185er, c0185er3, c0185er2, iHypot);
            }
        }
    }

    public final void g() {
        this.A = new a();
        C0185er c0185erC = this.p.c();
        this.D = c0185erC;
        c0185erC.a(this.A);
        this.D.a(this);
        this.B = new b();
        C0185er c0185erC2 = this.p.c();
        this.E = c0185erC2;
        c0185erC2.a(this.B);
        this.E.a(this);
        C0185er c0185erC3 = this.p.c();
        this.C = c0185erC3;
        c0185erC3.a(new c());
        this.C.p(1.0d).o();
        this.H = new GestureDetector(getContext(), new d());
    }

    public Bundle getExtras() {
        return this.F;
    }

    public lr getHorizontalPositionListener() {
        return this.A;
    }

    public C0185er getHorizontalSpring() {
        return this.D;
    }

    public T getKey() {
        return (T) this.s;
    }

    public State getState() {
        return this.r;
    }

    public int getUnreadCount() {
        return this.z;
    }

    public lr getVerticalPositionListener() {
        return this.B;
    }

    public C0185er getVerticalSpring() {
        return this.E;
    }

    public boolean h() {
        return this.G;
    }

    public boolean i() {
        return this.q;
    }

    public void j() {
        this.D.o();
        this.D.m();
        this.D.c();
        this.D = null;
        this.E.o();
        this.E.m();
        this.E.c();
        this.E = null;
        this.C.o();
        this.C.m();
        this.C.c();
        this.C = null;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        if (this.D == null || this.E == null) {
            return false;
        }
        if (this.H.onTouchEvent(motionEvent)) {
            return true;
        }
        C0185er c0185er = this.D;
        C0185er c0185er2 = this.E;
        int action = motionEvent.getAction();
        float rawX = motionEvent.getRawX();
        float rawY = motionEvent.getRawY();
        float f = rawX - this.t;
        float f2 = rawY - this.u;
        boolean zM = this.o.A().m(this);
        motionEvent.offsetLocation(this.o.k().b(this), this.o.k().d(this));
        if (action == 0) {
            if (this.o.A() instanceof com.flipkart.chatheads.ui.b) {
                ((com.flipkart.chatheads.ui.b) this.o.A()).B(true);
            }
            VelocityTracker velocityTracker = this.v;
            if (velocityTracker == null) {
                this.v = VelocityTracker.obtain();
            } else {
                velocityTracker.clear();
            }
            c0185er.s(jr.a);
            c0185er2.s(jr.a);
            setState(State.FREE);
            this.t = rawX;
            this.u = rawY;
            this.x = (float) c0185er.d();
            this.y = (float) c0185er2.d();
            this.C.r(0.8999999761581421d);
            c0185er.o();
            c0185er2.o();
            this.v.addMovement(motionEvent);
        } else if (action == 2) {
            if (Math.hypot(f, f2) > this.m) {
                this.w = true;
                if (zM) {
                    this.o.e().d();
                }
            }
            this.v.addMovement(motionEvent);
            if (this.w) {
                this.o.e().n(rawX, rawY);
                if (this.o.A().a(this)) {
                    if (this.o.c(rawX, rawY) >= this.c || !zM) {
                        setState(State.FREE);
                        c0185er.s(jr.c);
                        c0185er2.s(jr.c);
                        c0185er.p(this.x + f);
                        c0185er2.p(this.y + f2);
                        this.o.e().m();
                    } else {
                        setState(State.CAPTURED);
                        c0185er.s(jr.a);
                        c0185er2.s(jr.a);
                        int[] iArrY = this.o.y(this);
                        c0185er.r(iArrY[0]);
                        c0185er2.r(iArrY[1]);
                        this.o.e().k();
                    }
                    this.v.computeCurrentVelocity(zzbbn.zzq.zzf);
                }
            }
        } else if (action == 1 || action == 3) {
            boolean z = this.w;
            c0185er.s(jr.c);
            c0185er.s(jr.c);
            this.w = false;
            this.C.r(1.0d);
            int xVelocity = (int) this.v.getXVelocity();
            int yVelocity = (int) this.v.getYVelocity();
            this.v.recycle();
            this.v = null;
            if (this.D != null && this.E != null) {
                this.o.A().c(this, xVelocity, yVelocity, c0185er, c0185er2, z);
            }
        }
        return true;
    }

    public void setExtras(Bundle bundle) {
        this.F = bundle;
    }

    public void setHero(boolean z) {
        this.G = z;
    }

    @Override // android.widget.ImageView
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    public void setKey(T t) {
        this.s = t;
    }

    public void setState(State state) {
        this.r = state;
    }

    public void setUnreadCount(int i) {
        if (i != this.z) {
            this.o.j(this.s);
        }
        this.z = i;
    }

    public ChatHead(Context context) {
        super(context);
        this.c = AbstractC0102a5.a(getContext(), 110);
        this.m = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.n = AbstractC0102a5.a(getContext(), 10);
        this.q = false;
        this.t = -1.0f;
        this.u = -1.0f;
        this.z = 0;
        throw new IllegalArgumentException("This constructor cannot be used");
    }

    public ChatHead(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.c = AbstractC0102a5.a(getContext(), 110);
        this.m = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.n = AbstractC0102a5.a(getContext(), 10);
        this.q = false;
        this.t = -1.0f;
        this.u = -1.0f;
        this.z = 0;
        throw new IllegalArgumentException("This constructor cannot be used");
    }

    public ChatHead(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.c = AbstractC0102a5.a(getContext(), 110);
        this.m = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.n = AbstractC0102a5.a(getContext(), 10);
        this.q = false;
        this.t = -1.0f;
        this.u = -1.0f;
        this.z = 0;
        throw new IllegalArgumentException("This constructor cannot be used");
    }

    @Override // defpackage.lr
    public void a(C0185er c0185er) {
    }
}

================================================================
