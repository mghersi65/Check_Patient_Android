package ch.uepaa.quickstart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessFault;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;



public class MainActivity extends Activity {
    private boolean isLoggedInBackendless = false;

    // backendless
    private EditText identityField, passwordField;
    private Button bkndlsLoginButton, bkndlsRegisterButton, bkndlsRecuperarButton;
    private String identity = "";
    private String password = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_activity);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Backendless.setUrl(Defaults.SERVER_URL);
        Backendless.initApp(getApplicationContext(), Defaults.APPLICATION_ID, Defaults.API_KEY);


        initUI();
        initUIBehaviour();
        initUITres();


    }


    private void initUI() {
        Log.i("MainActivity", "initUI");
        SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
        identity = settings.getString("identity", "");
        password = settings.getString("password", "");

        if ((identity.isEmpty()) && (password.isEmpty())) {
            Log.i("MainActivity", "identity: " + identity);
            Log.i("MainActivity", "password: " + password);
            boolean rememberLogin = true;
            loginBackendless(identity, password, rememberLogin);
        } else {
            Log.i("MainActivity", "goAutomaticRegister");
            startActivity(new Intent(this, RegisterActivity.class));
        }

        // backendless

        identityField = (EditText) findViewById(R.id.identityField);
        passwordField = (EditText) findViewById(R.id.passwordField);
        bkndlsLoginButton = (Button) findViewById(R.id.bkndlsLoginButton);
        bkndlsRegisterButton = (Button) findViewById(R.id.bkndlsRegisterButton);
        bkndlsRecuperarButton = (Button) findViewById(R.id.bkndlsRecuperarButton);

    }

    private void initUIBehaviour() {
        Log.i("MainActivity", "initUIBehaviour");

        // backendless
        bkndlsLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginWithBackendlessButtonClicked();
            }
        });
        bkndlsRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterButtonClicked();
            }
        });
        bkndlsRecuperarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRestoreLinkClicked();
            }
        });
    }

    private void initUITres() {
        Log.i("MainActivity", "initUITres");
        Backendless.UserService.isValidLogin(new DefaultCallback<Boolean>(this) {
            @Override
            public void handleResponse(Boolean isValidLogin) {
                super.handleResponse(null);
                if (isValidLogin && Backendless.UserService.CurrentUser() == null) {
                    String currentUserId = Backendless.UserService.loggedInUser();

                    if (!currentUserId.equals("")) {
                        Backendless.UserService.findById(currentUserId, new DefaultCallback<BackendlessUser>(MainActivity.this, "Logging in...") {
                            @Override
                            public void handleResponse(BackendlessUser currentUser) {
                                super.handleResponse(currentUser);
                                isLoggedInBackendless = true;
                                Backendless.UserService.setCurrentUser(currentUser);
                                startLoginResult(currentUser);
                            }

                            @Override
                            public void handleFault(BackendlessFault fault) {
                                Log.i("MainActivity", "handleFault 1");
                                //super.handleFault(fault);
                            }
                        });
                    }
                }
                super.handleResponse(isValidLogin);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.i("MainActivity", "handleFault 2");
                super.handleFault(fault);
            }
        });

    }

    private void startLoginResult(BackendlessUser user) {
        Log.i("MainActivity", "startLoginResult");
        String userId = user.getUserId();

        String msg = "ObjectId: " + user.getObjectId() + "\n"
                + "UserId: " + user.getUserId() + "\n"
                + "Email: " + user.getEmail() + "\n"
                + "Properties: " + "\n";

        for (Map.Entry<String, Object> entry : user.getProperties().entrySet())
            msg += entry.getKey() + " : " + entry.getValue() + "\n";


        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userInfo_key, msg);
        intent.putExtra(LoginResult.userId_key, userId);
        intent.putExtra(LoginResult.logoutButtonState_key, true);
        startActivity(intent);
    }

    private void startLoginResultDetail(String msg, String userId, boolean logoutButtonState) {
        Log.i("MainActivity", "startLoginResultDetail");
        Intent intent = new Intent(this, LoginResult.class);
        intent.putExtra(LoginResult.userInfo_key, msg);
        intent.putExtra(LoginResult.userId_key, userId);
        startActivity(intent);
    }


    private void onLoginWithBackendlessButtonClicked() {
        Log.i("MainActivity", "onLoginWithBackendlessButtonClicked");
        identity = identityField.getText().toString();
        password = passwordField.getText().toString();
        boolean rememberLogin = true;

        loginBackendless(identity, password, rememberLogin);

    }

    private void loginBackendless(final String identity, final String password, Boolean rememberLogin) {

        Backendless.UserService.login(identity, password, new DefaultCallback<BackendlessUser>(MainActivity.this) {
            public void handleResponse(BackendlessUser backendlessUser) {
                super.handleResponse(backendlessUser);
                isLoggedInBackendless = true;

                // Graba los datos de memoria
                SharedPreferences settings = getSharedPreferences("FacePassPrefsFile", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("identity", identity);
                editor.putString("password", password);
                editor.apply();
                startLoginResult(backendlessUser);
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                super.handleFault(fault);

            }
        }, rememberLogin);
    }

    private void onRegisterButtonClicked() {
        Log.i("MainActivity", "onRegisterButtonClicked");
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void onRestoreLinkClicked() {
        Log.i("MainActivity", "onRestoreLinkClicked");
        startActivity(new Intent(this, RestorePasswordActivity.class));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("MainActivity", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);


    }


}

