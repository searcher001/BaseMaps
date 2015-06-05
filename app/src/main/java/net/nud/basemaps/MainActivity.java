package net.nud.basemaps;

import android.app.Dialog;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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


public class MainActivity extends ActionBarActivity implements OnTaskCompleted, UpdateDialogFragment.UpdateDialogListener {

    // menu items
    private MenuItem mSewerMenuItem;
    private MenuItem mWaterMenuItem;

    private MapView mMapView;
    private ArcGISLocalTiledLayer localWaterLayer;
    private ArcGISLocalTiledLayer localSewerLayer;
    private ArcGISLocalTiledLayer localOrthoLayer;
    private LocationDisplayManager lDisplayManager;

    private static String LASTSTATE;        // also contains the location manager
    private static Boolean ISWATERLAYER;
    private static Boolean ISORTHOON;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("version", "Version number is: " + BuildConfig.VERSION_CODE );

        setContentView(R.layout.activity_main);

        // set esri client token
        ApplicationToken appToken = new ApplicationToken();
        appToken.setAppToken();
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

        // hook into ortho Toggle button
        final Button orthoButton = (Button) findViewById(R.id.ortho_button);
        orthoButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button orthoButton = (Button)findViewById(R.id.ortho_button);
                if (localOrthoLayer.isVisible()){
                    localOrthoLayer.setVisible(false);
                    orthoButton.setText(R.string.button_ortho_turn_on);
                } else {
                    localOrthoLayer.setVisible(true);
                    orthoButton.setText(R.string.button_ortho_turn_off);
                }
            }
        });
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


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
    }

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

    // check connection to NUD Wifi, then check version
    private void checkVersion() {

        // Check if connected to NUD wifi
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Boolean testWifi = wifiInfo.getSSID().compareTo("\"" + getString(R.string.nud_wifi) +"\"") == 0;
        Boolean testWifi5G = wifiInfo.getSSID().compareTo("\"" + getString(R.string.nud_wifi_5g) +"\"") == 0;

        // if connected to NUD Wifi, check server version
        if (testWifi || testWifi5G)
        {
            CheckVersionTask checkVersion = new CheckVersionTask(this);
            checkVersion.execute(getResources().getString(R.string.version_file));

        }
    }

    public void onTaskCompleted(String args) {

        String localVersion = (Integer.toString(BuildConfig.VERSION_CODE) + "." + BuildConfig.VERSION_NAME);
        Integer integerComparison = versionCompare(args, localVersion);

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

    private Integer versionCompare(String str1, String str2)
    {
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

}
