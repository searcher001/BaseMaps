package net.nud.basemaps;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;

import java.io.File;


public class MainActivity extends ActionBarActivity implements OnTaskCompleted, UpdateDialogFragment.UpdateDialogListener, LoginDialogFragment.LoginDialogListener{

    // menu items
    private MenuItem mSewerMenuItem;
    private MenuItem mWaterMenuItem;

    private MapView mMapView;
    private ArcGISLocalTiledLayer localWaterLayer;
    private ArcGISLocalTiledLayer localSewerLayer;
    private ArcGISLocalTiledLayer localOrthoLayer;
    private LocationDisplayManager lDisplayManager;
    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver downloadComplete;
    private Button checkUpdateButton;
    private String userName;
    private String password;

    private long waterDownloadID;
    private long sewerDownloadID;
    private long orthoDownloadID;

    private static String LASTSTATE;        // also contains the location manager
    private static Boolean ISWATERLAYER;
    private static Boolean ISORTHOON;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set esri client token
        ApplicationToken appToken = new ApplicationToken();
        appToken.setAppToken();

        // Create the map view
        createMapView();


        // hook into ortho Toggle button
        final Button orthoButton = (Button) findViewById(R.id.ortho_button);
        orthoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button orthoButton = (Button) findViewById(R.id.ortho_button);
                if (localOrthoLayer.isVisible()) {
                    localOrthoLayer.setVisible(false);
                    orthoButton.setText(R.string.button_ortho_turn_on);
                } else {
                    localOrthoLayer.setVisible(true);
                    orthoButton.setText(R.string.button_ortho_turn_off);
                }
            }
        });


        // Hook Update Button
        checkUpdateButton = (Button)findViewById(R.id.update_button);
        checkUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });

        // Register wifi receiver to toggle update button
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (checkConnectedNUD()) {
                    checkUpdateButton.setVisibility(View.VISIBLE);
                } else {
                    checkUpdateButton.setVisibility(View.GONE);
                }
            }
        };

        waterDownloadID = sewerDownloadID = orthoDownloadID = -1;

        // register download complete register
        downloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                long downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                // local common directory
                String localDataDir = context.getResources().getString(R.string.local_data) + File.separator + context.getResources().getString(R.string.common_files);

                // Create the path to the local TPK
                String waterPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + context.getResources().getString(R.string.local_water);
                String sewerPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + context.getResources().getString(R.string.local_sewer);
                String orthoPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + context.getResources().getString(R.string.ortho_photo);

                if (downloadID == waterDownloadID){
                    Log.i("DOWNLOAD", "In water file swap");
                    File oldWaterTPK = new File(waterPath);
                    File newWaterTPK = new File(waterPath + getResources().getString(R.string.temp_file));
                    oldWaterTPK.delete();
                    newWaterTPK.renameTo(new File(waterPath));
                }

                if (downloadID == sewerDownloadID) {
                    Log.i("DOWNLOAD", "In sewer file swap");
                    File oldSewerTPK = new File(sewerPath);
                    File newSewerTPK = new File(sewerPath + getResources().getString(R.string.temp_file));
                    oldSewerTPK.delete();
                    newSewerTPK.renameTo(new File(sewerPath));
                }

                if (downloadID == orthoDownloadID) {
                    Log.i("DOWNLOAD", "In ortho file swap");
                    File oldOrthoTPK = new File(orthoPath);
                    File newOrthoTPK = new File(orthoPath + getResources().getString(R.string.temp_file));
                    oldOrthoTPK.delete();
                    newOrthoTPK.renameTo(new File(orthoPath));
                }
            }
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mWaterMenuItem = menu.getItem(1);
        mSewerMenuItem = menu.getItem(2);
        if (ISWATERLAYER != null) {
            if (ISWATERLAYER) {
                mWaterMenuItem.setChecked(true);
            } else {
                mSewerMenuItem.setChecked(true);
            }
        } else {
            mWaterMenuItem.setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.Water_Layer:
                localWaterLayer.setVisible(true);
                localSewerLayer.setVisible(false);
                mWaterMenuItem.setChecked(true);
                return true;
            case R.id.Sewer_layer:
                localSewerLayer.setVisible(true);
                localWaterLayer.setVisible(false);
                mSewerMenuItem.setChecked(true);
                return true;
            case R.id.gps_btn:
                toggleGPS();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Call superclass
        super.onSaveInstanceState(savedInstanceState);

        //Save Extent
        LASTSTATE = mMapView.retainState();
        ISWATERLAYER = localWaterLayer.isVisible();

        // save ortho layer visibility
        ISORTHOON = localOrthoLayer.isVisible();

    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.pause();

    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.unpause();

        // On startup, check version on server
        checkVersion();

        // Register the WIFI receiver for check for updates button
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        // register the download complete to the download manager
        registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    /**
     *  Fired from checkVersion
     *      - Compare the server to local version and prompt upgrade if required
     * @param serverVersion - server version
     */
    public void onTaskCompleted(String serverVersion) {

        String localVersion = (Integer.toString(BuildConfig.VERSION_CODE) + "." + BuildConfig.VERSION_NAME);
        Integer integerComparison = versionCompare(serverVersion, localVersion);

        // if the versions don't match, prompt for dialog update
        if ( integerComparison != 0) {

            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            android.support.v4.app.Fragment prev = getSupportFragmentManager().findFragmentByTag("UpdateDialogFragment");

            if (prev != null) {
                DialogFragment df = (DialogFragment) prev;
                df.dismiss();
                transaction.remove(prev);
            }

            transaction.addToBackStack(null);
            DialogFragment dialog = new UpdateDialogFragment();
            dialog.show(getSupportFragmentManager(), "UpdateDialogFragment");
        }
    }


    /**
     *  Fired from checkTPK
     *      - If Update is available, prompt user to download new TPK
     * @param updateWater - True if water TPK needs update
     * @param updateSewer - True of Sewer TPK needs update
     * @param updateOrtho - True if Ortho TPK needs update
    */
    public void onTaskCompleted(Boolean connected, Boolean updateWater, Boolean updateSewer, Boolean updateOrtho) {

        Log.i("UPDATE", "Connected is: " + connected);


        if (connected){
            if (updateWater || updateSewer || updateOrtho) {
                Toast.makeText(this, "Downloading Map Updates", Toast.LENGTH_SHORT).show();
                try {

                    String userpass = "NUD\\" + userName + ":" + password;
                    String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
                    String localDataDir = this.getResources().getString(R.string.common_files);
                    DownloadManager downloadManager = (DownloadManager) (getSystemService(Context.DOWNLOAD_SERVICE));

                    // If Water needs update
                    if (updateWater) {
                        String url = getResources().getString(R.string.server_water_tpk);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(true)
                                .setTitle(getResources().getString(R.string.local_water))
                                .setDescription("Downloading via Basemaps...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir("NUDLocalData", localDataDir + File.separator + getResources().getString(R.string.local_water) + getResources().getString(R.string.temp_file))
                                .addRequestHeader("Authorization", basicAuth)
                                .setVisibleInDownloadsUi(true);
                        waterDownloadID = downloadManager.enqueue(request);
                        Log.i("DOWNLOAD", "water download ID is: " + waterDownloadID);
                    }

                    // if Sewer needs update
                    if (updateSewer) {
                        String url = getResources().getString(R.string.server_sewer_tpk);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(true)
                                .setTitle(getResources().getString(R.string.local_sewer))
                                .setDescription("Downloading via Basemaps...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir("NUDLocalData", localDataDir + File.separator + getResources().getString(R.string.local_sewer) + getResources().getString(R.string.temp_file))
                                .addRequestHeader("Authorization", basicAuth);
                        sewerDownloadID = downloadManager.enqueue(request);
                        Log.i("DOWNLOAD", "sewer download ID is: " + sewerDownloadID);
                    }

                    // if Ortho needs update
                    if (updateOrtho) {
                        String url = getResources().getString(R.string.server_ortho_tpk);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(true)
                                .setTitle(getResources().getString(R.string.ortho_photo))
                                .setDescription("Downloading via Basemaps...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir("NUDLocalData", localDataDir + File.separator + getResources().getString(R.string.ortho_photo) + getResources().getString(R.string.temp_file))
                                .addRequestHeader("Authorization", basicAuth);
                        orthoDownloadID = downloadManager.enqueue(request);
                        Log.i("DOWNLOAD", "ortho download ID is: " + orthoDownloadID);
                    }

                } catch (Exception ex) {
                    Log.i("EXCEPTION", "EXCEPTION CAUSED BY: " + ex.getMessage());
                }
            }
            else {
                Toast.makeText(this, "No map updates at this time", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Could Not Authenticate User", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

        UpdateApp update = new UpdateApp();
        update.setContext(getApplicationContext());
            update.execute(getResources().getString(R.string.basemap_server));
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Dialog dialogView = dialog.getDialog();
        dialogView.cancel();
    }

    @Override
    public void onLoginPositiveClick(DialogFragment dialog) {

        Dialog dialogView = dialog.getDialog();

        EditText user = (EditText) dialogView.findViewById(R.id.username);
        EditText pw = (EditText) dialogView.findViewById(R.id.password);
        userName = user.getText().toString();
        password = pw.getText().toString();

        if (checkConnectedNUD()) {
            CheckTPKTask checkTPKTask = new CheckTPKTask(this, userName, password, this);
            checkTPKTask.execute();
        }
    }

    @Override
    public void onLoginNegativeClick(DialogFragment dialog) {
        Dialog dialogView = dialog.getDialog();
        dialogView.cancel();
    }

    /**
     * Show the Login Dialog prompting user to log in
     *  - Will fire asynchronous call to sync valves
    */
    private void showLoginDialog() {
        DialogFragment dialog = new LoginDialogFragment();
        dialog.show(getSupportFragmentManager(), "LoginDialogFragment");
    }

    /**
     * Create the Map View from current local files
     *  - Sets the map touch listener
     *  - sets the GPS listener
     */
    private void createMapView() {

        // create the mapview
        mMapView = (MapView) findViewById(R.id.map);

        // local common directory
        String localDataDir = this.getResources().getString(R.string.local_data) + File.separator + this.getResources().getString(R.string.common_files);

        // Create the path to the local TPK
        String waterPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + this.getResources().getString(R.string.local_water);
        String sewerPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + this.getResources().getString(R.string.local_sewer);
        String orthoPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + this.getResources().getString(R.string.ortho_photo);

        // create the local tpk
        localWaterLayer = new ArcGISLocalTiledLayer(waterPath);
        localSewerLayer = new ArcGISLocalTiledLayer(sewerPath);
        localOrthoLayer = new ArcGISLocalTiledLayer(orthoPath);

        // set non water layer visibility to default false
        localSewerLayer.setVisible(false);
        localOrthoLayer.setVisible(false);

        // set opacity of ortho
        localOrthoLayer.setOpacity(0.5f);

        // add the map layers
        mMapView.addLayer(localWaterLayer);
        mMapView.addLayer(localSewerLayer);
        mMapView.addLayer(localOrthoLayer);

        // enable panning over date line
        mMapView.enableWrapAround(true);

        // set Esri Logo
        mMapView.setEsriLogoVisible(true);

        // Set background to white
        mMapView.setMapBackground(Color.WHITE, 0, 0, 0);

        // Recreate the view if device was rotated
        if (LASTSTATE != null) {
            mMapView.restoreState(LASTSTATE);

            // set the ortho layer visibility
            localOrthoLayer.setVisible(ISORTHOON);

            lDisplayManager = mMapView.getLocationDisplayManager();

            if (ISWATERLAYER) {
                localSewerLayer.setVisible(false);
                localWaterLayer.setVisible(true);
            } else {
                localSewerLayer.setVisible(true);
                localWaterLayer.setVisible(false);
            }
        } else {

            mMapView.setOnStatusChangedListener(null);

            mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

                @Override
                public void onStatusChanged(Object source, STATUS status) {
                    if (source == mMapView && status == STATUS.INITIALIZED) {
                        lDisplayManager = mMapView.getLocationDisplayManager();
                        lDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                        lDisplayManager.setLocationListener(new LocationListener() {


                            boolean locationChanged = false;

                            @Override
                            public void onLocationChanged(Location location) {
                                if (!locationChanged) {
                                    locationChanged = true;
                                    mMapView.setExtent(zoomEnvelope());
                                }
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {

                            }

                            @Override
                            public void onProviderEnabled(String provider) {

                            }

                            @Override
                            public void onProviderDisabled(String provider) {

                            }
                        });
                        lDisplayManager.start();
                    }
                }
            });
        }
    }

    /**
     * Enable or Disable the GPS
     */
    private void toggleGPS() {
        if (lDisplayManager == null) {
            return;
        }
        if (lDisplayManager.isStarted()) {
            lDisplayManager.stop();
        } else {
            lDisplayManager.start();
            if (lDisplayManager.isStarted()) {
                mMapView.setExtent(zoomEnvelope());
            }
        }
    }

    /**
     * Zoom in on user's current location
     * @return - the estimated area of where the user is located
     */
    private Envelope zoomEnvelope() {
        if (mMapView.getLocationDisplayManager().getLocation() != null) {
            double locy = mMapView.getLocationDisplayManager().getLocation().getLatitude();
            double locx = mMapView.getLocationDisplayManager().getLocation().getLongitude();
            com.esri.core.geometry.Point wgspoint = new com.esri.core.geometry.Point(locx, locy);
            com.esri.core.geometry.Point mapPoint = (com.esri.core.geometry.Point) GeometryEngine
                    .project(wgspoint,
                            SpatialReference.create(4326),
                            mMapView.getSpatialReference()
                    );
            Unit mapUnit = mMapView.getSpatialReference().getUnit();
            double zoomWidth = Unit.convertUnits(
                    1000,
                    Unit.create(LinearUnit.Code.FOOT_US),
                    mapUnit);

            return new Envelope(mapPoint, zoomWidth, zoomWidth);
        }

        return null;
    }

    /**
     *  Check connection to NUD Wifi, Then check version
     */
    private void checkVersion() {

        // if connected to NUD Wifi, check server version
        if (checkConnectedNUD())
        {
            CheckVersionTask checkVersion = new CheckVersionTask(this);
            checkVersion.execute(getResources().getString(R.string.version_file));
        }
    }

    /**
     *  Compares the string integers if they are equivalent
     * @param str1 - first String Integer
     * @param str2 - second String Integer
     * @return 0 if the Integers are equivalent
     */
    private Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i]))
        {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length)
        {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else
        {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

    /**
     *  Check to see if Connected to NUD wifi
     * @return - true if connected to NUD wifi
     */
    private Boolean checkConnectedNUD() {
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifi = wifiManager.getConnectionInfo();
        if (wifi != null) {
            String connection = wifi.getSSID().replace("\"", "");

            return connection.equalsIgnoreCase(getString(R.string.nud_wifi)) || connection.equalsIgnoreCase(getString(R.string.nud_wifi_5g));
        }
        return false;
    }

}
