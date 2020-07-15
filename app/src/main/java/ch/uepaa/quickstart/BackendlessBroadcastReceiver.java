package ch.uepaa.quickstart;

import android.content.Context;
import android.content.Intent;

abstract class BackendlessBroadcastReceiver {
    public abstract boolean onMessage(Context context, Intent intent);
}
