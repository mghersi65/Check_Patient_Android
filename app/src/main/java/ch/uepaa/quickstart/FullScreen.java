package ch.uepaa.quickstart;

 import android.content.Intent;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.WindowManager;
 import android.widget.ImageView;
 import android.widget.Toolbar;

 import androidx.appcompat.app.AppCompatActivity;

 import java.io.FileInputStream;
 import java.io.InputStream;

public class FullScreen extends AppCompatActivity {

    private String fullScreenInd;
    static final String photoNameIntent = "photo1 o 2";
    private ImageView imageProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        imageProfile = (ImageView) findViewById(R.id.imageView);

        Log.i("FullScreen", "Click en Photo 1");

        Intent intent = getIntent();
        String photoName = intent.getStringExtra(photoNameIntent);


        if (photoName != null) {
            new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                    .execute(photoName);

        }


        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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

}