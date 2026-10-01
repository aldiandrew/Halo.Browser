package com.flipkart.chatheads.ui;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import defpackage.U4;
import defpackage.X4;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class HostFrameLayout extends FrameLayout {
    public final X4 c;
    public final U4 m;

    public HostFrameLayout(Context context, U4 u4, X4 x4) {
        super(context);
        this.c = x4;
        this.m = u4;
    }

    public void a() {
        if (this.c.A() instanceof b) {
            return;
        }
        this.c.p(b.class, null, true);
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        boolean zDispatchKeyEvent = super.dispatchKeyEvent(keyEvent);
        if (zDispatchKeyEvent || keyEvent.getAction() != 1 || keyEvent.getKeyCode() != 4) {
            return zDispatchKeyEvent;
        }
        a();
        return true;
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.c.D(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override // android.view.View
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.c.b(i, i2, i3, i4);
    }
}

================================================================
