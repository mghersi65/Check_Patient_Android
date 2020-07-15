package ch.uepaa.quickstart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.backendless.Backendless;

import java.util.HashMap;
import java.util.Map;

public class BacklessBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = BacklessBroadcastReceiver.class.getSimpleName();
    private static final String EXTRA_WAKE_LOCK_ID = "com.backendless.wakelockid";
    private static final Map<Integer, PowerManager.WakeLock> activeWakeLocks = new HashMap<>();
    public static final String EXTRA_MESSAGE_ID = "com.backendless.messageid";

    private static int mNextId = 1;

    public BacklessBroadcastReceiver()
    {
        Log.i("BackBroadcastReceiver", "INIT");

    }

    public Class<? extends BackendlessPushService> getServiceClass()
    {
        return BackendlessPushService.class;
    }

    /**
     * @deprecated Extend {@link BackendlessPushService} instead.
     */
    @Deprecated
    public void onRegistered( Context context, String registrationId )
    {
    }

    /**
     * @deprecated Extend {@link BackendlessPushService} instead.
     */
    @Deprecated
    public void onUnregistered( Context context, Boolean unregistered )
    {
    }

    /**
     * @deprecated Extend {@link BackendlessPushService} instead.
     */
    @Deprecated
    public boolean onMessage( Context context, Intent intent )
    {
        return true;
    }

    /**
     * @deprecated Extend {@link BackendlessPushService} instead.
     */
    @Deprecated
    public void onError( Context context, String message )
    {
        throw new RuntimeException( message );
    }

    @Override
    public final void onReceive( Context context, Intent intent )
    {
        if( !Backendless.isInitialized() )
            Backendless.initApplicationFromProperties( context );

        BackendlessPushService.enqueueWork( context, getServiceClass(), intent );
    }
}