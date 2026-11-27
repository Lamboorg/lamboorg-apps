package org.lamboorg.ultra;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import org.unifiedpush.android.connector.UnifiedPush;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

/**
 * Native push for the app (the WebView can't do web push).
 * UnifiedPush: uses a distributor app on the phone (ntfy etc.) if there is one, otherwise the built-in
 * Google push fallback (embedded FCM distributor). The endpoint + keys go to the site, which saves them
 * like any browser push subscription, so the server sends to the app exactly like to a browser.
 */
final class Push {
    static final String HOME = "https://lamboorg.neocities.org/";
    private static final String P = "lu-push";
    interface Listener { void changed(); }
    static Listener listener;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(P, Context.MODE_PRIVATE); }

    static void changed() { Listener l = listener; if (l != null) l.changed(); }

    /** state for the site: {state: off|pending|on|none|failed, endpoint, p256dh, auth, via, reason} */
    static String state(Context c) {
        SharedPreferences p = prefs(c);
        try {
            JSONObject o = new JSONObject();
            o.put("state", p.getString("state", "off"));
            if (p.contains("endpoint")) { o.put("endpoint", p.getString("endpoint", "")); o.put("p256dh", p.getString("p256dh", "")); o.put("auth", p.getString("auth", "")); }
            o.put("via", p.getString("via", ""));
            o.put("reason", p.getString("reason", ""));
            o.put("asked", p.getBoolean("asked", false));
            return o.toString();
        } catch (Exception e) { return "{\"state\":\"off\"}"; }
    }

    /** pick a distributor by itself: a real one (ntfy, NextPush…) if installed, else the built-in Google fallback */
    static void register(Context c, String vapid) {
        SharedPreferences p = prefs(c);
        p.edit().putString("vapid", vapid).putString("state", "pending").putBoolean("asked", true).remove("reason").apply();
        changed();
        String own = c.getPackageName(), pick = null, saved = p.getString("dist", null);
        List<String> ds = UnifiedPush.getDistributors(c);
        if (saved != null && ds.contains(saved)) pick = saved;
        if (pick == null) for (String d : ds) if (!d.equals(own)) { pick = d; break; }
        if (pick == null && ds.contains(own)) pick = own;
        if (pick == null) { p.edit().putString("state", "none").apply(); changed(); return; }
        p.edit().putString("dist", pick).putString("via", pick.equals(own) ? "google" : pick).apply();
        UnifiedPush.saveDistributor(c, pick);
        UnifiedPush.register(c, INSTANCE_DEFAULT, null, vapid);
    }

    /** the chosen distributor failed (e.g. the Google fallback on a phone without Google services): try the next one */
    static boolean tryNext(Context c) {
        SharedPreferences p = prefs(c);
        String own = c.getPackageName(), cur = p.getString("dist", null), vapid = p.getString("vapid", null);
        if (vapid == null) return false;
        List<String> ds = UnifiedPush.getDistributors(c);
        String tried = p.getString("tried", "") + "|" + cur + "|";
        for (String d : ds) if (!tried.contains("|" + d + "|")) {
            p.edit().putString("tried", tried).putString("dist", d).putString("via", d.equals(own) ? "google" : d).apply();
            UnifiedPush.saveDistributor(c, d);
            UnifiedPush.register(c, INSTANCE_DEFAULT, null, vapid);
            return true;
        }
        return false;
    }

    static void unregister(Context c) {
        try { UnifiedPush.unregister(c, INSTANCE_DEFAULT); } catch (Exception ignored) { }
        prefs(c).edit().remove("endpoint").remove("p256dh").remove("auth").remove("tried").putString("state", "off").apply();
        changed();
    }

    /* ---------- showing a push ---------- */
    static void show(Context c, byte[] content) {
        String title = "Lamboorg Ultra", body = "", url = "./", tag = "n-" + System.currentTimeMillis();
        boolean call = false;
        try {
            JSONObject d = new JSONObject(new String(content, StandardCharsets.UTF_8));
            title = d.optString("title", title); body = d.optString("body", ""); url = d.optString("url", url); tag = d.optString("tag", tag);
            call = "call".equals(d.optString("kind"));
            if (d.has("e2e")) { String txt = openE2E(c, d.getJSONObject("e2e")); if (txt != null) body = txt; }
        } catch (Exception e) { body = new String(content, StandardCharsets.UTF_8); }

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        String ch = call ? "calls" : "messages";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel m = new NotificationChannel("messages", c.getString(R.string.ch_messages), NotificationManager.IMPORTANCE_HIGH);
            m.enableVibration(true);
            nm.createNotificationChannel(m);
            NotificationChannel k = new NotificationChannel("calls", c.getString(R.string.ch_calls), NotificationManager.IMPORTANCE_HIGH);
            k.enableVibration(true); k.setVibrationPattern(new long[]{0, 700, 500, 700, 500, 700, 500, 700});
            nm.createNotificationChannel(k);
        }
        String path = url.startsWith("./") ? url.substring(2) : url.startsWith("/") ? url.substring(1) : url;
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(url.startsWith("http") ? url : HOME + path), c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, tag.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, ch) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notif).setContentTitle(title).setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true).setContentIntent(pi).setWhen(System.currentTimeMillis()).setShowWhen(true)
                .setCategory(call ? Notification.CATEGORY_CALL : Notification.CATEGORY_MESSAGE);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
        if (call) b.setTimeoutAfter(45000);
        nm.notify(tag, 1, b.build());
    }

    /* ---------- encrypted DMs: decrypt here with this phone's keys (copied from the site's key store) ---------- */
    static void saveRing(Context c, String json) { prefs(c).edit().putString("ring", json).apply(); }

    private static byte[] b64(String s) {
        s = s.replace('-', '+').replace('_', '/');
        while (s.length() % 4 != 0) s += "=";
        return android.util.Base64.decode(s, android.util.Base64.DEFAULT);
    }
    private static ECParameterSpec p256() throws Exception {
        AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
        ap.init(new ECGenParameterSpec("secp256r1"));
        return ap.getParameterSpec(ECParameterSpec.class);
    }
    private static byte[] gcm(byte[] key, byte[] iv, byte[] ct) throws Exception {
        Cipher ci = Cipher.getInstance("AES/GCM/NoPadding");
        ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return ci.doFinal(ct);
    }
    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key, "HmacSHA256")); return m.doFinal(data);
    }

    static String openE2E(Context c, JSONObject x) {
        try {
            JSONArray ring = new JSONArray(prefs(c).getString("ring", "[]"));
            JSONObject k = x.getJSONObject("k");
            ECParameterSpec spec = p256();
            KeyFactory kf = KeyFactory.getInstance("EC");
            byte[] pubRaw = b64(x.getString("pub"));
            PublicKey pub = kf.generatePublic(new ECPublicKeySpec(new ECPoint(new BigInteger(1, Arrays.copyOfRange(pubRaw, 1, 33)), new BigInteger(1, Arrays.copyOfRange(pubRaw, 33, 65))), spec));
            for (int i = 0; i < ring.length(); i++) {
                JSONObject r = ring.getJSONObject(i);
                String id = r.getString("id");
                if (!k.has(id)) continue;
                try {
                    PrivateKey priv = kf.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, b64(r.getJSONObject("jwk").getString("d"))), spec));
                    KeyAgreement ka = KeyAgreement.getInstance("ECDH"); ka.init(priv); ka.doPhase(pub, true);
                    byte[] bits = ka.generateSecret();
                    // HKDF-SHA256 (same as the site): salt "lamboorg-e2e-v1", info = both device ids sorted, joined with |
                    String a = id, f = x.getString("f");
                    String info = a.compareTo(f) <= 0 ? a + "|" + f : f + "|" + a;
                    byte[] prk = hmac("lamboorg-e2e-v1".getBytes(StandardCharsets.UTF_8), bits);
                    byte[] in = info.getBytes(StandardCharsets.UTF_8), t = new byte[in.length + 1];
                    System.arraycopy(in, 0, t, 0, in.length); t[in.length] = 1;
                    byte[] wrapKey = Arrays.copyOf(hmac(prk, t), 32);
                    String[] w = k.getString(id).split("\\.");
                    byte[] msgKey = gcm(wrapKey, b64(w[0]), b64(w[1]));
                    JSONObject p = new JSONObject(new String(gcm(msgKey, b64(x.getString("iv")), b64(x.getString("ct"))), StandardCharsets.UTF_8));
                    String txt = p.optString("b", "").trim();
                    if (!txt.isEmpty()) return txt.length() > 140 ? txt.substring(0, 139) + "…" : txt;
                    JSONArray m = p.optJSONArray("m");
                    return m != null && m.length() > 0 ? "📷" : null;
                } catch (Exception ignored) { /* try the next key */ }
            }
        } catch (Exception ignored) { }
        return null;
    }
}
