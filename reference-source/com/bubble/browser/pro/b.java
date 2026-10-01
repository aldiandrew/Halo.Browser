package com.bubble.browser.pro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.browser.browser.pro.R$id;
import com.browser.browser.pro.R$layout;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class b extends RecyclerView.Adapter {
    public a[] d;

    public static class a {
        public int a;
        public String b;
        public String c;

        public a(int i, String str, String str2) {
            this.a = i;
            this.b = str;
            this.c = str2;
        }
    }

    /* JADX INFO: renamed from: com.bubble.browser.pro.b$b, reason: collision with other inner class name */
    public static class C0054b extends RecyclerView.E {
        public ImageView u;
        public TextView v;
        public TextView w;

        public C0054b(View view) {
            super(view);
            this.u = (ImageView) view.findViewById(R$id.intro_image);
            this.v = (TextView) view.findViewById(R$id.intro_title);
            this.w = (TextView) view.findViewById(R$id.intro_description);
        }
    }

    public b(a[] aVarArr) {
        this.d = aVarArr;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int d() {
        return this.d.length;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    /* JADX INFO: renamed from: v, reason: merged with bridge method [inline-methods] */
    public void k(C0054b c0054b, int i) {
        a aVar = this.d[i];
        c0054b.u.setImageResource(aVar.a);
        c0054b.v.setText(aVar.b);
        c0054b.w.setText(aVar.c);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    /* JADX INFO: renamed from: w, reason: merged with bridge method [inline-methods] */
    public C0054b m(ViewGroup viewGroup, int i) {
        return new C0054b(LayoutInflater.from(viewGroup.getContext()).inflate(R$layout.intro_page_layout, viewGroup, false));
    }
}

================================================================
