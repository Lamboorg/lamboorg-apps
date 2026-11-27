package org.lamboorg.ultra;

import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PublicKeySet;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

/** receives push events from the distributor (ntfy etc. or the built-in Google fallback) */
public class PushServiceImpl extends PushService {
    @Override
    public void onNewEndpoint(PushEndpoint endpoint, String instance) {
        PublicKeySet k = endpoint.getPubKeySet();
        android.content.SharedPreferences.Editor e = Push.prefs(this).edit().putString("endpoint", endpoint.getUrl()).putString("state", "on").remove("reason");
        if (k != null) e.putString("p256dh", k.getPubKey()).putString("auth", k.getAuth());
        e.apply();
        Push.changed();
    }

    @Override
    public void onMessage(PushMessage message, String instance) {
        Push.show(this, message.getContent());
    }

    @Override
    public void onRegistrationFailed(FailedReason reason, String instance) {
        if (Push.tryNext(this)) return;
        Push.prefs(this).edit().putString("state", "failed").putString("reason", String.valueOf(reason)).apply();
        Push.changed();
    }

    @Override
    public void onUnregistered(String instance) {
        Push.prefs(this).edit().remove("endpoint").remove("p256dh").remove("auth").putString("state", "off").apply();
        Push.changed();
    }
}
