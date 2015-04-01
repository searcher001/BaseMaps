package net.nud.basemaps;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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
    private LocationDisplayManager lDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the path to the local TPK
        String waterPath = Environment.getExternalStorageDirectory() + File.separator + this.getResources().getString(R.string.local_water);
        String sewerPath = Environment.getExternalStorageDirectory() + File.separator + this.getResources().getString(R.string.local_sewer);

        // create the mapview
        mMapView = (MapView) findViewById(R.id.map);

        // create the local tpk
        localWaterLayer = new ArcGISLocalTiledLayer(waterPath);
        localSewerLayer = new ArcGISLocalTiledLayer(sewerPath);

        // set sewer visibility to default false
        localSewerLayer.setVisible(false);

        // add the map layers
        mMapView.addLayer(localWaterLayer);
        mMapView.addLayer(localSewerLayer);

        // enable panning over date line
        mMapView.enableWrapAround(true);

        // set Esri Logo
        mMapView.setEsriLogoVisible(false);

        // Set background to white
        mMapView.setMapBackground(Color.WHITE, 0, 0, 0);



        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source ==mMapView && status == STATUS.INITIALIZED) {
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSewerMenuItem = menu.getItem(1);
        mWaterMenuItem = menu.getItem(0);

        mWaterMenuItem.setChecked(true);

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
}
