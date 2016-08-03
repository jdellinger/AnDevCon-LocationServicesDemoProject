package com.dellingertech.locationservicesdemo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient.*;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnConnectionFailedListener, ConnectionCallbacks, LocationListener, ResultCallback {

    private static final String TAG = "LocationServicesDemo";
    private static final float START_ZOOM = 12;
    private static final float UPDATE_ZOOM = 15;
    private static final String FENCE_ID_PREFIX = "FENCE_";
    private static final String TRANSITION_ACTION = "locationservicesdemo.GEOFENCE_TRANSITION";
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Marker marker;
    private List<Geofence> geofences;

    private BitmapDescriptor redIcon;
    private BitmapDescriptor greenIcon;
    private GeofenceTransitionReceiver geofenceTransitionReceiver = new GeofenceTransitionReceiver();

    private static final double[][] FENCE_LOCATIONS = new double[][]{
            {42.23151, -71.516705},
            {42.235887, -71.507199}
    };
    private static final float[] FENCE_RADII = new float[]{200, 200};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        redIcon = BitmapDescriptorFactory.fromResource(R.drawable.red_marker);
        greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.green_marker);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker in Boston, MA.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Boston and move the camera
        LatLng boston = new LatLng(42.346625, -71.084292);
        updateMap(boston, "Sheraton Boston", START_ZOOM);

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, createLocationRequest(), this);
        buildGeofences(FENCE_LOCATIONS, FENCE_RADII);
        Intent intent = new Intent(TRANSITION_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingResult addResult = LocationServices.GeofencingApi.addGeofences(googleApiClient, buildGeofencingRequest(), pendingIntent);
        addResult.setResultCallback(this);
    }

    private GeofencingRequest buildGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        return locationRequest;
    }

    private void updateMap(LatLng position, String label, float zoom){
        if(marker == null) {
            marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(label)
                    .icon(redIcon)
            );
        }
        marker.setPosition(position);
        marker.setTitle(label);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, String.format("Location changed (%f, %f)", location.getLatitude(), location.getLongitude()));
        updateMap(new LatLng(location.getLatitude(), location.getLongitude()), "Current Location", UPDATE_ZOOM);
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

    private void buildGeofences(double[][] points, float[] radii) {
        geofences = new ArrayList<Geofence>();
        for (int i = 0; i < points.length; i++) {
            geofences.add(
                    new Geofence.Builder()
                            .setRequestId(FENCE_ID_PREFIX + i)
                            .setCircularRegion(points[i][0], points[i][1], radii[i])
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .build()
            );
            mMap.addCircle(new CircleOptions()
                    .center(new LatLng(points[i][0], points[i][1]))
                    .radius(radii[i])
                    .strokeColor(Color.BLUE));
        }
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.i(TAG, "Add Geofences result: "+result.getStatus().isSuccess()+":"+result.getStatus().getStatusCode());
    }

    private class GeofenceTransitionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                String errorMessage = geofencingEvent.getErrorCode()+"";
                Log.e(TAG, String.format("Geofence error %d", geofencingEvent.getErrorCode()));
                return;
            }
            final int transitionType = geofencingEvent.getGeofenceTransition();

            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.e(TAG, "Fence entered");
                marker.setIcon(greenIcon);
            } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.e(TAG, "Fence exited");
                marker.setIcon(redIcon);
            } else {
                Log.i(TAG, String.format("Fence other %d", transitionType));
            }

        }
    }
}
