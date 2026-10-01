package com.flipkart.chatheads.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import defpackage.AbstractC0102a5;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class ChatHeadOverlayView extends View {
    public float c;
    public float m;
    public Path n;
    public Paint o;
    public ObjectAnimator p;
    public PathEffect q;

    public ChatHeadOverlayView(Context context) {
        super(context);
        this.o = new Paint();
        a(context);
    }

    private void setPhase(float f) {
        this.q = new PathDashPathEffect(b(this.c), this.m, f, PathDashPathEffect.Style.ROTATE);
        invalidate();
    }

    public final void a(Context context) {
        this.m = AbstractC0102a5.a(context, 20);
        this.c = AbstractC0102a5.a(context, 3);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, "phase", 0.0f, -this.m);
        this.p = objectAnimatorOfFloat;
        objectAnimatorOfFloat.setInterpolator(new LinearInterpolator());
        this.p.setRepeatMode(1);
        this.p.setRepeatCount(-1);
        this.p.setDuration(600L);
    }

    public final Path b(float f) {
        Path path = new Path();
        path.addCircle(0.0f, 0.0f, f, Path.Direction.CCW);
        return path;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.n != null) {
            this.o.setPathEffect(this.q);
            canvas.drawPath(this.n, this.o);
        }
    }

    public ChatHeadOverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.o = new Paint();
        a(context);
    }

    public ChatHeadOverlayView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.o = new Paint();
        a(context);
    }
}

================================================================
