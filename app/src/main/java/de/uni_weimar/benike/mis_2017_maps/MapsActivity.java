package de.uni_weimar.benike.mis_2017_maps;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnInfoWindowCloseListener,
        GoogleMap.OnMapLongClickListener
{
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;
    private Criteria mCriteria;

    private EditText mEditText;
    private Button mDeleteButton;
    private int mMarkerCount = 0;

    private Marker mLastSelectedMarker;
    private List<MarkerOptions> mMarkerOptions = new ArrayList<MarkerOptions>();
    private ToggleButton mPolyButton;
    private boolean mPolyActive = false;
    private Polygon mCurrentPoly;
    private List<LatLng> mCurrentPolyLatLngs = new ArrayList<LatLng>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mEditText = (EditText) findViewById(R.id.editText);
        mDeleteButton = (Button) findViewById(R.id.deleteButton);
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                mMarkerCount = 0;
                mMarkerOptions.clear();
                mPolyButton.setChecked(false);
                mCurrentPolyLatLngs.clear();
                mPolyActive = false;

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
            }
        });

        mPolyButton = (ToggleButton) findViewById(R.id.polyButton);
        mPolyButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    mPolyActive = true;
                } else {
                    mPolyActive = false;
                    mCurrentPolyLatLngs.clear();
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE );
            /* PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true); */
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);

        }
    }

    private void moveToMyLocation() {

        LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mCriteria = new Criteria();
        String bestProvider = String.valueOf(manager.getBestProvider(mCriteria, true));

        Location location = manager.getLastKnownLocation(bestProvider);
        if (location != null) {
            final double currentLatitude = location.getLatitude();
            final double currentLongitude = location.getLongitude();
            LatLng loc1 = new LatLng(currentLatitude, currentLongitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 15));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();

        //Initialize Google Play Services
        /*
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //Location Permission already granted
            //buildGoogleApiClient();
            initMapLocation();
        } else {
            //Request Location Permission
            checkLocationPermission();
        } */

        mMap.setContentDescription("MIS 2017 map");

        restoreMarkers();

        // Set listeners for marker events.  See the bottom of this class for their behavior.
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        mMap.setOnMapLongClickListener(this);

        // Pan to see all markers in view.
        // Cannot zoom to bounds until the map has a size.
        moveToMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    private LatLng calculatePolygonCentroid(List<LatLng> path) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(LatLng point : path)
            builder.include(point);
        LatLngBounds latLngBounds = builder.build();
        return latLngBounds.getCenter();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //Toast.makeText(this, "Long click on " + latLng.toString(), Toast.LENGTH_SHORT).show();

        if(mPolyActive) {
            mCurrentPolyLatLngs.add(latLng);
            if(mCurrentPolyLatLngs.size() == 1) {
                PolygonOptions initialPoly = new PolygonOptions()
                        .add(latLng)
                        .fillColor(Color.argb(80, 255, 0, 0))
                        .strokeColor(Color.argb(120, 255, 0, 0));
                mCurrentPoly = mMap.addPolygon(initialPoly);
            } else {
                mCurrentPoly.setPoints(mCurrentPolyLatLngs);
            }

            // Calculate and show area
            if(mCurrentPolyLatLngs.size() > 2) {
                double area = SphericalUtil.computeArea(mCurrentPolyLatLngs);
                LatLng centroid = calculatePolygonCentroid(mCurrentPolyLatLngs);
            }
        } else { // we are adding a marker
            String title = mEditText.getText().toString();
            mEditText.setText("");

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .draggable(true);
            mMap.addMarker(markerOptions);
            mMarkerOptions.add(markerOptions);

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("title" + Integer.toString((mMarkerCount)), title);
            editor.putString("latitude" + Integer.toString((mMarkerCount)), Double.toString(latLng.latitude));
            editor.putString("longitude" + Integer.toString((mMarkerCount)), Double.toString(latLng.longitude));
            mMarkerCount += 1;
            editor.putInt("markerCount", mMarkerCount);
            editor.commit();
        }
    }

    private void restoreMarkers() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        mMarkerCount = sharedPref.getInt("markerCount", 0);
        if (mMarkerCount > 0) {
            for (int i = 0; i < mMarkerCount; i++) {
                String title = sharedPref.getString("title" + i, "");
                double lat = Double.valueOf(sharedPref.getString("latitude" + i, "0"));
                double lng = Double.valueOf(sharedPref.getString("longitude" + i, "0"));
                //Toast.makeText(this, lat + "," + lng, Toast.LENGTH_LONG).show();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(title)
                        .draggable(true);
                mMap.addMarker(markerOptions);
                mMarkerOptions.add(markerOptions);
            }

        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {


        mLastSelectedMarker = marker;
        mEditText.setText(marker.getTitle());
        // We return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        //Toast.makeText(this, "Close Info Window", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        //Toast.makeText(this, "Info Window long click", Toast.LENGTH_SHORT).show();
    }

}
