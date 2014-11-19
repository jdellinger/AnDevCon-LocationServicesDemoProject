package com.dellingertech.andevcon.locationservicesdemo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
        implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        LocationClient.OnAddGeofencesResultListener {

    private static final String TAG = "LocationServicesDemo";
    private static final String FENCE_ID = "GEOFENCE_";
    private static final String TRANSITION_ACTION = "locationservicesdemo.GEOFENCE_TRANSITION";

    GoogleMap map;
    Marker marker;
    BitmapDescriptor redIcon;
    BitmapDescriptor greenIcon;

    LocationClient locationClient;
    LocationRequest locationRequest;
    List<Geofence> geofences;

    private GeofenceTransitionReceiver geofenceTransitionReceiver = new GeofenceTransitionReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();


        LatLng here = new LatLng(37.593967,	-122.364967);
        redIcon = BitmapDescriptorFactory.fromResource(R.drawable.red_marker);
        greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.green_marker);

        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 16));

        marker = map.addMarker(new MarkerOptions()
                .icon(redIcon)
                .flat(true)
                .title("AnDevcon")
                .snippet("November 2014")
                .position(here));
        locationClient = new LocationClient(this, this, this);
        locationRequest = new LocationRequest()
                .setInterval(3*1000)
                .setFastestInterval(1*1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        buildGeofences(new double[][]{
                {37.59787808147712, -122.36619472503662},
                {37.604610242676635, -122.37728834152222},
                {37.6000882015635, -122.38484144210815}
            }, new float[]{
                200,
                200,
                200
            }
        );
    }

    private void buildGeofences(double[][] points, float[] radii) {
        geofences = new ArrayList<Geofence>();
        for(int i=0;i<points.length;i++){
            geofences.add(
                    new Geofence.Builder()
                            .setRequestId(FENCE_ID+i)
                            .setCircularRegion(points[i][0], points[i][1], radii[i])
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .build()
            );
            map.addCircle(new CircleOptions()
                    .center(new LatLng(points[i][0], points[i][1]))
                    .radius(radii[i])
                    .strokeColor(Color.BLUE));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        if(locationClient.isConnected()){
            locationClient.removeLocationUpdates(this);
        }
        locationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        registerReceiver(geofenceTransitionReceiver, new IntentFilter(TRANSITION_ACTION));
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(geofenceTransitionReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location updated: lat="+location.getLatitude()+",long="+location.getLongitude());
        LatLng postion = new LatLng(location.getLatitude(), location.getLongitude());
        marker.setPosition(postion);
        map.moveCamera(CameraUpdateFactory.newLatLng(postion));

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");
        locationClient.requestLocationUpdates(locationRequest, this);
        Intent intent = new Intent(TRANSITION_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        locationClient.addGeofences(geofences, pendingIntent, this);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "connection failed");
    }

    @Override
    public void onAddGeofencesResult(int statusCode, String[] fenceIds) {
        Log.e(TAG, "Add Geofences result: "+statusCode+":"+fenceIds);
    }

    public class GeofenceTransitionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            final int transitionType = LocationClient.getGeofenceTransition(intent);

            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
                Log.e(TAG, "Fence entered");
                marker.setIcon(greenIcon);
            }else if(transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
                Log.e(TAG, "Fence exited");
                marker.setIcon(redIcon);
            }
        }
    }
}
