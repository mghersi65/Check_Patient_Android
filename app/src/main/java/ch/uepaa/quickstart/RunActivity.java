package ch.uepaa.quickstart;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.PublishMessageInfo;
import com.backendless.persistence.DataQueryBuilder;
import com.backendless.rt.messaging.Channel;
import com.backendless.rt.messaging.MessageInfoCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ch.uepaa.p2pkit.AlreadyEnabledException;
import ch.uepaa.p2pkit.P2PKit;
import ch.uepaa.p2pkit.P2PKitStatusListener;
import ch.uepaa.p2pkit.StatusResult;
import ch.uepaa.p2pkit.discovery.DiscoveryInfoTooLongException;
import ch.uepaa.p2pkit.discovery.DiscoveryInfoUpdatedTooOftenException;
import ch.uepaa.p2pkit.discovery.DiscoveryListener;
import ch.uepaa.p2pkit.discovery.DiscoveryPowerMode;
import ch.uepaa.p2pkit.discovery.Peer;
import ch.uepaa.p2pkit.discovery.ProximityStrength;
import ch.uepaa.quickstart.fragments.ColorPickerFragment;
import ch.uepaa.quickstart.fragments.ConsoleFragment;
import ch.uepaa.quickstart.fragments.AboutFragment;
import ch.uepaa.quickstart.graph.Graph;
import ch.uepaa.quickstart.graph.GraphView;
import ch.uepaa.quickstart.utils.ColorStorage;
import ch.uepaa.quickstart.utils.Logger;


public class RunActivity extends AppCompatActivity implements ConsoleFragment.ConsoleListener, ColorPickerFragment.ColorPickerListener, AboutFragment.ConsoleListener {

    private BackendlessUser user;
    private User userClass = new User();
    private Boolean flagBackButton = false;

    static final String userIdIntent = "BackendlessUserId";
    static final String objectIdUserIntent = "BackendlessObjectId";
    static final String deviceIdIntent = "BackendlessDeviceId";

    private static final String APP_KEY = "54557df6afda418e8854718f7e7bca8c"; // Check Patient

    public void enableP2PKit() {
        try {
            Logger.i("RunActivity enableP2PKit", "Enabling Patient Check");
            P2PKit.enable(this, APP_KEY, p2pKitStatusListener);
        } catch (AlreadyEnabledException e) {
            Logger.w("RunActivity enableP2PKit", "Patient Check is already enabled " + e.getLocalizedMessage());
        }
    }

    private final P2PKitStatusListener p2pKitStatusListener = new P2PKitStatusListener() {

        @Override
        public void onEnabled() {
            Logger.i("RunActivity P2PKitStatusListener", "Successfully enabled Patient Check");

            UUID ownNodeId = P2PKit.getMyPeerId();
            setupPeers(ownNodeId);
            updateBackendless(ownNodeId);
            startDiscovery();
        }

        @Override
        public void onDisabled() {
            Logger.i("RunActivity P2PKitStatusListener", "Patient Check disabled");
        }

        @Override
        public void onError(StatusResult statusResult) {
            handleStatusResult(statusResult);
            Logger.e("RunActivity P2PKitStatusListener", "Patient Check lifecycle error with code: " + statusResult.getStatusCode());
        }

        @Override
        public void onException(Throwable throwable) {
            String errorMessage = "An error occurred and Patient Check stopped, please try again.";
            showError(errorMessage, true);
            Logger.e("RunActivity P2PKitStatusListener", "Patient Check threw an exception: " + Log.getStackTraceString(throwable));
            teardownPeers();
        }
    };

    public void disableP2PKit() {
        Logger.i("RunActivity P2PKit", "Disable Patient Check");

        if (P2PKit.isEnabled()) {
            P2PKit.disable();
            teardownPeers();
        }
    }

    @Override
    public void startDiscovery() {
        Logger.i("RunActivity P2PKit", "Start discovery");

        byte[] ownDiscoveryData = loadOwnDiscoveryData();

        try {
            P2PKit.enableProximityRanging();
            P2PKit.startDiscovery(ownDiscoveryData, DiscoveryPowerMode.HIGH_PERFORMANCE, mDiscoveryListener);
        } catch (DiscoveryInfoTooLongException e) {
            Logger.w("P2PKit", "Can not start discovery, discovery info is to long " + e.getLocalizedMessage());
        }
    }

    @Override
    public void stopDiscovery() {
        Logger.i("RunActivity P2PKit", "Stop discovery");
        P2PKit.stopDiscovery();

        for (Peer peer : nearbyPeers) {
            handlePeerLost(peer);
        }

        nearbyPeers.clear();
    }

    private boolean pushNewDiscoveryInfo(byte[] data) {
        Logger.i("RunActivity P2PKit", "Push new discovery info");
        boolean success = false;

        try {
            P2PKit.pushDiscoveryInfo(data);
            success = true;
        } catch (DiscoveryInfoTooLongException e) {
            Logger.e("RunActivity P2PKit", "Failed to push new discovery info, info too long: " + e.getLocalizedMessage());
        } catch (DiscoveryInfoUpdatedTooOftenException e) {
            Logger.e("RunActivity P2PKit", "Failed to push new discovery info due to throttling: " + e.getLocalizedMessage());
        }

        return success;
    }

    // Discovery events listener
    private final DiscoveryListener mDiscoveryListener = new DiscoveryListener() {

        @Override
        public void onStateChanged(final int state) {
            handleDiscoveryStateChange(state);
            //Sale a PatientCheck
            startPatientCheck(state);

        }

        @Override
        public void onPeerDiscovered(final Peer peer) {
            Logger.i("RunActivity DiscoveryListener", "Peer discovered: " + peer.getPeerId() + ". Proximity strength: " + peer.getProximityStrength());
            nearbyPeers.add(peer);
            handlePeerDiscovered(peer);
        }

        @Override
        public void onPeerLost(final Peer peer) {
            Logger.i("RunActivity DiscoveryListener", "Peer lost: " + peer.getPeerId());
            nearbyPeers.remove(peer);
            handlePeerLost(peer);
        }

        @Override
        public void onPeerUpdatedDiscoveryInfo(Peer peer) {
            Logger.i("RunActivity DiscoveryListener", "Peer updated discovery info: " + peer.getPeerId());
            handlePeerUpdatedDiscoveryInfo(peer);
        }

        @Override
        public void onProximityStrengthChanged(Peer peer) {
            Logger.i("RunActivity DiscoveryListener", "Peer changed proximity strength: " + peer.getPeerId() + ". Proximity strength: " + peer.getProximityStrength());
            handlePeerChangedProximityStrength(peer);
        }




    };

    private ColorStorage storage;
    private int defaultColor;
    private GraphView graphView;
    private final Set<Peer> nearbyPeers = new HashSet<>();

    private String objectIdDoc;
    private String userIdDoc;
    private String deviceIdDoc;
    private String peerIdDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.i("RunActivity", "Inicio");

        checkLocationPermission();

        setContentView(R.layout.run_activity);

        defaultColor = getResources().getColor(R.color.graph_node);
        storage = new ColorStorage(this);

        graphView = (GraphView) findViewById(R.id.graph);

        FloatingActionButton colorActionButton = (FloatingActionButton) findViewById(R.id.color_action);
        colorActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });

        Intent intent = getIntent();
        userIdDoc = intent.getStringExtra(userIdIntent);
        objectIdDoc = intent.getStringExtra(objectIdUserIntent);
        deviceIdDoc = intent.getStringExtra(deviceIdIntent);

        Log.i("RunActivity", "userId: " + userIdDoc);
        Log.i("RunActivity", "message: " + objectIdDoc);

        enableP2PKit();

        /// Messeages
        Channel channel = Backendless.Messaging.subscribe( "default" );
        channel.addMessageListener( new MessageInfoCallback()
        {
            @Override
            public void handleResponse( PublishMessageInfo message )
            {
                Log.i( "MYAPP", "Published message - " + message.getMessage() );
                Log.i( "MYAPP", "Publisher ID - " + message.getPublisherId() );
                Log.i( "MYAPP", "Message headers - " + message.getHeaders() );
                Log.i( "MYAPP", "Message subtopic " + message.getSubtopic() );
            }

            @Override
            public void handleFault( BackendlessFault fault )
            {
                Log.e( "MYAPP", "Error processing a message " + fault );
            }
        } );

    }



    @Override
    public void onDestroy() {
        Log.i("RunActivity", "onDestroy");
        disableP2PKit();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        Log.i("RunActivity", "onStart");
        super.onStart();

        if (P2PKit.isEnabled()) {
            P2PKit.setDiscoveryPowerMode(DiscoveryPowerMode.HIGH_PERFORMANCE);
        }
    }

    @Override
    public void onStop() {
        Log.i("RunActivity", "onStop");
        super.onStop();

        if (P2PKit.isEnabled()) {
            P2PKit.setDiscoveryPowerMode(DiscoveryPowerMode.LOW_POWER);
        }
    }

    @Override
    protected void onResume() {
        Log.i("RunActivity", "onResume");
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // locationManager.requestLocationUpdates(provider, 400, 1, RunActivity.this);
        }

        if (BuildConfig.BUILD_CONFIGURATION.equals("prod")) {
            hideSystemBars();
        }

        if (P2PKit.isEnabled()) {
            handleDiscoveryStateChange(P2PKit.getDiscoveryState());

            if (P2PKit.getDiscoveryState() == DiscoveryListener.STATE_OFF) {
                startDiscovery();
            }
        }
    }

    @Override
    protected void onPause() {
        Log.i("RunActivity", "onPause");
        super.onPause();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {


           // locationManager.removeUpdates(RunActivity.this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("RunActivity", "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("RunActivity", "onOptionsItemSelected");
        switch (item.getItemId()) {

            case R.id.action_console:
                showConsole();
                return true;

            case R.id.action_about:
                showAbout();
                return true;

            case R.id.action_enablekit:
                if (P2PKit.isEnabled()) {
                    disableP2PKit();
                } else {
                    enableP2PKit();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupPeers(final UUID ownNodeId) {
        Log.i("RunActivity", "setupPeers");
        byte[] ownDiscoveryData = loadOwnDiscoveryData();
        int ownColorCode = ColorStorage.getOrCreateColorCode(ownDiscoveryData, ColorStorage.createRandomColor());

        if (ownDiscoveryData == null) {
            storage.saveColor(ownColorCode);
        }

        Graph graph = graphView.getGraph();
        graph.setup(ownNodeId);
        graph.addNode(ownNodeId);
        graph.setNodeColor(ownNodeId, ownColorCode);
    }

    private void handlePeerDiscovered(final Peer peer) {
        Log.i("RunActivity", "handlePeerDiscovered");
        UUID peerId = peer.getPeerId();
        byte[] peerDiscoveryInfo = peer.getDiscoveryInfo();
        int peerColor = ColorStorage.getOrCreateColorCode(peerDiscoveryInfo, defaultColor);
        float proximityStrength = (peer.getProximityStrength() - 1f) / 4;
        boolean proximityStrengthImmediate = peer.getProximityStrength() == ProximityStrength.IMMEDIATE;

        Graph graph = graphView.getGraph();
        graph.addNode(peerId);
        graph.setNodeColor(peerId, peerColor);
        graph.setEdgeStrength(peerId, proximityStrength);
        graph.setHighlighted(peerId, proximityStrengthImmediate);
    }

    private void handlePeerLost(final Peer peer) {
        Log.i("RunActivity", "handlePeerLost");
        UUID peerId = peer.getPeerId();
        Graph graph = graphView.getGraph();
        graph.removeNode(peerId);
        graph.updateOwnNode();
    }

    private void handlePeerUpdatedDiscoveryInfo(final Peer peer) {
        Log.i("RunActivity", "handlePeerUpdatedDiscoveryInfo");
        UUID peerId = peer.getPeerId();
        byte[] peerDiscoveryInfo = peer.getDiscoveryInfo();

        int peerColor = ColorStorage.getOrCreateColorCode(peerDiscoveryInfo, defaultColor);

        Graph graph = graphView.getGraph();
        graph.setNodeColor(peerId, peerColor);
    }

    private void handlePeerChangedProximityStrength(final Peer peer) {
        Log.i("RunActivity", "handlePeerChangedProximityStrength");
        UUID peerId = peer.getPeerId();
        float proximityStrength = (peer.getProximityStrength() - 1f) / 4;
        boolean proximityStrengthImmediate = peer.getProximityStrength() == ProximityStrength.IMMEDIATE;

        Graph graph = graphView.getGraph();
        graph.setEdgeStrength(peerId, proximityStrength);
        graph.setHighlighted(peerId, proximityStrengthImmediate);
    }

    private void updateOwnDiscoveryInfo(int colorCode) {
        Log.i("RunActivity", "updateOwnDiscoveryInfo");
        if (!P2PKit.isEnabled()) {
            Toast.makeText(this, R.string.p2pkit_not_enabled, Toast.LENGTH_LONG).show();
            return;
        }

        byte[] newColorBytes = ColorStorage.getColorBytes(colorCode);

        if (!pushNewDiscoveryInfo(newColorBytes)) {
            Toast.makeText(this, R.string.p2pkit_discovery_info_update_failed, Toast.LENGTH_LONG).show();
            return;
        }

        storage.saveColor(colorCode);
        Graph graph = graphView.getGraph();
        UUID ownNodeId = P2PKit.getMyPeerId();
        graph.setNodeColor(ownNodeId, colorCode);
    }

    private byte[] loadOwnDiscoveryData() {
        return storage.loadColor();
    }

    private void teardownPeers() {
        Log.i("RunActivity", "teardownPeers");
        Graph graph = graphView.getGraph();
        graph.reset();
    }

    private void showColorPicker() {
        Log.i("RunActivity", "showColorPicker");
        byte[] colorData = storage.loadColor();
        int colorCode = ColorStorage.getOrCreateColorCode(colorData, defaultColor);

        ColorPickerFragment fragment = ColorPickerFragment.newInstance(colorCode);
        fragment.show(getFragmentManager(), ColorPickerFragment.FRAGMENT_TAG);
    }

    @Override
    public void onColorPicked(int colorCode) {
        updateOwnDiscoveryInfo(colorCode);
    }

    private void handleStatusResult(final StatusResult statusResult) {
        Log.i("RunActivity", "handleStatusResult");
        String description = "";

        if (statusResult.getStatusCode() == StatusResult.INVALID_APP_KEY) {
            description = "Invalid app key";
        }
        else if (statusResult.getStatusCode() == StatusResult.INVALID_APPLICATION_ID) {
            description = "Invalid application ID";
        }
        else if (statusResult.getStatusCode() == StatusResult.INCOMPATIBLE_CLIENT_VERSION) {
            description = "Incompatible Patient Check (SDK) version, please update";
        }
        else if (statusResult.getStatusCode() == StatusResult.SERVER_CONNECTION_UNAVAILABLE) {
            description = "Server connection not available";
        }

        showError(description, true);
    }

    private void handleDiscoveryStateChange(final int state) {
        Log.i("RunActivity", "handleDiscoveryStateChange");

        if (state == DiscoveryListener.STATE_OFF) {
            return;
        }

        if ((state & DiscoveryListener.STATE_LOCATION_PERMISSION_NOT_GRANTED) == DiscoveryListener.STATE_LOCATION_PERMISSION_NOT_GRANTED) {
            Toast.makeText(this, R.string.p2pkit_state_no_location_permission, Toast.LENGTH_LONG).show();
        }
        else if ((state & DiscoveryListener.STATE_SERVER_CONNECTION_UNAVAILABLE) == DiscoveryListener.STATE_SERVER_CONNECTION_UNAVAILABLE) {
            Toast.makeText(this, R.string.p2pkit_state_offline, Toast.LENGTH_LONG).show();
        }
        else if (state != DiscoveryListener.STATE_ON) {
            Toast.makeText(this, R.string.p2pkit_state_suspended, Toast.LENGTH_LONG).show();
        }
    }

    private void showConsole() {
        Log.i("RunActivity", "showConsole");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(ConsoleFragment.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        ConsoleFragment fragment = ConsoleFragment.newInstance();
        fragment.show(ft, ConsoleFragment.FRAGMENT_TAG);
    }

    private void showAbout() {
        Log.i("RunActivity", "showAbout");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(AboutFragment.FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        AboutFragment fragment = AboutFragment.newInstance();
        fragment.show(ft, AboutFragment.FRAGMENT_TAG);
    }

    private void showError(final String message, final boolean retry) {
        Log.i("RunActivity", "showError");
        final AlertDialog.Builder builder =  new AlertDialog.Builder(this).setTitle("App Error").setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        if (retry) {
            builder.setNegativeButton("Reintentar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    RunActivity.this.enableP2PKit();
                }
            });
        }

        builder.create().show();
    }

    private void hideSystemBars() {
        Log.i("RunActivity", "hideSystemBars");
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        Log.i("RunActivity", "checkLocationPermission");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(RunActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i("RunActivity", "onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                       // locationManager.requestLocationUpdates(provider, 400, 1, RunActivity.this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    private void updateBackendless(final UUID ownNodeId) {
        Log.i("RunActivity", "updateBackendless");

        String whereClause = "ownerId = '" + userIdDoc + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );
        Log.i("RunActivity", "whereClause: " + whereClause);

        Backendless.Data.of("User").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> facemap )
                    {
                        if (facemap.isEmpty()) {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(RunActivity.this);
                            builder.setTitle(R.string.error_database);
                            android.app.AlertDialog dialog = builder.create();
                            dialog.show();

                        } else {
                            Map userMap = facemap.get(0);
                            peerIdDoc = ownNodeId.toString();
                            userMap.put("peerId", peerIdDoc);
                            Log.i("RunActivity", "RunActivity PeerId: " + peerIdDoc);

                            Backendless.Data.of("User").save(userMap, new AsyncCallback<Map>() {
                                @Override
                                public void handleResponse(Map response) {
                                    // Contact instance has been updated
                                    Log.i("RunActivity", "User has been updated with peerId");

                                }

                                @Override
                                public void handleFault(BackendlessFault fault) {
                                    // an error has occurred, the error code can be retrieved with fault.getCode()
                                    Log.i("RunActivity", "Error: " + fault);
                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(RunActivity.this);
                                    builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                                    android.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            });
                        }


                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("RunActivity", "Error: " + fault);
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(RunActivity.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        android.app.AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });



    }


    /*
    public void bringPatientinfo(UUID peerIdPatient) {
        Log.i("RunActivity", "bringPatientinfo");
        String whereClause = "peerId = '" + peerIdPatient.toString() + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause( whereClause );
        queryBuilder.setSortBy( "created DESC" );


        Backendless.Data.of("Patients").find( queryBuilder,
                new AsyncCallback <List<Map>>()  {
                    @Override
                    public void handleResponse( List<Map> patientsMap )
                    {

                        Map patientMap = patientsMap.get(0);

                        Log.i("RunActivity", "bringPatientinfo: " + patientMap);
                        Patients patient = new Patients();
                        patient.setPeerIdPac(peerIdPatient.toString());
                        patient.setPeerIdDoc(peerIdDoc);
                        patient.setName(patientMap.get("name").toString());
                        patient.setDni(patientMap.get("dni").toString());
                        patient.setProfileImageUrl(patientMap.get("profileImageUrl").toString());
                        patient.setObjectIdwnerId(patientMap.get("ownerId").toString());
                        //control
                        Log.i("RunActivity", "name: " + patient.getName());
                        //carga Patients

                    }
                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                        Log.i("RunActivity", "Error: " + fault);
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(RunActivity.this);
                        builder.setMessage(fault.getMessage()).setTitle(R.string.registration_error);
                        android.app.AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });

    }

     */

    @Override
    public void onBackPressed() {
        // your stuff here
        flagBackButton = true;

        super.onBackPressed();
    }

    public void startPatientCheck(int state) {
        Logger.i("RunActivity DiscoveryListener", "Discovery state changed: " + state);
        if (state < 0) {
            Logger.i("RunActivity DiscoveryListener", "onStateChanged Se toco");
            Logger.i("RunActivity", "startPatientCheck" + flagBackButton);

            if (flagBackButton) {
                //Veremos
                flagBackButton = false;

            } else {
                Intent myIntent = new Intent(this, PatientCheck.class);
                myIntent.putExtra(PatientCheck.objectIdUser, objectIdDoc);
                myIntent.putExtra(PatientCheck.userIdUser, userIdDoc);
                myIntent.putExtra(PatientCheck.deviceIdUser, deviceIdDoc);
                myIntent.putExtra(PatientCheck.peerIdUser, peerIdDoc);
                startActivity(myIntent);
            }
        }



    }

}
