package ch.heigvd.iict.sym_labo4;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.logging.Logger;

import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by Guillaume Laubscher, Ilias Goujgali, Eric Bousbaa on 30.01.2022
 * (C) 2019 - HEIG-VD, IICT
 */
public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    // Opengl
    private OpenGLRenderer opglr = null;
    private GLSurfaceView m3DView = null;
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private Sensor mMagneticField = null;
    private float[] rotationMatrix = new float[16];
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need fullscreen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // We initiate the view
        setContentView(R.layout.activity_compass);

        // We create the renderer
        this.opglr = new OpenGLRenderer(getApplicationContext());

        // Link to GUI
        this.m3DView = findViewById(R.id.compass_opengl);

        // Init opengl surface view
        this.m3DView.setRenderer(this.opglr);

        // Init sensor manager and sensors
        this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Fill arrays for future calculation
        Arrays.fill(rotationMatrix, 0);
        Arrays.fill(gravity, 0);
        Arrays.fill(geomagnetic, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register accelerometer
        mSensorManager.registerListener(this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        // Register magnetic field
        mSensorManager.registerListener(this,
                mMagneticField,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister all sensors when paused
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Update corresponding sensor data
        if (sensorEvent.sensor.getName().equals(mAccelerometer.getName())) {
            gravity = sensorEvent.values;
        } else if (sensorEvent.sensor.getName().equals(mMagneticField.getName())) {
            geomagnetic = sensorEvent.values;
        }

        // Calculate rotation matrix with updated data
        SensorManager.getRotationMatrix(
                opglr.swapRotMatrix(rotationMatrix),
                null,
                gravity,
                geomagnetic
        );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
