package ch.uepaa.quickstart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.backendless.Backendless;

public class RestorePasswordActivity extends Activity {
    private Button restorePasswordButton;
    private EditText emailField;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_restore_password );
        Log.i("RestorePassword", "Inicio");

        initUI();
    }

    private void initUI()
    {
        restorePasswordButton = (Button) findViewById( R.id.restorePasswordButton );
        emailField = (EditText) findViewById( R.id.emailField );

        restorePasswordButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                onRestorePasswordButtonClicked();
            }
        } );
    }

    public void onRestorePasswordButtonClicked()
    {
        String email = emailField.getText().toString();
        Backendless.UserService.restorePassword( email, new DefaultCallback<Void>( this )
        {
            @Override
            public void handleResponse( Void response )
            {
                super.handleResponse( response );

                AlertDialog.Builder builder = new AlertDialog.Builder(RestorePasswordActivity.this);
                builder.setMessage(R.string.password_recovered_message)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            }
        } );
    }

}
