package com.teamlocator.main.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.teamlocator.main.R;
import com.teamlocator.main.model.MapLocation;
import com.teamlocator.main.model.MapRoute;
import com.teamlocator.main.model.MarkerInfo;
import com.teamlocator.main.model.Mission;
import com.teamlocator.main.service.FusionService;
import com.teamlocator.main.service.KanotguidenService;
import com.teamlocator.main.service.MissionService;
import com.teamlocator.main.utils.ConnectionUtils;
import com.teamlocator.main.utils.MiscUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.teamlocator.main.R.id.main_map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;
    private static final int DIALOG_DURATION = 5000;
    private static final int DEFAULT_ZOOM = 10;

    private FusionService fusionService;
    private MissionService missionService;
    private Gson gson = new Gson();

    private GoogleApiClient googleApiClient;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Marker myMarker;
    private Location lastLocation;

    private ProgressDialog progressDialog;
    private TextView tvCachePlaceholder;

    private BroadcastReceiver missionBroadcastReceiver;
    private BroadcastReceiver resendBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        fusionService = new FusionService(this);
        missionService = new MissionService(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");

        tvCachePlaceholder = (TextView) findViewById(R.id.main_cache_placeholder);

        missionBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startMission((Mission) intent.getSerializableExtra(MissionService.EXTRA_MISSION));
            }
        };
        registerReceiver(missionBroadcastReceiver, new IntentFilter(MissionService.ACTION_MISSION_FOUND));

        resendBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tvCachePlaceholder.setVisibility(View.VISIBLE);
            }
        };
        registerReceiver(resendBroadcastReceiver, new IntentFilter(MissionService.ACTION_MISSION_SEND_CACHED));

        // initialize buttons
        ImageButton btnUnlock = (ImageButton) findViewById(R.id.main_buttons_unlock);
        ImageButton btnWorld = (ImageButton) findViewById(R.id.main_buttons_world);
        ImageButton btnRefresh = (ImageButton) findViewById(R.id.main_buttons_refresh);
        ImageButton btnZoomIn = (ImageButton) findViewById(R.id.main_zoom_in);
        ImageButton btnZoomOut = (ImageButton) findViewById(R.id.main_zoom_out);
        btnUnlock.setOnClickListener(this);
        btnWorld.setOnClickListener(this);
        btnRefresh.setOnClickListener(this);
        btnZoomIn.setOnClickListener(this);
        btnZoomOut.setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(main_map);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(missionBroadcastReceiver);
        unregisterReceiver(resendBroadcastReceiver);

        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMap != null) {
            if (MiscUtils.isServiceRunning(this, KanotguidenService.class)) {
                Mission mission = missionService.checkIfNearMission(this);
                Mission currentMission = missionService.getCurrentMission();
                if (mission != null && (currentMission == null || !mission.getMission_code().equals(currentMission.getMission_code()))) {
                    startMission(mission);
                }
            } else {
                Toast.makeText(this, "Restarting service", Toast.LENGTH_SHORT).show();
                startService(new Intent(this, KanotguidenService.class));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // restart app
                    Intent restartIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(restartIntent).startActivities();
                }
                break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker marker) {

                MarkerInfo markerInfo = gson.fromJson(marker.getTitle(), MarkerInfo.class);

                if (markerInfo.getTitle().isEmpty() && markerInfo.getDescription().isEmpty()) {
                    return new View(MapsActivity.this);
                } else
                    return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                MarkerInfo markerInfo = gson.fromJson(marker.getTitle(), MarkerInfo.class);

                if (markerInfo.getTitle().isEmpty() && markerInfo.getDescription().isEmpty()) {
                    return null;
                }

                LinearLayout infoWindow = (LinearLayout) MapsActivity.this.getLayoutInflater().inflate(R.layout.layout_marker_info, null, false);
                TextView tvName = (TextView) infoWindow.findViewById(R.id.marker_name);
                TextView tvDescription = (TextView) infoWindow.findViewById(R.id.marker_description);
                TextView tvLink = (TextView) infoWindow.findViewById(R.id.marker_link);

                if (markerInfo.getTitle().isEmpty()) {
                    tvName.setVisibility(View.GONE);
                } else {
                    tvName.setVisibility(View.VISIBLE);
                    tvName.setText(markerInfo.getTitle());
                }

                if (markerInfo.getDescription().isEmpty()) {
                    tvDescription.setVisibility(View.GONE);
                } else {
                    tvDescription.setVisibility(View.VISIBLE);
                    tvDescription.setText(Html.fromHtml(markerInfo.getDescription()));
                }

                if (markerInfo.getLink().isEmpty())
                    tvLink.setVisibility(View.GONE);
                else
                    tvLink.setVisibility(View.VISIBLE);
                infoWindow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("", "");
                    }
                });

                return infoWindow;
            }
        });
        mMap.setOnInfoWindowClickListener(marker -> {
            MarkerInfo markerInfo = gson.fromJson(marker.getTitle(), MarkerInfo.class);

            if (!markerInfo.getLink().isEmpty()) {

                String url = markerInfo.getLink();
                if (!url.startsWith("http://"))
                    url = "http://" + url;

                Intent wwwIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                wwwIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(wwwIntent);
            }
        });

        downloadData();

        if (lastLocation != null) {
            setMyLocation(lastLocation);
        }

        startService(new Intent(this, KanotguidenService.class));
    }

    private void setMyLocation(Location location) {
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_my);
        MarkerInfo markerInfo = new MarkerInfo("My location", "", "");

        MarkerOptions marker = new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(gson.toJson(markerInfo))
                .icon(bitmapDescriptor);

        myMarker = mMap.addMarker(marker);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myMarker.getPosition(), DEFAULT_ZOOM));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_buttons_unlock:
                List<Mission> skippedMissions = missionService.getSkippedMissions();
                Mission missionInRange = missionService.checkIfNearMission(this);

                if (skippedMissions.isEmpty()) {
                    startMission(missionInRange);
                } else if (ConnectionUtils.isConnected(this)) {
                    startMission(skippedMissions.get(0));
                } else {
                    if (missionInRange == null)
                        showPopupDialog("You have completed missions that are not submitted, but there is no internet. Please try again later");
                    else
                        startMission(missionInRange);
                }
                break;
            case R.id.main_buttons_world:
                String url = missionService.getCachedMissions().getExternalLink();
                if (!url.startsWith("http://"))
                    url = "http://" + url;
                Intent wwwIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(wwwIntent);
                break;
            case R.id.main_buttons_refresh:
                if (mMap != null && myMarker != null)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myMarker.getPosition(), DEFAULT_ZOOM));
                break;
            case R.id.main_zoom_in:
                mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom + 1f));
                break;
            case R.id.main_zoom_out:
                mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1f));
                break;
        }
    }

    private void startMission(Mission mission) {
        missionService.setCurrentMission(mission);
        if (mission == null) {
            showPopupDialog("You are not within the required range");
        } else {
            AlertDialog.Builder missionDialog = new AlertDialog.Builder(this);
            missionDialog.setCancelable(false);

            final View missionView = getLayoutInflater().inflate(R.layout.dialog_mission, null, false);
            TextView tvCode = (TextView) missionView.findViewById(R.id.dialog_mission_code);
            TextView tvTitle = (TextView) missionView.findViewById(R.id.dialog_mission_title);
            tvCode.setText("Mission Code: " + mission.getMission_code());
            tvTitle.setText("Mission Title: " + mission.getTitle());
            missionDialog.setTitle("Congratulations!");
            missionDialog.setView(missionView);
            AlertDialog dialog = missionDialog.create();
            dialog.show();

            // Hide after some seconds
            final Handler handler = new Handler();
            final Runnable runnable = () -> {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                showMissionForm(mission);
            };
            handler.postDelayed(runnable, DIALOG_DURATION);
        }
    }

    private void showMissionForm(Mission mission) {
        mission.setDate(System.currentTimeMillis());
        if (ConnectionUtils.isConnected(this)) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setCancelable(false);

            final View view = getLayoutInflater().inflate(R.layout.dialog_form, null, false);
            EditText etName = (EditText) view.findViewById(R.id.dialog_form_name);
            EditText etEmail = (EditText) view.findViewById(R.id.dialog_form_email);
            EditText etDate = (EditText) view.findViewById(R.id.dialog_form_date);
            EditText etCode = (EditText) view.findViewById(R.id.dialog_form_code);
            EditText etLocation = (EditText) view.findViewById(R.id.dialog_form_location);
            Button btnSend = (Button) view.findViewById(R.id.dialog_form_send);
            Button btnCancel = (Button) view.findViewById(R.id.dialog_form_cancel);

            etName.setText(missionService.getUserName());
            etEmail.setText(missionService.getUserEmail());

            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            etDate.setText(formatter.format(new Date(mission.getDate())));
            etCode.setText(mission.getMission_code());
            etLocation.setText(mission.getLat() + "/" + mission.getLon());
            adb.setView(view);

            AlertDialog alertDialog = adb.create();
            alertDialog.show();

            btnCancel.setOnClickListener(v -> alertDialog.dismiss());

            btnSend.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();

                if (name.isEmpty())
                    etName.setError("Please fill your name");
                else if (email.isEmpty())
                    etEmail.setError("Please fill your email");
                else {
                    MiscUtils.hideSoftwareKeyboard(this);
                    missionService.setUserName(etName.getText().toString().trim());
                    missionService.setUserEmail(etEmail.getText().toString().trim());
                    missionService.sendMissionToServer(name, email, mission).subscribe(
                            response -> {
                                String result = response.string();
                                if (result.toLowerCase().contains("error")) {
                                    showPopupDialog("\nForm didn't submit. Please try again using unlock button\n\n(" + result + ")\n");
                                } else {
                                    showPopupDialog("\nMission submitted. Thank you!\n");

                                    missionService.removeMissionFromSkipped(mission);
                                    if (missionService.getSkippedMissions().isEmpty())
                                        tvCachePlaceholder.setVisibility(View.GONE);
                                }
                                alertDialog.dismiss();
                            },
                            throwable -> {
                                showPopupDialog("\nForm didn't submit. Please try again using unlock button\n\n(" + throwable.getMessage() + ")\n");
                            });
                }
            });

//            // Hide after some seconds
//            final Handler handler = new Handler();
//            final Runnable runnable = () -> {
//                if (alertDialog.isShowing()) {
//                    btnSend.performClick();
//                }
//            };
//            handler.postDelayed(runnable, DIALOG_DURATION);
        } else {
            missionService.addMissionToSkipped(mission);
            showPopupDialog("There is no internet connection at the moment. Please again later");
        }
    }

    private void downloadData() {
        fusionService.getLocationsFromServer()
//                .doOnComplete(this::showLocations)
                .subscribe(mapRoutes -> showLocations(), throwable -> showLocations());
        fusionService.getRoutesFromServer()
                .subscribe(mapRoutes -> showRoutes(), throwable -> showRoutes());
    }

    private void showRoutes() {
        new Thread(() -> {
            BitmapDescriptor bitmapA = BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_a);
            BitmapDescriptor bitmapB = BitmapDescriptorFactory.fromResource(R.drawable.icon_marker_b);
            for (MapRoute mapRoute : fusionService.getRoutesFromCache()) {
                PolylineOptions polyline = new PolylineOptions().addAll(mapRoute.getRoute());
                polyline.color(Color.parseColor(mapRoute.getColor()));

                MarkerInfo markerInfo = new MarkerInfo(mapRoute.getName(), "", mapRoute.getLink());

                MarkerOptions start = new MarkerOptions()
                        .position(mapRoute.getRoute().get(0))
                        .title(gson.toJson(markerInfo))
                        .icon(bitmapA);
                MarkerOptions end = new MarkerOptions()
                        .position(mapRoute.getRoute().get(mapRoute.getRoute().size() - 1))
                        .title(gson.toJson(markerInfo))
                        .icon(bitmapB);

                runOnUiThread(() -> {
                    mMap.addPolyline(polyline);
                    mMap.addMarker(start);
                    mMap.addMarker(end);
                });
            }
        }).start();
    }

    private void showLocations() {
        new Thread(() -> {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_marker);
            for (MapLocation mapLocation : fusionService.getMarkersFromCache()) {
                MarkerInfo markerInfo = new MarkerInfo(mapLocation.getName(), mapLocation.getDescription(), mapLocation.getLink());

                MarkerOptions marker = new MarkerOptions()
                        .position(mapLocation.getLatLng())
                        .title(gson.toJson(markerInfo))
                        .icon(bitmapDescriptor);
                runOnUiThread(() -> mMap.addMarker(marker));
            }
        }).start();
    }

    private void showPopupDialog(String message) {
        AlertDialog.Builder failedDialog = new AlertDialog.Builder(this);
        failedDialog.setCancelable(false);

        failedDialog.setMessage(message);
        AlertDialog dialog = failedDialog.create();
        dialog.show();

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = () -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        };
        handler.postDelayed(runnable, DIALOG_DURATION);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mapFragment.getMapAsync(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(5 * 1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Can`t enable GPS", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Can`t enable GPS", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null)
            return;

        if (myMarker == null) {
            setMyLocation(location);
        } else {
            myMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }
}
