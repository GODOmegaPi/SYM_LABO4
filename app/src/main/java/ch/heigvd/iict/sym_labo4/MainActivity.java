package ch.heigvd.iict.sym_labo4;

import android.Manifest;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //events
        findViewById(R.id.nav_4).setOnClickListener((view) -> {
            Intent i = new Intent(MainActivity.this, CompassActivity.class);
            startActivity(i);
        });
        findViewById(R.id.nav_5).setOnClickListener((view) -> {
            MainActivityPermissionsDispatcher.startBleActivityWithPermissionCheck(this);
        });
    }

    @NeedsPermission({Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    protected void startBleActivity() {
        Intent i = new Intent(MainActivity.this, BleActivity.class);
        startActivity(i);
    }

}
