package ch.uepaa.quickstart;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import static ch.uepaa.quickstart.Defaults.API_KEY;
import static ch.uepaa.quickstart.Defaults.APPLICATION_ID;
import static ch.uepaa.quickstart.Defaults.SERVER_URL;
import static ch.uepaa.quickstart.DoctorCheck.rotateImage;


public class RegisterActivity extends Activity  {
    private final static java.text.SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy/MM/dd");

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;

    // key to store image path in savedInstance state
    public static final String KEY_IMAGE_STORAGE_PATH = "image_path";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    // Bitmap sampling size
    public static final int BITMAP_SAMPLE_SIZE = 8;

    // Gallery directory name to store the images or videos
    public static final String GALLERY_DIRECTORY_NAME = "FacePassDirectory";

    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";

    private static String imageStoragePath;

    private EditText nameField;
    private EditText dniField;
    private EditText emailField;
    private EditText passwordField;
    private Button registerButton;
    private ImageView imageProfile;

    private String name;
    private String dni;
    private String email;
    private String password;
    private String imageFilePath;
    private String imageFilePathCamera;
    private String imageProfileLocation;
    private Boolean flagPhoto = false;
    final String imageDirectory = "ProfileImages";


    private BackendlessUser user;


    public void onCreate(Bundle savedInstanceState) {
        Log.i("RegisterActivity", "onCreate");
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_register);

        // Checking availability of the camera
        if (!CameraUtils.isDeviceSupportCamera(getApplicationContext())) {
            Toast.makeText(getApplicationContext(),
                    R.string.no_camera,
                    Toast.LENGTH_LONG).show();
            // will close the app if the device doesn't have camera
            finish();
        }

        nameField = (EditText) findViewById(R.id.nameField);
        dniField = (EditText) findViewById(R.id.dniField);
        emailField = (EditText) findViewById(R.id.emailField);
        passwordField = (EditText) findViewById(R.id.passwordField);
        registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterButtonClicked();
            }
        });


        imageProfile = (ImageView) findViewById(R.id.imageView);
        imageProfile.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (CameraUtils.checkPermissions(getApplicationContext())) {
                    captureImage();
                } else {
                    requestCameraPermission(MEDIA_TYPE_IMAGE);
                }
            }
        });

        // restoring storage image path from saved instance state
        // otherwise the path will be null on device rotation
        restoreFromBundle(savedInstanceState);
    }


    private void onGaleriaButtonClicked () {
        Log.i("RegisterActivity", "onGaleriaButtonClicked");
        Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        choosePhotoIntent.setType("image/*");

        if (choosePhotoIntent.resolveActivity(this.getPackageManager()) != null) {
            startActivityForResult(choosePhotoIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
        }
    }


    private void onRegisterButtonClicked() {
        Log.i("RegisterActivity", "Click en Registrate aquí");
        String nameText = nameField.getText().toString().trim();
        String dniText = dniField.getText().toString().trim();
        String emailText = emailField.getText().toString().trim();
        String passwordText = passwordField.getText().toString().trim();

        if (emailText.isEmpty()) {
            Toast.makeText(this, "El email es requerido.", Toast.LENGTH_SHORT).show();
            return;
        } else
            email = emailText;

        if (passwordText.isEmpty()) {
            Toast.makeText(this, "La clave es requerida.", Toast.LENGTH_SHORT).show();
            return;
        }
        else
            password = passwordText;

        if (nameText.isEmpty()) {
            Toast.makeText(this, "El nombre y apellido es requerido.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            name = nameText;
        }

        if (dniText.isEmpty()) {
            Toast.makeText(this, "El DNI es requerido.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            dni = dniText;
        }

        //Si hay foto la graba
        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
            flagPhoto = true;
            Log.i("RegisterActivity", "flagPhoto = true");
            Log.i("RegisterActivity", "Con foto");
            grabaFotoProfile(nameText);
        } else {
            flagPhoto = false;

            //Para cambiar ERROR de Foto Inicial
            //flagPhoto = true;

            Log.i("RegisterActivity", "Sin foto");
            Toast.makeText(RegisterActivity.this, "Error: Falta foto de perfil.", Toast.LENGTH_LONG).show();
            return;
        }

        final User userClass = new User();
        userClass.setName(name);
        userClass.setDni(dni);
        String files = "https://backendlessappcontent.com" + "/" + APPLICATION_ID + "/" + API_KEY + "/" + "files";
        imageProfileLocation = files + "/" + imageDirectory + "/" + imageFilePath;
        Log.i("imageProfileLocation", imageProfileLocation);
        userClass.setProfileImageUrl(imageProfileLocation);


        final BackendlessUser user = new BackendlessUser();

        if (email != null) {
            user.setEmail(email);
        }

        if (password != null) {
            user.setPassword(password);
        }


        Backendless.UserService.register(user, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(final BackendlessUser response) {
                final Resources resources = getResources();
                final String message = resources.getString(R.string.registration_success_message);

                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setMessage(message);
                builder.setTitle(R.string.registration_success);
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logUser();

                        // Graba los datos de memoria
                        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("identity", user.getEmail() );
                        editor.putString("password", user.getPassword() );
                        editor.apply();

                        userSave(response.getObjectId(), userClass, message);

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            }

            @Override
            public void handleFault(BackendlessFault fault) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // TODO: Move this to where you establish a user session



    }

    private void userSave(final String masterObjectId, final User userClass, String messageIN) {
        Log.i("RegisterActivity", "UserSave");

        userClass.setOwnerId( masterObjectId );
        final String message = messageIN;
        final String userId = masterObjectId;

        // save object asynchronously
        Backendless.Persistence.of(User.class ).save( userClass, new AsyncCallback<User>() {
            public void handleResponse( User response )
            {

                Log.i( "RegisterActivity", "UserSave recorded" );
                startLoginResult(message, userId, true );
                flagPhoto = false;

            }

            public void handleFault( BackendlessFault fault )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void startLoginResult(String msg, String userId, boolean logoutButtonState)
    {
        Log.i("RegisterActivity", "startLoginResult");
        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userInfo_key, msg);
        intent.putExtra(LoginResult.userId_key, userId);
        startActivity(intent);
    }

    private void logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Log.i("RegisterActivity", "logUser");

    }



    private void onPhotoButtonClicked() {
        Log.i("RegisterActivity", "onPhotoButtonClicked");
        File imageFile = null;
        try {
            imageFile = createImageFile();
            imageFilePathCamera = imageFile.getAbsolutePath();
            Log.i("imageFilePathCamera", imageFilePathCamera);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (imageFile != null) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            flagPhoto = true;
            Log.i("RegisterActivity", "flagPhoto = true");

            if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        Log.i("RegisterActivity", "createImageFile");
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(fileName, ".jpg", storageDir);
        return image;
    }

    private void addPhotoToGallery () {
        Log.i("RegisterActivity", "addPhotoToGallery");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageFilePathCamera);
        Uri uri = Uri.fromFile(f);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);
        Log.i("FaceTrip", "Termino de grabar");

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Bitmap bitmap3 = cropBitmap(bitmap);
            Bitmap bitmap2 = getResizedBitmap(bitmap3, 250);
            displayPopuoImage(bitmap2);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void displayPopuoImage(final Bitmap imageBitmap) {
        Log.i("RegisterActivity", "displayPopuoImage");
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

    public Bitmap getResizedBitmap(Bitmap bm, int size) {
        Log.i("RegisterActivity", "getResizedBitmap");

        int width = bm.getWidth();
        int height = bm.getHeight();
        Log.i("RegisterActivity", "width: " + width);
        Log.i("RegisterActivity", "height: " + height);
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

    public Bitmap cropBitmap (Bitmap srcBmp) {
        Log.i("RegisterActivity", "cropBitmap");
        Log.i("RegisterActivity", "width: " + srcBmp.getWidth());
        Log.i("RegisterActivity", "height: " + srcBmp.getHeight());
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth()/2 - srcBmp.getHeight()/2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );
            dstBmp = rotateImage(dstBmp, 270);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("RegisterActivity", "onActivityResult");
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            Log.i("RegisterActivity", "requestCode 1");
            if (resultCode == RESULT_OK) {
                // Refreshing the gallery
                CameraUtils.refreshGallery(getApplicationContext(), imageStoragePath);
                //Saco la foto
                //captureImage();
                Toast.makeText(this, "Foto OK", Toast.LENGTH_SHORT).show();
                // successfully captured the image
                // display it in image view
                previewCapturedImage();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Foto Cancelada", Toast.LENGTH_SHORT).show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
            Log.i("RegisterActivity", "requestCode 2");
            if (resultCode == Activity.RESULT_OK) {
                //Saco la foto
                Toast.makeText(this, "Eligió OK", Toast.LENGTH_SHORT).show();
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                    Bitmap bitmap3 = cropBitmap(bitmap);
                    Bitmap bitmap2 = getResizedBitmap(bitmap3, 250);
                    displayPopuoImage(bitmap2);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Galería Cancelada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showToast( String msg )
    {
        Toast.makeText( this, msg, Toast.LENGTH_SHORT ).show();
    }

    private void grabaFotoProfile (final String name) {
        Log.i("RegisterActivity", "grabaFotoProfile");

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
        Log.i("RegisterActivity", "grabaNewUser INIT");


    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("RegisterActivity", "onRequestPermissionsResult INIT");

        switch (requestCode) {
            case CAMERA_CAPTURE_IMAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera disponible.", Toast.LENGTH_LONG).show();
                    onPhotoButtonClicked();
                } else {
                    Toast.makeText(this, "Camera no disponible.", Toast.LENGTH_SHORT).show();
                }
        }

    }

    private void restoreFromBundle(Bundle savedInstanceState) {
        Log.i("RegisterActivity", "restoreFromBundle");
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
     * Requesting permissions using Dexter library
     */
    private void requestCameraPermission(final int type) {
        Log.i("RegisterActivity", "requestCameraPermission");
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                            if (type == MEDIA_TYPE_IMAGE) {
                                // capture picture
                                captureImage();
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
     * Capturing Camera Image will launch camera app requested image capture
     */
    private void captureImage() {
        Log.i("RegisterActivity", "captureImage");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
            flagPhoto = true;
            Log.i("RegisterActivity", "flagPhoto = true");
        }

        Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);

    }

    /**
     * Saving stored image path to saved instance state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i("RegisterActivity", "onSaveInstanceState");
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
        Log.i("RegisterActivity", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }


    /**
     * Activity result method will be called after closing the camera
     */
    /**
     * Display image from gallery
     */
    private void previewCapturedImage() {
        Log.i("RegisterActivity", "previewCapturedImage");
        try {
            // hide video preview
            imageProfile.setVisibility(View.VISIBLE);
            Log.i("RegisterActivity", imageStoragePath);
            Bitmap bitmap = CameraUtils.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);
            escalaYsigue(bitmap);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    /**
     * Alert dialog to navigate to app settings
     * to enable necessary permissions
     */
    private void showPermissionsAlert() {
        Log.i("RegisterActivity", "showPermissionsAlert");
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Permiso de Camara es requerido!")
                .setMessage("Se necesita utilizar la cámara para tomar su foto.")
                .setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CameraUtils.openSettings(RegisterActivity.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private void escalaYsigue (final Bitmap imageBitmap) {
        Log.i("RegisterActivity", "escalaYsigue");
        Bitmap imageBitmapCrop = cropBitmap(imageBitmap);
        Bitmap resizedBitmap = getResizedBitmap(imageBitmapCrop, 250 ); // 1,77 aspect ratio 250 x 442
        imageProfile.setImageBitmap(resizedBitmap);

    }




}

