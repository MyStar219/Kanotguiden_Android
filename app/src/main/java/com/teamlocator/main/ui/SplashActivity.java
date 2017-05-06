package com.teamlocator.main.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.teamlocator.main.R;
import com.teamlocator.main.service.FusionService;
import com.teamlocator.main.service.MissionService;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class SplashActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;
    private static final int SETTINGS_REQUEST_GPS = 2;

    private MissionService missionService;
    private FusionService fusionService;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        missionService = new MissionService(this);
        fusionService = new FusionService(this);

        requestGPSPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient != null)
            googleApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // restart app
                Intent restartIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                TaskStackBuilder.create(this).addNextIntentWithParentStack(restartIntent).startActivities();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST_GPS) {
            if (resultCode == Activity.RESULT_OK) {
                downloadData();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(SplashActivity.this, "The app cant't work without GPS1", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void requestGPSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            connectApiClient();
        }
    }

    private void connectApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        googleApiClient.connect();
    }

    private void initializeGPS() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // **************************
        builder.setAlwaysShow(true); // this is the key ingredient
        // **************************

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(result1 -> {
            final Status status = result1.getStatus();
            final LocationSettingsStates state = result1
                    .getLocationSettingsStates();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    // All location settings are satisfied. The client can
                    // initialize location
                    // requests here.
                    downloadData();
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. But could be
                    // fixed by showing the user
                    // a dialog.
                    try {
                        // Show the dialog by calling
                        // startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(SplashActivity.this, SETTINGS_REQUEST_GPS);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Location settings are not satisfied. However, we have
                    // no way to fix the
                    // settings so we won't show the dialog.
                    downloadData();
                    break;
            }
        });
    }

    private void downloadData() {
        missionService.getMissionsFromServer()
//                .concatMap((Function<Object, ObservableSource<?>>) o1 -> fusionService.getLocationsFromServer())
//                .concatMap((Function<Object, ObservableSource<?>>) o2 -> fusionService.getRoutesFromServer())
                .subscribe(o3 -> {
                    SplashActivity.this.startActivity(new Intent(SplashActivity.this, MapsActivity.class));
                    SplashActivity.this.finish();
                }, throwable -> {
                    if (missionService.getCachedMissions() == null)
                        finish();
                    else {
                        SplashActivity.this.startActivity(new Intent(SplashActivity.this, MapsActivity.class));
                        SplashActivity.this.finish();
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        initializeGPS();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(SplashActivity.this, "The app cant't work without GPS3", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(SplashActivity.this, "The app cant't work without GPS4", Toast.LENGTH_LONG).show();
        finish();
    }
}
