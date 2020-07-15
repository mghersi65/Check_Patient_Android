package ch.uepaa.quickstart;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static ch.uepaa.quickstart.Defaults.API_KEY;
import static ch.uepaa.quickstart.Defaults.APPLICATION_ID;
import static ch.uepaa.quickstart.Defaults.SERVER_URL;

public class PatientPhotos extends Activity {

    private final static java.text.SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy/MM/dd");

    static final String objectIdPacIntent = "BackendlessObjectId"; //Object Patient enviado en el Mensaje

    private String userIdPac;

    private TextView nameField;
    private TextView dniField;
    private Button cancelButton;
    private ImageView imageProfile;
    private ImageView imagePacOne;
    private ImageView imagePacTwo;

    private String name;
    private String dni;
    private String objectId;
    private String filepath;
    private String filepathOne;
    private String filepathTwo;
    private Integer photosChecked;

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
        setContentView(R.layout.activity_patient_photos);

        Intent intent = getIntent();
        objectId = intent.getStringExtra(objectIdPacIntent);

        Log.i("PatientPhotos", "userId: " + objectId);

        imageProfile = (ImageView) findViewById(R.id.imageView);
        imagePacOne = (ImageView) findViewById(R.id.imageViewOne);
        imagePacTwo = (ImageView) findViewById(R.id.imageViewTwo);

        initUI();

    }

    private void initUI() {

        nameField = (TextView) findViewById(R.id.nameField);
        nameField.setKeyListener(null);
        dniField = (TextView) findViewById(R.id.dniField);
        dniField.setKeyListener(null);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelButtonClicked();
            }
        });


        //Busca al Paciente con el peerId
        String whereClause = "objectId = '" + objectId + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );

        Backendless.Data.of("Patients").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientMap )
                    {
                        // every loaded object from the "Contact" table is now an individual java.util.Map
                        Log.i("PatientPhotos", "patientMap: " + patientMap);
                        Map patientDB = patientMap.get(0);
                        name = patientDB.get("namePac").toString();
                        dni = patientDB.get("dniPac").toString();
                        filepath = patientDB.get("profileImageUrlPac").toString();
                        filepathOne = patientDB.get("frontImageUrl").toString();
                        filepathTwo = patientDB.get("rearImageUrl").toString();
                        photosChecked = patientDB.get("photosChecked").hashCode();

                        //control
                        Log.i("PatientPhotos", "name: " + name);
                        //carga Patients

                        patient.setDniPac(dni);
                        patient.setNamePac(name);
                        patient.setProfileImageUrlPac(filepath);
                        patient.setFrontImageUrl(filepathOne);
                        patient.setRearImageUrl(filepathTwo);
                        patient.setObjectId(objectId);
                        patient.setPhotosChecked(photosChecked);

                        nameField.setText(name);
                        dniField.setText(dni);
                        if (filepath != null) {
                            new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                                    .execute(filepath);

                        }
                        if (filepathOne != null) {
                            new DownloadImageTask((ImageView) findViewById(R.id.imageViewOne))
                                    .execute(filepathOne);

                        }
                        if (filepathTwo != null) {
                            new DownloadImageTask((ImageView) findViewById(R.id.imageViewTwo))
                                    .execute(filepathTwo);

                        }

                        readyForClick();

                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("PatientPhotos", "Error: " + fault);
                    }
                });

    }

    private void readyForClick() {
        Log.i("PatientPhotos", "readyForClick");
        //Click en la foto
        imagePacOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PatientPhotos", "Click en Photo 1");
                fullScreen(filepathOne);

            }
        });

        imagePacTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("PatientPhotos", "Click en Photo 2");
                fullScreen(filepathTwo);
            }
        });


    }

    private void fullScreen(String photoName) {
        Intent myIntent = new Intent(this, FullScreen.class);
        myIntent.putExtra(FullScreen.photoNameIntent, photoName);
        startActivity(myIntent);
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


    private void onCancelButtonClicked() {
        Log.i("PatientPhotos", "onCancelButtonClicked");
        patientsSave();

    }

    private void patientsSave() {
        Log.i("PatientPhotos", "patientsSave");

        patient.setPhotosChecked(patient.getPhotosChecked() + 1);

        // save object asynchronously
        Backendless.Persistence.of(Patients.class ).save( patient, new AsyncCallback<Patients>() {
            public void handleResponse( Patients response )
            {
                Log.i( "PatientPhotos", "patientsSave recorded: " + response );
                finish();
            }

            public void handleFault( BackendlessFault fault )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(PatientPhotos.this);
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

}

