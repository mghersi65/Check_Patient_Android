package ch.uepaa.quickstart;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.DeviceRegistration;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.messaging.DeliveryOptions;
import com.backendless.messaging.MessageStatus;
import com.backendless.messaging.PublishOptions;
import com.backendless.persistence.DataQueryBuilder;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static ch.uepaa.quickstart.Defaults.API_KEY;
import static ch.uepaa.quickstart.Defaults.APPLICATION_ID;
import static ch.uepaa.quickstart.Defaults.SERVER_URL;

public class PatientCheck extends Activity {

    private final static java.text.SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy/MM/dd");

    static final String userIdUser = "BackendlessUserId";
    static final String objectIdUser = "BackendlessObjectId";
    static final String deviceIdUser = "BackendlessDeviceId";
    static final String peerIdUser = "BackendlessPeerId";
    private String userIdDoc;
    private String objectIdDoc;
    private String deviceIdDoc;
    private String peerIdDoc;
    private String objectIdTratamiento; //Es el Objet si está grabado previamente

    private EditText nameField;
    private EditText dniField;
    private Button checkButton;
    private Button cancelButton;
    private ImageView imageProfile;

    private String name;
    private String dni;
    private String filepath;
    private String ownerId;
    private Intent data;
    private String peerId;
    private String deviceId;

    private BackendlessUser user;
    private Patients patient = new Patients();

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
    private static String imageStoragePath;
    // key to store image path in savedInstance state
    public static final String KEY_IMAGE_STORAGE_PATH = "image_path";
    private Boolean flagPhoto = false;
    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";
    // Bitmap sampling size
    public static final int BITMAP_SAMPLE_SIZE = 8;
    private String imageFilePath;
    private String imageFilePathCamera;
    private String imageProfileLocation;
    final String imageDirectory = "ProfileImages";



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_patient_check);

        Intent intent = getIntent();
        userIdDoc = intent.getStringExtra(userIdUser);
        objectIdDoc = intent.getStringExtra(objectIdUser);
        deviceIdDoc = intent.getStringExtra(deviceIdUser);
        peerIdDoc = intent.getStringExtra(peerIdUser);

        Log.i("PatientCheck", "userId: " + userIdDoc);
        Log.i("PatientCheck", "objectIdDoc: " + objectIdDoc);
        Log.i("PatientCheck", "deviceIdDoc: " + deviceIdDoc);
        Log.i("PatientCheck", "peerIdDoc: " + peerIdDoc);


        imageProfile = (ImageView) findViewById(R.id.imageView);

        //Click en la foto
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PatientCheck", "Click en Photo");
                if (CameraUtils.checkPermissions(getApplicationContext())) {
                    captureImage();
                } else {
                    requestCameraPermission(MEDIA_TYPE_IMAGE);
                }
                Log.i("PatientCheck", "ELSE");
                onPhotoButtonClicked();
            }
        });


        peerId = GlobalVar.getInstance().getGlobalVar1();
        Log.i("PatientCheck", "Inicio");
        Log.i("PatientCheck", "peerId: " + peerId);


        initUI();
    }

    private void initUI() {

        nameField = (EditText) findViewById(R.id.nameField);
        nameField.setKeyListener(null);
        dniField = (EditText) findViewById(R.id.dniField);
        dniField.setKeyListener(null);
        checkButton = (Button) findViewById(R.id.checkButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelButtonClicked();
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheckButtonClicked();
            }
        });

        //Busca al Paciente con el peerId
        String whereClause = "peerId = '" + peerId + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );
        Log.i("PatientCheck", "peerId: " + peerId);

        Backendless.Data.of("User").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientMap )
                    {
                        // every loaded object from the "Contact" table is now an individual java.util.Map
                        Log.i("PatientCheck", "patientMap: " + patientMap);
                        if (patientMap.isEmpty()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                            builder.setTitle(R.string.error_database);
                            builder.setMessage(R.string.error_peerId);
                            builder.setCancelable(false);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    onCancelButtonClicked();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();

                        } else {
                            Map patientDB = patientMap.get(0);


                            Object checkdata = patientDB.get("deviceId");
                            Log.i("PatientCheck", "deviceId: " + checkdata);

                            if (checkdata == null) {

                                AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                                builder.setTitle(R.string.error_database);
                                builder.setMessage(R.string.error_paciente);
                                builder.setCancelable(false);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        onCancelButtonClicked();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            } else {

                                name = patientDB.get("name").toString();
                                dni = patientDB.get("dni").toString();
                                filepath = patientDB.get("profileImageUrl").toString();
                                ownerId = patientDB.get("ownerId").toString();
                                deviceId = patientDB.get("deviceId").toString();

                                //control
                                Log.i("PatientCheck", "name: " + name);
                                //carga Patients

                                patient.setDniPac(dni);
                                patient.setNamePac(name);
                                patient.setProfileImageUrlPac(filepath);
                                patient.setOwnerIdPac(ownerId);
                                patient.setPeerIdPac(peerId);
                                patient.setDeviceIdPac(deviceId);

                                //Patient Doc Info
                                patient.setOwnerIdDoc(userIdDoc);
                                patient.setDeviceIdDoc(deviceIdDoc);
                                patient.setPeerIdDoc(peerIdDoc);

                                nameField.setText(name);
                                dniField.setText(dni);
                                if (filepath != null) {
                                    new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                                            .execute(filepath);

                                }
                            }
                        }
                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("PatientCheck", "Error: " + fault);
                    }
                });

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private void onCheckButtonClicked() {
        Log.i("onCheckButtonClicked", "INIT");

        List<String> devices = new ArrayList<>();
        devices.add( patient.getDeviceIdDoc() );
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setPushSinglecast( devices );
        //Grabar en Backendless
        checkPatientsSaved();
        Log.i("onCheckButtonClicked", "devices: " + devices);
    }

    private void sendMesssage(List<String> devices) {
        Log.i("sendMesssage", "objectIdTratamiento: " + objectIdTratamiento);

        if (objectIdTratamiento == null) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PatientCheck.this);
            builder.setTitle(R.string.error_mensaje);
            android.app.AlertDialog dialog = builder.create();
            dialog.show();

        } else {

            PublishOptions publishOptions = new PublishOptions();
            publishOptions.putHeader("android-ticker-text", "Ha recibido una notificación!");
            publishOptions.putHeader("android-content-title", "Su doctor necesita sacar fotos");
            publishOptions.putHeader("android-content-text", "DOC@" + objectIdTratamiento);
            publishOptions.putHeader("ios-alert-title", "Su doctor necesita sacar fotos");
            publishOptions.putHeader("ios-alert-subtitle", " ");
            publishOptions.putHeader("ios-alert-body", "DOC@" + objectIdTratamiento);

            DeliveryOptions deliveryOptions = new DeliveryOptions();
            Date publishDate = new Date(System.currentTimeMillis() + 1000); // add 1 second
            Log.i(TAG, "sendMesssage: " + publishDate);
            // deliveryOptions.setPublishAt( publishDate );
            deliveryOptions.setPushSinglecast(devices);

            //Backendless.Messaging.pushWithTemplate("foto_template"

            Backendless.Messaging.publish("default", "Mensaje de Foto", publishOptions, deliveryOptions, new AsyncCallback<MessageStatus>() {
                @Override
                public void handleResponse(MessageStatus messageStatus) {
                    Log.i(TAG, "Message published - " + messageStatus.getMessageId());
                    System.out.println("Message status: " + messageStatus.getStatus() + "\n");
                    getMessageStatus(messageStatus.getMessageId());
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.e(TAG, fault.getMessage());
                }
            });
        }

    }

    private void getMessageStatus(String messageId) {
        Backendless.Messaging.getMessageStatus( messageId, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus messageStatus) {
                Log.i(TAG, "Message messageStatus - " + messageStatus);
                Log.i(TAG, "Message published - " + messageStatus.getMessageId());
                System.out.println("Message status: " + messageStatus.getStatus() + "\n");
                finish();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, fault.getMessage());
            }
        });

    }


    private void onCancelButtonClicked() {
        Log.i("PatientCheck", "onCancelButtonClicked");
        finish();

    }

        private void startLoginResult(String userId, boolean logoutButtonState)
    {
        Log.i("EditarActivity", "startLoginResult");
        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userId_key, userId);
        startActivity(intent);
    }

    private final TextWatcher editTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            nameField.setCursorVisible(true);
            dniField.setCursorVisible(true);
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        public void afterTextChanged(Editable s) {
            if (s.length() == 0) {
                //  Toast.makeText(LoginActivity.this, "Blank Field", Toast.LENGTH_LONG).show();
            } else {
                //   Toast.makeText(LoginActivity.this, "Entered text: " + identityField.getText(), Toast.LENGTH_LONG).show();
            }
        }
    };

    /**
     * Capturing Camera Image will launch camera app requested image capture
     */
    private void captureImage() {
        Log.i("EditarActivity", "captureImage");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
        }

        Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /**
     * Requesting permissions using Dexter library
     */
    private void requestCameraPermission(final int type) {
        Log.i("EditarActivity", "requestCameraPermission");
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                            if (type == MEDIA_TYPE_IMAGE) {
                                // capture picture
                                captureImage();
                            } else {
                                captureVideo();
                            }

                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            showPermissionsAlert();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    /**
     * Launching camera app to record video
     */
    private void captureVideo() {
        Log.i("EditarActivity", "captureVideo");
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_VIDEO);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
        }

        Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);

        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file

        // start the video capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
    }

    /**
     * Alert dialog to navigate to app settings
     * to enable necessary permissions
     */
    private void showPermissionsAlert() {
        Log.i("EditarActivity", "showPermissionsAlert");
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permissions required!")
                .setMessage("Camera needs few permissions to work properly. Grant them in settings.")
                .setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CameraUtils.openSettings(PatientCheck.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    /**
     * Saving stored image path to saved instance state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i("EditarActivity", "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putString(KEY_IMAGE_STORAGE_PATH, imageStoragePath);
    }

    /**
     * Restoring image path from saved instance state
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("EditarActivity", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }

    private void onPhotoButtonClicked() {
        Log.i("EditarActivity", "onPhotoButtonClicked");

        File imageFile = null;
        try {
            imageFile = createImageFile();
            imageFilePathCamera = imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (imageFile != null) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));

            if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        Log.i("EditarActivity", "createImageFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = timeStamp ;

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(fileName, ".jpg", storageDir);
        return image;
    }

    //Posterior a tener la foto
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("EditarActivity", "onRequestPermissionsResult INIT");

        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Camera disponible.", Toast.LENGTH_LONG).show();

                onPhotoButtonClicked();

            } else {

                Toast.makeText(this, "Camera no disponible.", Toast.LENGTH_LONG).show();

            }

        }
    }

    private void restoreFromBundle(Bundle savedInstanceState) {
        Log.i("EditarActivity", "restoreFromBundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_IMAGE_STORAGE_PATH)) {
                imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
                if (!TextUtils.isEmpty(imageStoragePath)) {
                    if (imageStoragePath.substring(imageStoragePath.lastIndexOf(".")).equals("." + IMAGE_EXTENSION)) {
                        previewCapturedImage();
                    }
                }
            }
        }
    }

    /**
     * Display image from gallery
     */
    private void previewCapturedImage() {
        Log.i("EditarActivity", "previewCapturedImage");
        try {
            // hide video preview
            imageProfile.setVisibility(View.VISIBLE);

            Bitmap bitmap = CameraUtils.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);

            imageProfile.setImageBitmap(bitmap);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void grabaFotoProfile (final String name) {
        Log.i("EditarActivity", "grabaFotoProfile");

        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFilePath = timeStamp + ".jpg";
        String files = SERVER_URL + "/" + APPLICATION_ID + "/" + API_KEY + "/" + "files";
        imageProfileLocation = files + "/" + imageDirectory + "/" + imageFilePath;

        Bitmap bitmap =  ((BitmapDrawable)imageProfile.getDrawable()).getBitmap();

        Backendless.Files.Android.upload(
                bitmap,
                Bitmap.CompressFormat.JPEG,
                100,
                imageFilePath,
                imageDirectory,
                new AsyncCallback<BackendlessFile>() {
                    @Override
                    public void handleResponse(BackendlessFile response) {
                        Log.i("RegisterActivity", "Foto grabada en Backendless!");
                        grabaNewUser(name);

                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.i("RegisterActivity", "Error: Foto grabada en Backendless!");

                    }
                }
        );
    }

    private void grabaNewUser (final String name) {
        Log.i("EditarActivity", "grabaNewUser INIT");


    }


    private void checkPatientsSaved() {
        String whereClause = "peerIdDoc = '" + patient.getPeerIdDoc() + "' && peerIdPac = '" + patient.getPeerIdPac() + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );
        Log.i("checkPatientsSaved", "INIT: " + whereClause);

        Backendless.Data.of("Patients").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientMap )
                    {
                        // every loaded object from the "Contact" table is now an individual java.util.Map
                        if (patientMap.isEmpty()) {
                            Log.i("PatientCheck", "patientMap NULL");
                            //Graba nuevo Tratamiento Paciente / Doctor
                            patientsSave();
                        } else {
                            Log.i("PatientCheck", "patientMap: " + patientMap);
                            Map patientDB = patientMap.get(0);
                            objectIdTratamiento = patientDB.get("objectId").toString();
                            String deviceIdPac = patientDB.get("deviceIdPac").toString();
                            List<String> devices = new ArrayList<>();
                            devices.add( deviceIdPac );
                            sendMesssage(devices);
                            updatePatients();
                        }
                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("checkPatientsSaved", "Error: " + fault);
                    }
                });
    }


    private void updatePatients() {
        Log.i("DoctorCheck", "updatePatients");

        AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
        builder.setMessage(R.string.mensageEnviado)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();


    }

    private void patientsSave() {
        Log.i("PatientCheck", "patientsSave");

        //Primera Vez no Update
        patient.setPhotosChecked(0);

        // save object asynchronously
        Backendless.Persistence.of(Patients.class ).save( patient, new AsyncCallback<Patients>() {
            public void handleResponse( Patients response )
            {

                Log.i( "patientsSave", "patientsSave recorded: " + response );
                relationUserPatient();

            }

            public void handleFault( BackendlessFault fault )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void relationUserPatient() {
        Log.i( "relationUserPatient", "patientsSave recorded: " + objectIdDoc + " / " + patient.getObjectId() );
        HashMap<String, Object> parentObject = new HashMap<String, Object>();
        parentObject.put( "objectId", objectIdDoc );

        HashMap<String, Object> childObject = new HashMap<String, Object>();
        childObject.put( "objectId", patient.getObjectId() );

        ArrayList<Map> children = new ArrayList<Map>();
        children.add( childObject );

        Backendless.Data.of("User" ).setRelation( parentObject  , "tratamientos", children,
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {
                        Log.i( "relationUserPatient", "relation has been set" );

                        relationPatientUser();
                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } );

    }

    private void relationPatientUser() {
        Log.i( "relationPatientUser", "patientsSave recorded: " + objectIdDoc + " / " + patient.getObjectId() );

        HashMap<String, Object> parentObject = new HashMap<String, Object>();
        parentObject.put( "objectId", patient.getObjectId()  );

        HashMap<String, Object> childObject = new HashMap<String, Object>();
        childObject.put( "objectId", objectIdDoc );

        ArrayList<Map> children = new ArrayList<Map>();
        children.add( childObject );

        Backendless.Data.of("Patients" ).setRelation( parentObject  , "paciente", children,
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {
                        Log.i( "relationPatientUser", "relation has been set" );

                        String deviceIdPac = patient.getDeviceIdPac();
                        Log.i( "relationPatientUser", "deviceIdPac: " + deviceIdPac );
                        List<String> devices = new ArrayList<>();
                        devices.add( deviceIdPac );
                        sendMesssage(devices);

                        AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                        builder.setMessage(R.string.mensageEnviado)
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();


                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(PatientCheck.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } );

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("EditarActivity", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //Saco la foto
                Log.i("EditarActivity", "addPhotoToGallery");
                //Toast.makeText(this, "Foto OK", Toast.LENGTH_SHORT).show();
                flagPhoto = true;
                addPhotoToGallery(imageFilePathCamera);


            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Toast.makeText(this, "Foto Cancelada", Toast.LENGTH_SHORT).show();
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            //Toast.makeText(this, "Galería Cancelada", Toast.LENGTH_SHORT).show();
        }
    }


    private void addPhotoToGallery (String filepath ) {
        Log.i("EditarActivity", "addPhotoToGallery");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(filepath);
        Uri uri = Uri.fromFile(f);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Bitmap bitmap3 = cropBitmap(bitmap);
            Bitmap bitmap2 = getResizedBitmap(bitmap3, 2000);
            displayPopuoImage(bitmap2);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Bitmap cropBitmap (Bitmap srcBmp) {
        Log.i("EditarActivity", "cropBitmap");
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        } else {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight()/2 - srcBmp.getWidth()/2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        return dstBmp;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int size) {
        Log.i("EditarActivity", "getResizedBitmap");
        int width = bm.getWidth();
        int height = bm.getHeight();
        int newHeight = size;
        int newWidth = size;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void displayPopuoImage(final Bitmap imageBitmap) {
        Log.i("EditarActivity", "displayPopuoImage");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Imagen seleccionada");
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Si es boton OK
                escalaYsigue (imageBitmap);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onGaleriaButtonClicked();

            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(imageBitmap);

        alertDialog.setView(imageView);
        alertDialog.create();
        alertDialog.show();

    }

    private void escalaYsigue (final Bitmap imageBitmap) {
        Log.i("EditarActivity", "escalaYsigue");
        Bitmap aCrop = getResizedBitmap(imageBitmap, 250 ); // 1,77 aspect ratio 250 x 442
        imageProfile.setImageBitmap(aCrop);


    }

    private void onGaleriaButtonClicked () {
        Log.i("EditarActivity", "onGaleriaButtonClicked");
        Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        choosePhotoIntent.setType("image/*");

        if (choosePhotoIntent.resolveActivity(this.getPackageManager()) != null) {
            startActivityForResult(choosePhotoIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
        }
    }

    ////////////////////////////////////////////
    //////////////////////////////////////////


    private void showToast( String msg )
    {
        Toast.makeText( this, msg, Toast.LENGTH_SHORT ).show();
    }



}

