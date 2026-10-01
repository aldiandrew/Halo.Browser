package com.bubble.browser.pro;

import android.R;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/* JADX INFO: loaded from: /home/runner/work/_temp/dex-inputs/classes.dex */
public class PermissionActivity extends Activity {
    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setBackgroundDrawableResource(R.color.transparent);
        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, 200);
        } else {
            finish();
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 200) {
            Toast.makeText(this, (iArr.length <= 0 || iArr[0] != 0) ? "Permission Denied. Downloads may fail." : "Permission Granted", 0).show();
        }
        finish();
    }
}

================================================================
