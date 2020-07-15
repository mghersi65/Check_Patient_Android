package ch.uepaa.quickstart;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.DeviceRegistration;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.push.DeviceRegistrationResult;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.RemoteMessage;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;


public class ServiceCheck extends Activity {

    private Button bluetoothButton, alertasButton, ubicacionButton;
    private String objectId;
    private String userId;
    private String deviceId;
    static final String userIdIntent = "BackendlessUserId";
    static final String objectIdUserIntent = "BackendlessObjectId";
    static final String deviceIdIntent = "BackendlessDeviceId";
    boolean bluetoothFlag = false;
    boolean alertasFlag = false;
    boolean ubicacionFlag = false;
    LocationManager locationManager ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("ServiceCheck", "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_service_check);

        Intent intent = getIntent();
        userId = intent.getStringExtra(userIdIntent);
        objectId = intent.getStringExtra(objectIdUserIntent);
        deviceId = intent.getStringExtra(deviceIdIntent);

        initUI();
        initUIBehaviour();
        initCheck();

    }


    private void initUI() {
        Log.i("ServiceCheck", "initUI");
        bluetoothButton = (Button) findViewById(R.id.BluetoothBtn);
        alertasButton = (Button) findViewById(R.id.AlertasBtn);
        ubicacionButton = (Button) findViewById(R.id.UbicacionBtn);

        bluetoothButton.setEnabled(false);
        alertasButton.setEnabled(false);
        ubicacionButton.setEnabled(false);

    }

    private void initUIBehaviour() {
        Log.i("ServiceCheck", "initUIBehaviour");

        // backendless
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBluetoothButtonClicked();
            }
        });
        alertasButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAlertasButtonClicked();
            }
        });
        ubicacionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUbicacionButtonClicked();
            }
        });

    }

    private void initCheck() {
        Log.i("ServiceCheck", "initCheck");
        //Bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            AlertDialog.Builder builder = new AlertDialog.Builder(ServiceCheck.this);
            builder.setMessage(R.string.bluetooth_message);
            builder.setTitle(R.string.bluetooth_title);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
            bluetoothButton.setEnabled(true);
        } else {
            // Bluetooth is enabled
            bluetoothButton.setEnabled(false);
            bluetoothButton.setBackgroundColor(getResources().getColor(R.color.verde ));
            bluetoothFlag = true;
        }

        //Alertas
        if (areNotificationsEnabled()) {
            // Conectada
            alertasButton.setEnabled(false);
            alertasButton.setBackgroundColor(getResources().getColor(R.color.verde));
            alertasFlag = true;
        } else {
            alertasButton.setEnabled(true);
        }

        //Ubicacion
        if (canGetLocation()) {
            ubicacionButton.setEnabled(false);
            ubicacionButton.setBackgroundColor(getResources().getColor(R.color.verde));
            ubicacionFlag = true;
        } else {
            ubicacionButton.setEnabled(true);

        }

        if (bluetoothFlag && alertasFlag && ubicacionFlag) {
            continuar();
        }


    }

    private void onBluetoothButtonClicked() {
        Log.i("ServiceCheck", "onBluetoothButtonClicked");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        initCheck();

    }


    private void onAlertasButtonClicked() {
        Log.i("ServiceCheck", "onAlertasButtonClicked");
        AlertDialog.Builder builder = new AlertDialog.Builder(ServiceCheck.this);
        builder.setMessage(R.string.alertas_message);
        builder.setTitle(R.string.alertas_title);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                initCheck();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onUbicacionButtonClicked() {
        Log.i("ServiceCheck", "onUbicacionButtonClicked");
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        initCheck();

    }


    private void continuar() {
        Intent myIntent = new Intent(this, RunActivity.class);
        myIntent.putExtra(RunActivity.objectIdUserIntent, objectId);
        myIntent.putExtra(RunActivity.userIdIntent, userId);
        myIntent.putExtra(RunActivity.deviceIdIntent, deviceId);
        startActivity(myIntent);
    }

    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.areNotificationsEnabled()) {
                return false;
            }
            List<NotificationChannel> channels = manager.getNotificationChannels();
            for (NotificationChannel channel : channels) {
                if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    return false;
                }
            }
            return true;
        } else {
            return NotificationManagerCompat.from(this).areNotificationsEnabled();
        }
    }

    public boolean canGetLocation() {
        boolean result = true;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        if (locationManager == null)
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // exceptions will be thrown if provider is not permitted.
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }
        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (gps_enabled == false || network_enabled == false) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }


}

