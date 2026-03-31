package lk.raminsenanayake.thefeast.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.raminsenanayake.thefeast.R;
import lk.raminsenanayake.thefeast.databinding.ActivityMapsBinding;
import lk.raminsenanayake.thefeast.BuildConfig;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private Marker markerPin;
    private LatLng restaurant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        enableMyLocation();

        restaurant = new LatLng(6.949755, 80.05491);
        mMap.addMarker(new MarkerOptions().position(restaurant).title("The Feast"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurant, 18f));

        mMap.setOnMapLongClickListener(latLng -> {
            if (markerPin == null) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.person_pin));
                markerPin = mMap.addMarker(markerOptions);
            } else {
                markerPin.setPosition(latLng);
            }

            getDistance(restaurant, latLng);
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                setResult(Activity.RESULT_FIRST_USER);
                finish();
            }
        }
    }

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private void getDistance(LatLng start, LatLng end) {
        String origin = start.latitude + "," + start.longitude;
        String destination = end.latitude + "," + end.longitude;

        service.execute(() -> {
            try (GeoApiContext context = new GeoApiContext.Builder()
                    .apiKey(BuildConfig.MAPS_API_KEY).build()) {

                try {
                    DistanceMatrix result = DistanceMatrixApi.newRequest(context)
                            .origins(origin)
                            .destinations(destination)
                            .mode(TravelMode.DRIVING)
                            .await();

                    this.runOnUiThread(() -> {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Get selected location")
                                .setMessage("Do you want get this selected location as delivery location?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("distance", result.rows[0].elements[0].distance.inMeters);
                                    resultIntent.putExtra("location", destination);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();
                    });


                } catch (ApiException | InterruptedException e) {
                    Log.e(TAG, "Distance request failed: ", e);
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}