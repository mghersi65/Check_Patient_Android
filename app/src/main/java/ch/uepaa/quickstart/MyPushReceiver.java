package ch.uepaa.quickstart;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;

import com.backendless.push.BackendlessFCMService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyPushReceiver extends BackendlessFCMService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MyPushReceiver", "onCreate");
    }

    @Override
    public boolean onMessage(Context appContext, Intent msgIntent) {
        Log.i("MyPushReceiver", "onMessage");
        Log.i("MyPushReceiver", "msgIntent: " + msgIntent);

       // String message = msgIntent.getExtras().getString("price");
        String message = Objects.toString(msgIntent.getExtras());
        Log.i("MyPushReceiver", "message: " + message);

        //Definir si es Android o iPhone el envio
        // String userIdDocFull = msgIntent.getExtras().getString("android-content-text");
        String userIdDocFull = Objects.toString(msgIntent.getExtras());
        Log.i("MyPushReceiver", "userIdDocFull: " + userIdDocFull);

        if (userIdDocFull.equals("null") || userIdDocFull.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MyPushReceiver.this);
            builder.setMessage(R.string.alertas_message);
            builder.setTitle(R.string.error_mensaje);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //Nothing
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            String[] splitArray = userIdDocFull.split("@");
            //ID del Patient
            String objectId = splitArray[1];
            Log.i("MyPushReceiver", "objectId: " + objectId);
            if (objectId == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MyPushReceiver.this);
                builder.setMessage(R.string.alertas_message);
                builder.setTitle(R.string.error_mensaje);
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Nothing
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            } else {

                String DocPac = splitArray[0];
                Log.i("MyPushReceiver", "objectId: " + objectId);

                if (DocPac.equals("DOC")) {
                    // Pantalla de DOC para sacar las fotos del Paciente
                    Intent myIntent = new Intent(this, DoctorCheck.class);
                    myIntent.putExtra(DoctorCheck.objectIdDocIntent, objectId);
                    myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(myIntent);

                } else {
                    // Pantalla de PC para recibir las fotos del paciente
                    Intent myIntent = new Intent(this, PatientPhotos.class);
                    myIntent.putExtra(PatientPhotos.objectIdPacIntent, objectId);
                    myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(myIntent);

                }
            }
        }

        // return super.onMessage(appContext, msgIntent);
        return true;
    }

    public MyPushReceiver() {
        super();
    }

}