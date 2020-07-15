package ch.uepaa.quickstart;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class PassRecovActivity extends Activity
{
    private Button loginButton;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView( R.layout.password_recovery_requested );
        Log.i("PasswordRecovery", "Inicio");
        initUI();
    }

    private void initUI()
    {
        loginButton = (Button) findViewById( R.id.registerButton );

        loginButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                onLoginButtonClicked();
            }
        } );
    }

    public void onLoginButtonClicked()
    {
        startActivity( new Intent( this, MainActivity.class ) );
        finish();
    }
}
