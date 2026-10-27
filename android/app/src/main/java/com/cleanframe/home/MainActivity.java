package com.cleanframe.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedDispatcher;

import java.io.IOException;
import java.io.InputStream;

/* Hosts the shared CleanFrame UI from bundled assets on a private https origin. */
public class MainActivity extends Activity {
    static final String ORIGIN = "https://appassets.androidplatform.net";

    private WebView web;
    private FrameBridge bridge;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        bridge = new FrameBridge(this);
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) WebView.setWebContentsDebuggingEnabled(true);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setDefaultFocusHighlightEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                Log.i("CleanFrame", m.message() + " (" + m.sourceId() + ":" + m.lineNumber() + ")");
                return true;
            }
        });
        web.addJavascriptInterface(bridge, "CleanFrameAndroid");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return serve(req);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !req.getUrl().toString().startsWith(ORIGIN + "/");
            }
        });

        // CleanFrame is drawn for a 1920px stage; zoom so the page viewport is 1920 CSS px at any density.
        web.setInitialScale(Math.round(getResources().getDisplayMetrics().widthPixels * 100f / 1920f));
        setContentView(web);
        web.loadUrl(ORIGIN + "/index.html");
        web.requestFocus();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        }
    }

    private WebResourceResponse serve(WebResourceRequest req) {
        if (!ORIGIN.equals(req.getUrl().getScheme() + "://" + req.getUrl().getHost())) return null;
        String path = req.getUrl().getPath();
        if (path == null || path.contains("..")) return notFound();
        if (path.startsWith("/cf-icon/")) {
            byte[] png = bridge.iconPng(path.substring("/cf-icon/".length()).replace(".png", ""));
            if (png == null) return notFound();
            return new WebResourceResponse("image/png", null, new java.io.ByteArrayInputStream(png));
        }
        try {
            InputStream in = getAssets().open("web" + (path.equals("/") ? "/index.html" : path));
            return new WebResourceResponse(mime(path), mime(path).startsWith("text") || path.endsWith(".js") ? "utf-8" : null, in);
        } catch (IOException e) {
            return notFound();
        }
    }

    private static WebResourceResponse notFound() {
        WebResourceResponse r = new WebResourceResponse("text/plain", "utf-8", null);
        r.setStatusCodeAndReasonPhrase(404, "Not Found");
        return r;
    }

    private static String mime(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".woff")) return "font/woff";
        if (p.endsWith(".ttf")) return "font/ttf";
        return "text/plain";
    }

    /* Android delivers Back to the activity, not the page; route it through the UI's own navigation. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        int code = ev.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            if (ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 0) {
                back();
            }
            return true;
        }
        return super.dispatchKeyEvent(ev);
    }

    private void back() {
        web.evaluateJavascript("window.ShoreQA && window.ShoreQA.nav('back') && 0", null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        web.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.requestFocus();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
