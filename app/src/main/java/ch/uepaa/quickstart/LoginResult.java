package ch.uepaa.quickstart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.DeviceRegistration;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;
import com.backendless.push.DeviceRegistrationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LoginResult extends Activity {
    static final String userInfo_key = "BackendlessUserInfo";
    static final String userId_key = "BackendlessUserId";
    static final String deviceId_key = "";
    static final String logoutButtonState_key = "LogoutButtonState";
    static final String editButtonState_key = "EditButtonState";
    static final String continueButtonState_key = "ContinueButtonState";

    //private EditText backendlessUserInfo;
    private Button bkndlsLogoutButton;
    private Button bkndlsEditButton;
    private Button continueButton;

    private String message;
    private String userId;
    private String ownerId;
    private String objectIdUser;
    private String flagMessaging;
    private String deviceId;
    private Boolean flagErrorDeviceId = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("LoginResult", "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login_result);

        bkndlsLogoutButton = (Button) findViewById(R.id.button_bkndlsBackendlessLogout);
        bkndlsEditButton = (Button) findViewById(R.id.button_bkndlsBackendlessEdit);
        continueButton = (Button) findViewById(R.id.continueButton);

        continueButton.setBackgroundColor(getResources().getColor(R.color.btn_disable));
        continueButton.setText(getResources().getText(R.string.grabando));

        Intent intent = getIntent();
        message = intent.getStringExtra(userInfo_key);
        message = message == null ? "" : message;
        userId = intent.getStringExtra(userId_key);
        userId = userId == null ? "" : userId;
        boolean logoutButtonState = intent.getBooleanExtra(logoutButtonState_key, true);
        boolean editButtonState = intent.getBooleanExtra(editButtonState_key, true);
        boolean continueButtonState = intent.getBooleanExtra(continueButtonState_key, true);

        Log.i("LoginResult", "message: " + message);
        Log.i("LoginResult", "userId: " + userId);

        if (logoutButtonState) {
            bkndlsLogoutButton.setVisibility(View.VISIBLE);
            bkndlsEditButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.VISIBLE);
            //backendlessUserInfo.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.black, null));
        }
        else {
            bkndlsLogoutButton.setVisibility(View.INVISIBLE);
            bkndlsEditButton.setVisibility(View.INVISIBLE);
            continueButton.setVisibility(View.INVISIBLE);
            //backendlessUserInfo.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.holo_red_dark, null));
        }
        //backendlessUserInfo.setText(message);

        initUIBehaviour();
        loadDeviceId();
    }


    private void initUIBehaviour() {
        Log.i("LoginResult", "initUIBehaviour");
        bkndlsLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutFromBackendless();
            }
        });
        bkndlsEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFromBackendless();
            }
        });
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueRunActivity();
            }
        });
    }

    private void initNotification() {
        Log.i("LoginResult", "initNotification INIT");

        List<String> channels = new ArrayList<String>();
        channels.add( "default" );
        Context context = getApplicationContext();
        Backendless.Messaging.registerDevice(channels, new AsyncCallback<DeviceRegistrationResult>() {
            @Override
            public void handleResponse(DeviceRegistrationResult response) {
                Log.i("LoginResult", "initNotification response: " + response.toString());

                //Leer deviceId
                readDeviceId();

            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.i("LoginResult", "initNotification response error: " + fault);
                Toast.makeText( context, "Error registro mensajes" + fault.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void readDeviceId() {
        Log.i("LoginResult", "readDeviceId INIT");

        Backendless.Messaging.getDeviceRegistration( new AsyncCallback<DeviceRegistration>() {
            @Override
            public void handleResponse(DeviceRegistration response) {
                Log.i("LoginResult", "response: " + response.getDeviceId());
                deviceId = response.getDeviceId();
                addDeviceId();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.i("LoginResult", "fault: " + fault);
            }
        });

    }

    private void addDeviceId() {
        Log.i("LoginResult", "addDeviceId: " + deviceId);

        if (deviceId == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginResult.this);
            builder.setMessage(message);
            builder.setTitle(R.string.device_unsuccess);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("flagMessaging", "True" );
        editor.apply();

        Toast.makeText( LoginResult.this, R.string.device_success,
                Toast.LENGTH_LONG).show();

        continueButton.setEnabled(true);
        continueButton.setBackgroundColor(getResources().getColor(R.color.backgrounds ));
        continueButton.setText(getResources().getText(R.string.btn_continue));

        Log.i("LoginResult", "INIT");
        String whereClause = "ownerId = '" + userId + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );

        Backendless.Data.of("User").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> facemap )
                    {

                        Map face = facemap.get(0);
                        face.put("deviceId", deviceId);
                        Log.i("LoginResult", "updateUser: " + deviceId);

                        Backendless.Data.of( "User" ).save( face, new AsyncCallback<Map>() {
                            @Override
                            public void handleResponse( Map response )
                            {
                                // Contact instance has been updated
                                Log.i("LoginResult", "User deviceId has been updated: " + response);
                                objectIdUser = Objects.toString(response.get("objectId"));
                                Log.i("LoginResult", "User objectIdUser: " + objectIdUser);
                                // Graba los datos de memoria
                                SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("objectIdUser", objectIdUser );
                                editor.apply();

                            }
                            @Override
                            public void handleFault( BackendlessFault fault )
                            {
                                // an error has occurred, the error code can be retrieved with fault.getCode()
                                Log.i("LoginResult", "Error: " + fault);

                            }
                        } );

                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("EditarActivity", "Error: " + fault);

                    }
                });


    }

    private void logoutFromBackendless(){
        Log.i("LoginResult", "logoutFromBackendless");

        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("identity", "" );
        editor.putString("password", "" );
        editor.apply();


        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                //backendlessUserInfo.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.black, null));
                //backendlessUserInfo.setText("");
                bkndlsLogoutButton.setVisibility(View.INVISIBLE);
                termina();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                //backendlessUserInfo.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.holo_red_dark, null));
                //backendlessUserInfo.setText(fault.toString());
            }
        });
    }

    private void editFromBackendless(){
        Log.i("LoginResult", "editFromBackendless");
        Log.i("LoginResult2", "ownerId: " + userId);

        Intent myIntent = new Intent(this, EditarActivity.class);
        myIntent.putExtra(EditarActivity.ownerIdIntent, userId);
        startActivity(myIntent);
    }

    private void continueRunActivity(){
        Log.i("LoginResult", "continueRunActivity INIT");
        Log.i("LoginResult2", "objectIdUser: " + objectIdUser);
        Log.i("LoginResult2", "userId: " + userId);
        Log.i("LoginResult2", "deviceId: " + deviceId);

        if (!deviceId.isEmpty()) {
            // Si tengo no hay problema
        } else if (flagErrorDeviceId) {
                // Intentó pero no lo logro
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginResult.this);
                builder.setMessage(message);
                builder.setTitle(R.string.device_unsuccess);
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                // Estoy en problemas lo necesito
                flagErrorDeviceId = true;
                initNotification();
            }

        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
        objectIdUser = settings.getString("objectIdUser", null);
        Intent myIntent = new Intent(this, ServiceCheck.class);
        myIntent.putExtra(ServiceCheck.objectIdUserIntent, objectIdUser);
        myIntent.putExtra(ServiceCheck.userIdIntent, userId);
        myIntent.putExtra(ServiceCheck.deviceIdIntent, deviceId);
        startActivity(myIntent);


        // Test Code
/*
       // Test Patient Check
       String peerIdPatient = "8e863e64-ae1d-423c-b8dc-7b0272d282fa";
       GlobalVar.getInstance().setGlobalVar1(peerIdPatient);
       Intent myIntent = new Intent(this, PatientCheck.class);

        //Test Directo a Doctor Check
        Intent myIntent = new Intent(this, DoctorCheck.class);
        myIntent.putExtra(DoctorCheck.objectIdDocIntent, "E43BB14F-4F23-2523-FF63-51031C5AC200"); //Manda el ObjectID de Patient / Tratamiento
        startActivity(myIntent);


        //Test Directo a Photo Checked
        Intent myIntent = new Intent(this, PatientPhotos.class);
        myIntent.putExtra(PatientPhotos.objectIdPacIntent, "E43BB14F-4F23-2523-FF63-51031C5AC200"); //Manda el ObjectID de Patient / Tratamiento
        startActivity(myIntent);
*/
    }

    public void termina() {
        Log.i("LoginResult", "finish");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void loadDeviceId() {
        Log.i("LoginResult", "loadDeviceId");
        String whereClause = "ownerId = '" + userId + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );

        Backendless.Data.of("User").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> facemap )
                    {

                        Map face = facemap.get(0);
                        Log.i("LoginResult", "database: " + face);
                        deviceId = Objects.toString(face.get("deviceId"));
                        objectIdUser = Objects.toString(face.get("objectId"));
                        Log.i("LoginResult", "updateUser: " + deviceId);
                        Log.i("LoginResult", "objectIdUser: " + objectIdUser);

                        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("objectIdUser", objectIdUser );
                        editor.apply();

                        if (deviceId.equals("null") || deviceId.isEmpty()) {
                            Log.i("LoginResult", "deviceId == null");
                            //No tiene es nuevo
                            initNotification();
                        } else {
                            Log.i("LoginResult", "deviceId != null");
                            continueButton.setEnabled(true);
                            continueButton.setBackgroundColor(getResources().getColor(R.color.backgrounds ));
                            continueButton.setText(getResources().getText(R.string.btn_continue));
                        }

                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("LoginResult", "Error: " + fault);

                    }
                });
    }
}

