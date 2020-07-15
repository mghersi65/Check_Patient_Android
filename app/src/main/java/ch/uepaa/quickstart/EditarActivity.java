package ch.uepaa.quickstart;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

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

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraActivity;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;
import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ch.uepaa.quickstart.Defaults.API_KEY;
import static ch.uepaa.quickstart.Defaults.APPLICATION_ID;
import static ch.uepaa.quickstart.Defaults.MY_APP_ID;
import static ch.uepaa.quickstart.Defaults.SERVER_URL;

public class EditarActivity extends AppCompatActivity {

    public static final int RequestPermissionCode = 1;

    private final static java.text.SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy/MM/dd");

    private EditText nameField;
    private EditText dniField;
    private Button editarButton;
    private ImageView imageProfile;

    private String name;
    private String dni;
    private String filepath;
    private String ownerId;
    private Intent data;
    private String objectId;
    private String deviceId;

    static final String ownerIdIntent = "BackendlessObjectIdIntent";

    private User userClass = new User();

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
    final String imageDirectory = "ProfileImages";




    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


            Log.i("EditarActivity", "flagPhoto: " + flagPhoto);
            setContentView(R.layout.activity_edit_register);

            imageProfile = (ImageView) findViewById(R.id.imageView);
            imageProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i("EditarActivity", "Click en Photo");
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, 7);

                }
            });

            EnableRuntimePermission();
            Intent intent = getIntent();
            ownerId = intent.getStringExtra(ownerIdIntent);
            Log.i("EditarActivity", "Inicio");
            Log.i("EditarActivity", "ownerId: " + ownerId);

        nameField = (EditText) findViewById(R.id.nameField);
        dniField = (EditText) findViewById(R.id.dniField);
        editarButton = (Button) findViewById(R.id.editarButton);

        editarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEditarButtonClicked();
            }
        });



        initUI();


    }

    private void initUI() {
            String whereClause = "ownerId = '" + ownerId + "'";
            DataQueryBuilder queryBuilder = DataQueryBuilder.create();
            queryBuilder.setWhereClause(whereClause);
            queryBuilder.setSortBy("created DESC");

            Backendless.Data.of("User").find(queryBuilder,
                    new AsyncCallback<List<Map>>() {
                        @Override
                        public void handleResponse(List<Map> facemap) {
                            // every loaded object from the "Contact" table is now an individual java.util.Map
                            Log.i("EditarActivity", "facemap: " + facemap);
                            Map face = facemap.get(0);
                            name = face.get("name").toString();
                            dni = face.get("dni").toString();
                            filepath = face.get("profileImageUrl").toString();
                            objectId = face.get("objectId").toString();
                            //control
                            Log.i("EditarActivity", "name: " + name);
                            //carga Patients

                            userClass.setProfileImageUrl(filepath);
                            userClass.setObjectId(objectId);

                            nameField.setText(name);
                            dniField.setText(dni);

                            if (flagPhoto) {
                                Log.i("EditarActivity", "flagPhoto updateContact: " + flagPhoto);
                                grabaFotoProfile();


                            } else if (filepath != null && !flagPhoto) {
                                Log.i("EditarActivity", "flagPhoto: " + flagPhoto);
                                new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                                        .execute(filepath);

                            }

                        }

                        @Override
                        public void handleFault(BackendlessFault fault) {
                            // an error has occurred, the error code can be retrieved with fault.getCode()
                            Log.i("EditarActivity", "Error: " + fault);
                        }
                    });


    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            Log.i("onEditarButtonClicked", "DownloadImageTask");
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

    private void onEditarButtonClicked() {
        Log.i("onEditarButtonClicked", "INIT");
        String nameText = nameField.getText().toString().trim();
        String dniText = dniField.getText().toString().trim();

        Log.i("onEditarButtonClicked", "nameText: " + nameText);
        Log.i("onEditarButtonClicked", "dniText: " + dniText);

        if (!nameText.isEmpty()) {
            name = nameText;
        }

        if (!dniText.isEmpty()) {
            dni = dniText;
        }

        updateContact(); //Update de contacto
        startLoginResult();


    }

    private void startLoginResult()
    {
        Log.i("EditarActivity", "startLoginResult");
        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userId_key, ownerId);
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
                        CameraUtils.openSettings(EditarActivity.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }



    private void grabaFotoProfile () {
        Log.i("EditarActivity", "grabaFotoProfile");

        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFilePath = timeStamp + ".jpg";
        String files = SERVER_URL + "/" + APPLICATION_ID + "/" + API_KEY + "/" + "files";
        filepath = files + "/" + imageDirectory + "/" + imageFilePath;
        userClass.setProfileImageUrl(filepath);
        updateContact();

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


                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.i("RegisterActivity", "Error: Foto grabada en Backendless!");

                    }
                }
        );
    }


    public void updateContact()
    {
        Log.i( "DoctorCheck", "INIT" );

        HashMap updateUser = new HashMap();
        updateUser.put("dni", dniField.getText().toString().trim());
        updateUser.put("name", nameField.getText().toString().trim());
        updateUser.put("profileImageUrl", userClass.getProfileImageUrl());
        updateUser.put("objectId", userClass.getObjectId());
        Log.i( "DoctorCheck", "objectId recorded: " + userClass.getObjectId() );



        Backendless.Data.of("User").save( updateUser, new AsyncCallback<Map>() {
            public void handleResponse( Map response )
            {
                Log.i( "DoctorCheck", "updateUser recorded: " + response );

                flagPhoto = false;


            }
            public void handleFault( BackendlessFault fault )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditarActivity.this);
                builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


    }

    ////////////////////////////////////////////
    //////////////////////////////////////////


    private void showToast( String msg )
    {
        Toast.makeText( this, msg, Toast.LENGTH_SHORT ).show();
    }

    ///////////////////////////////////////////
    /////Camara Automatica /////
    //////////////////////////////////////////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7 && resultCode == RESULT_OK) {
            Log.i("EditarActivity", "onActivityResult: ");
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            escalaYsigue(bitmap);
            flagPhoto = true;
            initUI();
        }
    }
    public void EnableRuntimePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(EditarActivity.this,
                Manifest.permission.CAMERA)) {
          //  Toast.makeText(EditarActivity.this,"CAMERA permission allows us to Access CAMERA app",     Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(EditarActivity.this,new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] result) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (result.length > 0 && result[0] == PackageManager.PERMISSION_GRANTED) {
            //        Toast.makeText(EditarActivity.this, "Permission Granted, Now your application can access CAMERA.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(EditarActivity.this, "Permission Canceled, Now your application cannot access CAMERA.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public Bitmap cropBitmap (Bitmap srcBmp) {
        Log.i("EditarActivity", "cropBitmap");
        Log.i("EditarActivity", "getWidth: " + srcBmp.getWidth());
        Log.i("EditarActivity", "getHeight: " + srcBmp.getHeight());
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
        Log.i("EditarActivity", "width: " + width);
        Log.i("EditarActivity", "height: " + height);
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

    private void escalaYsigue (final Bitmap imageBitmap) {
        Log.i("EditarActivity", "escalaYsigue");
        Bitmap imageBitmapCrop = cropBitmap(imageBitmap);
        Bitmap resizedBitmap = getResizedBitmap(imageBitmapCrop, 250 ); // 1,77 aspect ratio 250 x 442
        imageProfile.setImageBitmap(resizedBitmap);

    }

}

