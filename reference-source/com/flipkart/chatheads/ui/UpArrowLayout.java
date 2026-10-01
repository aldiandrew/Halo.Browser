package com.flipkart.chatheads.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.flipkart.chatheads.R$drawable;
import java.util.ArrayList;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class UpArrowLayout extends ViewGroup {
    public final Point c;
    public final ArrayList m;
    public ImageView n;
    public int o;

    public UpArrowLayout(Context context) {
        super(context);
        this.c = new Point(0, 0);
        this.m = new ArrayList(1);
        this.o = R$drawable.chat_top_arrow;
        b();
    }

    public ImageView a() {
        Drawable drawable = getResources().getDrawable(this.o);
        ImageView imageView = new ImageView(getContext());
        imageView.setImageDrawable(drawable);
        return imageView;
    }

    public final void b() {
        ImageView imageView = this.n;
        if (imageView != null) {
            removeView(imageView);
        }
        ImageView imageViewA = a();
        this.n = imageViewA;
        addView(imageViewA);
    }

    public void c(int i, int i2) {
        Point point = this.c;
        point.x = i;
        point.y = i2;
        if (getMeasuredHeight() != 0 && getMeasuredWidth() != 0) {
            d();
        }
        invalidate();
    }

    public final void d() {
        int measuredWidth = this.c.x - (this.n.getMeasuredWidth() / 2);
        int i = this.c.y;
        float f = measuredWidth;
        if (f != this.n.getTranslationX()) {
            this.n.setTranslationX(f);
        }
        float f2 = i;
        if (f2 != this.n.getTranslationY()) {
            this.n.setTranslationY(f2);
        }
    }

    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new FrameLayout.LayoutParams(-1, -1);
    }

    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new FrameLayout.LayoutParams(getContext(), attributeSet);
    }

    public int getArrowDrawable() {
        return this.o;
    }

    @Override // android.view.ViewGroup
    public void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        view.measure(ViewGroup.getChildMeasureSpec(i, i2, layoutParams.width), ViewGroup.getChildMeasureSpec(i3, i4, layoutParams.height));
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        ImageView imageView = this.n;
        imageView.layout(i, i2, imageView.getMeasuredWidth() + i, this.n.getMeasuredHeight() + i2);
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            ImageView imageView2 = this.n;
            if (childAt != imageView2) {
                childAt.layout(i, imageView2.getMeasuredHeight() + i2 + this.c.y, i3, i4);
            }
        }
    }

    @Override // android.view.View
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int childCount = getChildCount();
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        this.n.measure(iMakeMeasureSpec, iMakeMeasureSpec);
        int measuredHeight = this.n.getMeasuredHeight();
        int size = View.MeasureSpec.getSize(i2);
        int iMakeMeasureSpec2 = size > measuredHeight ? View.MeasureSpec.makeMeasureSpec(size - (this.c.y + measuredHeight), View.MeasureSpec.getMode(i2)) : i2;
        boolean z = (View.MeasureSpec.getMode(i) == 1073741824 && View.MeasureSpec.getMode(iMakeMeasureSpec2) == 1073741824) ? false : true;
        int iCombineMeasuredStates = 0;
        int iMax = 0;
        int iMax2 = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt == this.n || childAt.getVisibility() == 8) {
                iMax2 = iMax2;
                iMax = iMax;
                iCombineMeasuredStates = iCombineMeasuredStates;
            } else {
                measureChildWithMargins(childAt, i, 0, iMakeMeasureSpec2, 0);
                ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
                iMax = Math.max(iMax, childAt.getMeasuredWidth());
                iMax2 = Math.max(iMax2, childAt.getMeasuredHeight());
                iCombineMeasuredStates = View.combineMeasuredStates(iCombineMeasuredStates, childAt.getMeasuredState());
                if (z && (layoutParams.width == -1 || layoutParams.height == -1)) {
                    this.m.add(childAt);
                }
            }
        }
        int i4 = iCombineMeasuredStates;
        setMeasuredDimension(View.resolveSizeAndState(Math.max(iMax, getSuggestedMinimumWidth()), i, i4), View.resolveSizeAndState(Math.max(iMax2, getSuggestedMinimumHeight()), iMakeMeasureSpec2, i4 << 16));
        int size2 = this.m.size();
        if (size2 > 1) {
            for (int i5 = 0; i5 < size2; i5++) {
                View view = (View) this.m.get(i5);
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                int i6 = marginLayoutParams.width;
                int iMakeMeasureSpec3 = i6 == -1 ? View.MeasureSpec.makeMeasureSpec((getMeasuredWidth() - marginLayoutParams.leftMargin) - marginLayoutParams.rightMargin, 1073741824) : ViewGroup.getChildMeasureSpec(i, marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, i6);
                int i7 = marginLayoutParams.height;
                view.measure(iMakeMeasureSpec3, i7 == -1 ? View.MeasureSpec.makeMeasureSpec((getMeasuredHeight() - marginLayoutParams.topMargin) - marginLayoutParams.bottomMargin, 1073741824) : ViewGroup.getChildMeasureSpec(iMakeMeasureSpec2, marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, i7));
            }
        }
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + measuredHeight + this.c.y);
        d();
    }

    @Override // android.view.ViewGroup
    public void removeAllViews() {
        super.removeAllViews();
        addView(this.n);
    }

    public void setArrowDrawable(int i) {
        this.o = i;
        b();
    }

    public UpArrowLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.c = new Point(0, 0);
        this.m = new ArrayList(1);
        this.o = R$drawable.chat_top_arrow;
        b();
    }

    public UpArrowLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.c = new Point(0, 0);
        this.m = new ArrayList(1);
        this.o = R$drawable.chat_top_arrow;
        b();
    }
}

================================================================
