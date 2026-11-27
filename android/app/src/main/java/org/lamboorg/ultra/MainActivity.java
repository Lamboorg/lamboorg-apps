package org.lamboorg.ultra;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Lamboorg Ultra for Android: the live site in a full-screen app window.
 * No browser bar, no tabs, its own icon and splash screen. The site updates itself,
 * so the app never needs an update for new features.
 */
public class MainActivity extends Activity {
    private static final String HOST = "lamboorg.neocities.org";
    private static final String HOME = "https://" + HOST + "/";
    private static final int REQ_FILE = 1, REQ_PERM = 2, REQ_NOTIF = 3;
    private String pendingVapid;

    private FrameLayout root;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPermission;
    private View fullVideo;
    private WebChromeClient.CustomViewCallback fullVideoCallback;
    private WebChromeClient chrome;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // phones stay upright (Ultra OS is built for portrait); tablets may rotate
        if (getResources().getConfiguration().smallestScreenWidthDp < 600) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#050506"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#050506"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        immersive();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setUserAgentString(s.getUserAgentString() + " LamboorgApp/" + BuildConfig.VERSION_NAME);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("https".equals(u.getScheme()) && HOST.equals(u.getHost())) return false;   // our site: stay in the app
                openOutside(u);                                                                // anything else: the normal browser / app
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showOffline();
            }
        });

        chrome = new WebChromeClient() {
            // picking photos / videos for posts and chats
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    Intent i = params.createIntent();
                    if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(i, REQ_FILE);
                } catch (ActivityNotFoundException e) { fileCallback = null; return false; }
                return true;
            }

            // camera / microphone for calls and the QR code scanner
            @Override
            public void onPermissionRequest(PermissionRequest req) {
                if (!HOST.equals(req.getOrigin().getHost())) { req.deny(); return; }
                List<String> need = new ArrayList<>();
                for (String r : req.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r) && !granted(Manifest.permission.CAMERA)) need.add(Manifest.permission.CAMERA);
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r) && !granted(Manifest.permission.RECORD_AUDIO)) need.add(Manifest.permission.RECORD_AUDIO);
                }
                if (need.isEmpty()) { req.grant(req.getResources()); return; }
                pendingPermission = req;
                requestPermissions(need.toArray(new String[0]), REQ_PERM);
            }

            // full-screen video
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (fullVideo != null) { cb.onCustomViewHidden(); return; }
                fullVideo = view; fullVideoCallback = cb;
                root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                if (fullVideo == null) return;
                root.removeView(fullVideo); fullVideo = null;
                if (fullVideoCallback != null) fullVideoCallback.onCustomViewHidden();
                fullVideoCallback = null;
            }
        };
        web.setWebChromeClient(chrome);

        web.setDownloadListener((url, ua, cd, mime, len) -> openOutside(Uri.parse(url)));

        // native push for the site (the WebView itself can't do web push): window.LamboorgNative
        web.addJavascriptInterface(new Bridge(), "LamboorgNative");
        Push.listener = () -> runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.LU_nativePush&&window.LU_nativePush()", null); });

        if (state != null) web.restoreState(state);
        else {
            Uri link = getIntent() != null ? getIntent().getData() : null;
            web.loadUrl(link != null && HOST.equals(link.getHost()) ? link.toString() : HOME);
        }
    }

    /* full screen: no status bar, no navigation bar (swipe from the edge shows them for a moment).
       The keyboard still pushes the page up so the chat input stays visible. */
    private void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                Insets cut = insets.getInsets(WindowInsets.Type.displayCutout());
                v.setPadding(cut.left, cut.top, cut.right, Math.max(ime.bottom, cut.bottom));
                return WindowInsets.CONSUMED;
            });
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) immersive();
    }

    private boolean granted(String p) { return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    private void openOutside(Uri u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) { }
    }

    private void showOffline() {
        String html = "<html><body style=\"margin:0;height:100vh;display:grid;place-items:center;background:#050506;color:#9ff3ff;font-family:monospace;text-align:center\">"
                + "<div><p style=\"font-size:20px\">LAMBOORG ULTRA</p><p style=\"color:#8b8c94\">no connection</p>"
                + "<button onclick=\"location.href='" + HOME + "'\" style=\"margin-top:14px;padding:12px 22px;border-radius:22px;border:1px solid #9ff3ff;background:none;color:#9ff3ff;font:16px monospace\">try again</button></div></body></html>";
        web.loadDataWithBaseURL(HOME, html, "text/html", "utf-8", HOME);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(pickResult(res, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    private Uri[] pickResult(int res, Intent data) {
        if (res != RESULT_OK || data == null) return null;
        if (data.getClipData() != null) {
            Uri[] out = new Uri[data.getClipData().getItemCount()];
            for (int i = 0; i < out.length; i++) out[i] = data.getClipData().getItemAt(i).getUri();
            return out;
        }
        return WebChromeClient.FileChooserParams.parseResult(res, data);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (req == REQ_NOTIF) {
            boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            if (ok && pendingVapid != null) Push.register(this, pendingVapid);
            else { Push.prefs(this).edit().putString("state", "blocked").putBoolean("asked", true).apply(); Push.changed(); }
            pendingVapid = null;
            return;
        }
        if (req == REQ_PERM && pendingPermission != null) {
            boolean ok = true;
            for (int r : results) ok &= r == PackageManager.PERMISSION_GRANTED;
            if (ok) pendingPermission.grant(pendingPermission.getResources()); else pendingPermission.deny();
            pendingPermission = null;
        }
    }

    /** what the site can call: push on/off, push state, and a copy of the chat keys so notifications can show the message */
    private class Bridge {
        @JavascriptInterface public String pushState() { return Push.state(MainActivity.this); }
        @JavascriptInterface public void pushEnable(String vapid) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingVapid = vapid; requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF); return;
                }
                Push.register(MainActivity.this, vapid);
            });
        }
        @JavascriptInterface public void pushDisable() { Push.unregister(MainActivity.this); }
        @JavascriptInterface public void saveRing(String json) { Push.saveRing(MainActivity.this, json); }
        @JavascriptInterface public String version() { return BuildConfig.VERSION_NAME; }
    }

    @Override
    protected void onDestroy() { Push.listener = null; super.onDestroy(); }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri link = intent.getData();
        if (link != null && HOST.equals(link.getHost())) web.loadUrl(link.toString());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (fullVideo != null) { chrome.onHideCustomView(); return; }
        if (web.canGoBack()) web.goBack();
        else moveTaskToBack(true);   // like a real app: leaves it running in the background
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() { super.onPause(); CookieManager.getInstance().flush(); }

    @Override
    protected void onDestroy() { if (web != null) web.destroy(); super.onDestroy(); }
}
