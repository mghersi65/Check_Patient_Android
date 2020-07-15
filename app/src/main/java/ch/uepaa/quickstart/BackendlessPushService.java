package ch.uepaa.quickstart;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

class BackendlessPushService {
    public static void enqueueWork(Context context, Class<? extends BackendlessPushService> serviceClass, Intent intent) {

        Log.i("BackendlessPushService", "INIT");

    }
}
