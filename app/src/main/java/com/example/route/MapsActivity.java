package com.example.route;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.route.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.Map;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private String commandStr;
    private LocationManager locationManager;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    private double lat = 0;
    private double lng = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        //設定監聽移動距離做更新(單位:m)
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
        //設定秒數做更新(單位:ms)
        long MIN_TIME_BW_UPDATES = 1000;
        commandStr = LocationManager.NETWORK_PROVIDER;
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COARSE_LOCATION);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        //declare the locationListener
        LocationListener locationListenerGPS = new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String msg = "New Latitude: " + latitude + "New Longitude: " + longitude;
                System.out.println(msg);
                LatLng HOME = new LatLng(latitude, longitude);
                String now = HOME.toString();
                mMap.addMarker(new MarkerOptions().position(HOME).title(now));
                PolylineOptions polylineOpt = new PolylineOptions().color(0xfffd8364).width(5);
                polylineOpt.add(new LatLng(lat, lng));
                polylineOpt.add(HOME);
                mMap.addPolyline(polylineOpt);
                lat = latitude;
                lng = longitude;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HOME, 15));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        login(latitude, longitude);
                    }
                }).start();
            }
        };


        //設定更新速度與距離
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerGPS);

        Location location = locationManager.getLastKnownLocation(commandStr);
        lat = location.getLatitude();
        lng = location.getLongitude();

        System.out.println(lat);
        System.out.println(lng);
        LatLng HOME = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(HOME).title("目前位置"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HOME, 15.0f));


    }

    private static void login(double lat, double lng){
        String url = "https://dr.kymco.com/api/login";
        String token = "";
        String acc = "ky5910";
        String pwd = "KY5910";

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create("acc=" + acc + "&pwd=" + pwd, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()){
                String header = response.header("Set-Cookie");
                token = header;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        String tmp = step2(token, lat, lng);
        Log.d("token", token);
        Log.d("tmp", tmp);
    }

    private static String  step2(String token, double lat, double lng){
        String url = "https://dr.kymco.com/es/eAPI/fmTrackLog";

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create("{\r\n\"uid\": \"0a3c6d13-dac1-48f9-b001-f92d00773e3e\"," +
                "\r\n\"lat\": \"" + lat + "\",\r\n\"lng\": \"" + lng + "\"\r\n}", mediaType);
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Cookie", token)
                .build();

        String jsonStr = "";
        try{
            Response response = client.newCall(request).execute();
            jsonStr = response.body().string();

        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(jsonStr);
        return jsonStr;
    }
}