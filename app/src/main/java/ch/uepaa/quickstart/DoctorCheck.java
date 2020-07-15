package ch.uepaa.quickstart;


import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraActivity;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;
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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.content.ContentValues.TAG;
import static ch.uepaa.quickstart.Defaults.API_KEY;
import static ch.uepaa.quickstart.Defaults.APPLICATION_ID;
import static ch.uepaa.quickstart.Defaults.SERVER_URL;

import static ch.uepaa.quickstart.PatientCheck.userIdUser;

public class DoctorCheck extends HiddenCameraActivity {

    private CameraConfig mCameraConfig;

    private final static java.text.SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy/MM/dd");

    public static String objectIdDocIntent; //Intent
    private String userIdDocID;

    private TextView nameField;
    private Button checkButton;
    private Button cancelButton;
    private ImageView imageProfile;
    private ImageView imagePacOne;
    private ImageView imagePacTwo;

    private String nameDoc;
    private String namePac;
    private String dniDoc;
    private String dniPac;
    private String filepathDoc;
    private String filepathPac;
    private String ownerIdDoc;
    private String ownerIdPac;
    private Intent data;
    private String peerIdDoc;
    private String peerIdPac;
    private String deviceIdDoc;
    private String deviceIdPac;
    private String objectIdPac;
    private String objectIdDoc;
    private String objectIdTratamiento;

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
    private Boolean flag2Photo = true;
    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";
    // Bitmap sampling size
    public static final int BITMAP_SAMPLE_SIZE = 8;
    private String imageFilePath;
    private String imageFilePathCamera;
    private String imageProfileLocation;
    CountDownTimer countDownTimer;




    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_doctor_check);

        Intent intent = getIntent();
        objectIdTratamiento = intent.getStringExtra(objectIdDocIntent);

        Log.i("DoctorCheck", "objectIdTratamiento: " + objectIdTratamiento);

        imageProfile = (ImageView) findViewById(R.id.imageView);
        imagePacOne = (ImageView) findViewById(R.id.imageViewOne);
        imagePacTwo = (ImageView) findViewById(R.id.imageViewTwo);


        initUI();
        startPhotoFront();
        countdown();

    }

    private void countdown() {
        countDownTimer = new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                checkButton.setText("seconds remaining: " + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                checkButton.setText("listo!");
                checkButton.setEnabled(false);
                takePicture();
            }

        }.start();
    }


    private void initUI() {

        nameField = (TextView) findViewById(R.id.nameField);
        nameField.setKeyListener(null);
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

        String whereClause = "objectId = '" + objectIdTratamiento + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );
        Log.i("DoctorCheck", "objectId: " + objectIdTratamiento);

        Backendless.Data.of("Patients").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientMap )
                    {
                        // every loaded object from the "Contact" table is now an individual java.util.Map
                        Log.i("DoctorCheck", "patientMap: " + patientMap);
                        if (patientMap.isEmpty()) {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(DoctorCheck.this);
                            builder.setTitle(R.string.error_doctor);
                            android.app.AlertDialog dialog = builder.create();
                            dialog.show();

                        } else {
                            Map patientDB = patientMap.get(0);
                            ownerIdDoc = patientDB.get("ownerIdDoc").toString();
                            peerIdDoc = patientDB.get("peerIdDoc").toString();
                            deviceIdDoc = patientDB.get("deviceIdDoc").toString();

                            //control
                            Log.i("PatientCheck", "ownerIdDoc: " + ownerIdDoc);
                            //carga Patients

                            patient.setOwnerIdDoc(ownerIdDoc);
                            patient.setPeerIdDoc(peerIdDoc);

                            getDoctor(ownerIdDoc);
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

    private void getDoctor( String ownerIdDoc) {
        String whereClause = "ownerId = '" + ownerIdDoc + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );

        Backendless.Data.of("User").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientMap )
                    {
                        // every loaded object from the "Contact" table is now an individual java.util.Map
                        Log.i("PatientCheck", "patientMap: " + patientMap);
                        Map patientDB = patientMap.get(0);
                        nameDoc = patientDB.get("name").toString();
                        dniDoc = patientDB.get("dni").toString();
                        filepathDoc = patientDB.get("profileImageUrl").toString();
                        deviceIdDoc = patientDB.get("deviceId").toString();
                        objectIdDoc = patientDB.get("objectId").toString();

                        //control
                        Log.i("PatientCheck", "name: " + nameDoc);
                        //carga Patients

                        patient.setNameDoc(nameDoc);
                        patient.setDniDoc(dniDoc);
                        patient.setProfileImageUrlDoc(filepathDoc);
                        patient.setDeviceIdDoc(deviceIdDoc);

                        nameField.setText(nameDoc);

                        if (filepathDoc != null) {
                            new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                                    .execute(filepathDoc);
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        takePicture();


        /*
        List<String> devices = new ArrayList<>();
        devices.add( patient.getDeviceId() );
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setPushSinglecast( devices );
        sendMesssage(devices);
        Log.i("onCheckButtonClicked", "devices: " + devices);

        */
    }

    private void sendMesssage(List<String> devices) {

        PublishOptions publishOptions = new PublishOptions();
        publishOptions.putHeader( "android-ticker-text", "Ha recibido una notificación!" );
        publishOptions.putHeader( "android-content-title", "Recibiendo fotos de un paciente" );
        publishOptions.putHeader( "android-content-text", "PAC@" + objectIdTratamiento);
        publishOptions.putHeader( "ios-alert-title", "Recibiendo fotos de un paciente" );
        publishOptions.putHeader( "ios-alert-subtitle", "PAC@" + objectIdTratamiento );
        publishOptions.putHeader( "ios-alert-body", "Click aquí para continuar" );

        DeliveryOptions deliveryOptions = new DeliveryOptions();
        Date publishDate = new Date( System.currentTimeMillis() + 1000 ); // add 1 second
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


    private void getMessageStatus(String messageId) {
        Backendless.Messaging.getMessageStatus( messageId, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus messageStatus) {
                Log.i(TAG, "Message messageStatus - " + messageStatus);
                Log.i(TAG, "Message published - " + messageStatus.getMessageId());
                System.out.println("Message status: " + messageStatus.getStatus() + "\n");

                //Termina el proceso avisando al usuario
                mensajeEnviado();

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
        Log.i("DoctorCheck", "startLoginResult");
        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userId_key, userId);
        startActivity(intent);
    }


    /**
     * Capturing Camera Image will launch camera app requested image capture
     */
    private void captureImage() {
        Log.i("DoctorCheck", "captureImage");
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
     * Alert dialog to navigate to app settings
     * to enable necessary permissions
     */
    private void showPermissionsAlert() {
        Log.i("DoctorCheck", "showPermissionsAlert");
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permissions required!")
                .setMessage("Camera needs few permissions to work properly. Grant them in settings.")
                .setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CameraUtils.openSettings(DoctorCheck.this);
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
        Log.i("DoctorCheck", "onSaveInstanceState");
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
        Log.i("DoctorCheck", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }

    private void onPhotoButtonClicked() {
        Log.i("DoctorCheck", "onPhotoButtonClicked");

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
        Log.i("DoctorCheck", "createImageFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = timeStamp ;

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(fileName, ".jpg", storageDir);
        return image;
    }

    //Posterior a tener la foto

    private void restoreFromBundle(Bundle savedInstanceState) {
        Log.i("DoctorCheck", "restoreFromBundle");
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
        Log.i("DoctorCheck", "previewCapturedImage");
        try {
            // hide video preview
            imageProfile.setVisibility(View.VISIBLE);

            Bitmap bitmap = CameraUtils.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);

            imageProfile.setImageBitmap(bitmap);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private String grabaFotoProfile (Bitmap bitmap) {
        Log.i("DoctorCheck", "grabaFotoProfile");
        final String imageDirectory = "pacients";
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFilePath = timeStamp + ".jpg";
        String files = SERVER_URL + "/" + APPLICATION_ID + "/" + API_KEY + "/" + "files";
        String imageProfileLocation = files + "/" + imageDirectory + "/" + imageFilePath;

        Backendless.Files.Android.upload(
                bitmap,
                Bitmap.CompressFormat.JPEG,
                100,
                imageFilePath,
                imageDirectory,
                new AsyncCallback<BackendlessFile>() {
                    @Override
                    public void handleResponse(BackendlessFile response) {
                        Log.i("RegisterActivity", "Foto grabada en Backendless!: " + response);

                        if (flag2Photo) {
                            flag2Photo = false;
                            startPhotoBack();
                            takePicture();
                        } else {
                            updatePatients();
                        }
                        // Avisa al Doc
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.i("RegisterActivity", "Error: Foto grabada en Backendless!: " + fault);

                    }
                }
        );
        return imageProfileLocation;
    }


    private void updatePatients() {
        Log.i("DoctorCheck", "updatePatients");
        Log.i("DoctorCheck", "setFrontImageUrl: " + patient.getFrontImageUrl());
        Log.i("DoctorCheck", "setRearImageUrl: " + patient.getRearImageUrl());
        Log.i("DoctorCheck", "checkObjectId: " + objectIdTratamiento);
        HashMap updatePatients = new HashMap();
        updatePatients.put("objectId", objectIdTratamiento);
        updatePatients.put("frontImageUrl", patient.getFrontImageUrl());
        updatePatients.put("rearImageUrl", patient.getRearImageUrl());
        updatePatients.put("deviceIdDoc", patient.getDeviceIdDoc());
        updatePatients.put("dniDoc", patient.getDniDoc());
        updatePatients.put("nameDoc", patient.getNameDoc());
        updatePatients.put("profileImageUrlDoc", patient.getProfileImageUrlDoc());

        // save object asynchronously
        Backendless.Data.of("Patients").save( updatePatients, new AsyncCallback<Map>() {
            public void handleResponse( Map patientMap )
            {
                Log.i( "DoctorCheck", "updatePatients recorded: " + patientMap );
                Log.i("PatientCheck", "patientMap: " + patientMap);
                Integer photosChecked = patientMap.get("photosChecked").hashCode();
                Log.i("PatientCheck", "photosChecked: " + photosChecked);
                if (photosChecked > 0) {
                    // Está con relaciones
                } else {
                    // Si doctor está vacío graba las relaciones
                    relationPatientsDoc();
                }

            }
            public void handleFault( BackendlessFault fault )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(DoctorCheck.this);
                builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void relationPatientsDoc() {
        Log.i( "DoctorCheck", "relationUserDoc recorded: " + objectIdDoc + " / " + objectIdTratamiento );
        HashMap<String, Object> parentObject = new HashMap<String, Object>();
        parentObject.put( "objectId", objectIdDoc );

        HashMap<String, Object> childObject = new HashMap<String, Object>();
        childObject.put( "objectId", objectIdTratamiento );

        ArrayList<Map> children = new ArrayList<Map>();
        children.add( childObject );

        Backendless.Data.of("User" ).setRelation( parentObject  , "tratamientos", children,
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {
                        Log.i( "DoctorCheck", "relation has been set" );

                        relationDocUser();
                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(DoctorCheck.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } );

    }

    private void relationDocUser() {
        Log.i( "DoctorCheck", "relationDocUser recorded: " + objectIdTratamiento + " / " + objectIdDoc );

        HashMap<String, Object> parentObject = new HashMap<String, Object>();
        parentObject.put( "objectId", objectIdTratamiento );

        HashMap<String, Object> childObject = new HashMap<String, Object>();
        childObject.put( "objectId", objectIdDoc);

        ArrayList<Map> children = new ArrayList<Map>();
        children.add( childObject );

        Backendless.Data.of("Patients" ).setRelation( parentObject  , "doctor", children,
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {
                        Log.i( "DoctorCheck", "relation has been set" );

                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(DoctorCheck.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } );

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("DoctorCheck", "onActivityResult");
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
        Log.i("DoctorCheck", "addPhotoToGallery");
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
        Log.i("DoctorCheck", "cropBitmap");
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
        Log.i("DoctorCheck", "getResizedBitmap");
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
        Log.i("DoctorCheck", "displayPopuoImage");
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
        Log.i("DoctorCheck", "escalaYsigue");
        Bitmap aCrop = getResizedBitmap(imageBitmap, 250 ); // 1,77 aspect ratio 250 x 442
        imageProfile.setImageBitmap(aCrop);


    }

    private void onGaleriaButtonClicked () {
        Log.i("DoctorCheck", "onGaleriaButtonClicked");
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

/*
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "DoctorCheck - Received message");
        String message = intent.getExtras().getString("price");
        Log.d("OnMSG",message);

        AlertDialog.Builder builder = new AlertDialog.Builder(DoctorCheck.this);
        builder.setMessage(R.string.mensagePaciente)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

 */

// Tema Camara Automatica
    private void startPhotoFront() {

        mCameraConfig = new CameraConfig()
                .getBuilder(this)
                .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                .build();


        //Check for the camera permission for the runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            //Start camera preview
            startCamera(mCameraConfig);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

    }

    private void startPhotoBack() {

        mCameraConfig = new CameraConfig()
                .getBuilder(this)
                .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                .build();


        //Check for the camera permission for the runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            //Start camera preview
            startCamera(mCameraConfig);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //noinspection MissingPermission
                startCamera(mCameraConfig);
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        Uri imageUri = Uri.fromFile(imageFile);


        bitmap = rotateImageIfReq(this, bitmap);


        //Display the image to the image view
        if (flag2Photo) {
            ((ImageView) findViewById(R.id.imageViewOne)).setImageBitmap(bitmap);
            Bitmap newSize = getResizedPhoto(bitmap);
            String urlLocation = grabaFotoProfile(newSize);
            patient.setFrontImageUrl(urlLocation);
        } else {
            bitmap = rotateImage(bitmap, 180);
            ((ImageView) findViewById(R.id.imageViewTwo)).setImageBitmap(bitmap);
            Bitmap newSize = getResizedPhoto(bitmap);
            String urlLocation = grabaFotoProfile(newSize);
            patient.setRearImageUrl(urlLocation);

            //Sube la segunda foto y manda el mensaje
            enviarMensaheDoc();

        }
    }

    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Toast.makeText(this, "Cannot open camera.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camra permission before initializing it.
                Toast.makeText(this, "Camera permission not available.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                //This error will never happen while hidden camera is used from activity or fragment
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(this, "Your device does not have front camera.", Toast.LENGTH_LONG).show();
                break;
        }
    }


    public static Bitmap rotateImage(Bitmap source, float angle) {
        Log.i("DoctorCheck", "rotateImage: " + angle);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private static Bitmap rotateImageIfReq(Context context, Bitmap bitmap) {
        Log.i("DoctorCheck", "rotateImageIfReq");

        if (bitmap.getWidth() > bitmap.getHeight()) {
            //meaning the image is landscape view
            Log.i("DoctorCheck", "landscape");
            return rotateImage(bitmap, 270);
        } else {
            Log.i("DoctorCheck", "portrait");
            return bitmap;
        }
    }

    public Bitmap getResizedPhoto(Bitmap bitm) {
        Log.i("DoctorCheck", "getResizedBitmap");
        int width = bitm.getWidth();
        int height = bitm.getHeight();
        int newHeight = 320; // alto
        int newWidth = 240; // ancho
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bitm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private void enviarMensaheDoc() {

        List<String> devices = new ArrayList<>();
        devices.add(deviceIdDoc);
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setPushSinglecast(devices);
        sendMesssage(devices);
        Log.i("enviarMensaheDoc", "devices: " + devices);

    }

    private void mensajeEnviado() {

        Toast.makeText( this, R.string.mensagePaciente,
                Toast.LENGTH_LONG).show();


        countDownTimer = new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                finish();
            }

        }.start();

        // Termina el proceso

    }

}

