package com.cleanframe.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* The JavaScript side is android/web/device.js. Every method returns synchronously; errors are strings. */
public class FrameBridge {
    private static final String[] STOCK_HOMES = {
        "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.leanbacklauncher"
    };

    private final Activity activity;
    private final PackageManager pm;
    private final Map<String, byte[]> icons = new HashMap<>();

    FrameBridge(Activity activity) {
        this.activity = activity;
        this.pm = activity.getPackageManager();
    }

    @JavascriptInterface
    public String apps() {
        JSONArray out = new JSONArray();
        Map<String, Boolean> seen = new HashMap<>();
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER, out, seen);
        collect(Intent.CATEGORY_LAUNCHER, out, seen);
        return out.toString();
    }

    private void collect(String category, JSONArray out, Map<String, Boolean> seen) {
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> list = pm.queryIntentActivities(probe, 0);
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(activity.getPackageName()) || seen.containsKey(pkg)) continue;
            /* Phone-only apps also appear under LAUNCHER; on a TV keep only ones with a TV entry point. */
            if (Intent.CATEGORY_LAUNCHER.equals(category) && pm.getLeanbackLaunchIntentForPackage(pkg) == null
                && pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) continue;
            seen.put(pkg, true);
            try {
                JSONObject a = new JSONObject();
                a.put("id", pkg);
                a.put("name", String.valueOf(ri.loadLabel(pm)));
                out.put(a);
            } catch (Exception ignored) { }
        }
    }

    byte[] iconPng(String pkg) {
        synchronized (icons) {
            if (icons.containsKey(pkg)) return icons.get(pkg);
        }
        byte[] png = null;
        try {
            Drawable d = pm.getApplicationIcon(pkg);
            int size = 192;
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, size, size);
            d.draw(new Canvas(bmp));
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, buf);
            png = buf.toByteArray();
        } catch (PackageManager.NameNotFoundException ignored) { }
        synchronized (icons) { icons.put(pkg, png); }
        return png;
    }

    @JavascriptInterface
    public String launch(String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return "This app is not installed.";
        return start(i);
    }

    @JavascriptInterface
    public String home() {
        for (String pkg : STOCK_HOMES) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg);
            if (pm.resolveActivity(i, 0) != null) return start(i);
        }
        return "The stock home screen is not available.";
    }

    @JavascriptInterface
    public String settings() {
        return start(new Intent(Settings.ACTION_SETTINGS));
    }

    /* Nearest thing to Art Mode: start the screensaver, or open its settings. */
    @JavascriptInterface
    public String screensaver() {
        Intent dream = new Intent(Intent.ACTION_MAIN)
            .setComponent(new ComponentName("com.android.systemui", "com.android.systemui.Somnambulator"));
        if (pm.resolveActivity(dream, 0) != null && start(dream).isEmpty()) return "";
        return start(new Intent(Settings.ACTION_DREAM_SETTINGS));
    }

    private String start(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(i);
            return "";
        } catch (ActivityNotFoundException | SecurityException e) {
            return "The TV could not open this screen.";
        }
    }
}
