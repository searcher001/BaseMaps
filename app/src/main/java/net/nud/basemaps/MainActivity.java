package net.nud.basemaps;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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


public class MainActivity extends ActionBarActivity {

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

        if (savedInstanceState != null) {
            Log.i("util", "water layer is: " + savedInstanceState.getBoolean("ISWATERLAYER"));
            Log.i("tuil", "Actual water layer is: " + ISWATERLAYER);
        }


        // set esri client token
        ApplicationToken appToken = new ApplicationToken();
        appToken.setAppToken();

        setContentView(R.layout.activity_main);

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

            if (ISWATERLAYER) {
                localSewerLayer.setVisible(false);
                localWaterLayer.setVisible(true);
            } else {
                localSewerLayer.setVisible(true);
                localWaterLayer.setVisible(false);
            }
        } else {

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
                                    double locy = location.getLatitude();
                                    double locx = location.getLongitude();
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
                                    Envelope zoomExtent = new Envelope(mapPoint,
                                            zoomWidth, zoomWidth);
                                    mMapView.setExtent(zoomExtent);
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

        mWaterMenuItem = menu.getItem(0);
        mSewerMenuItem = menu.getItem(1);
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

}
