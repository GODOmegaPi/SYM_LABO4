package ch.heigvd.iict.sym_labo4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity;
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter;
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by Guillaume Laubscher, Ilias Goujgali, Eric Bousbaa on 30.01.2022
 * (C) 2019 - HEIG-VD, IICT
 */
public class BleActivity extends BaseTemplateActivity {

    private static final String TAG = BleActivity.class.getSimpleName();

    //system services
    private BluetoothAdapter bluetoothAdapter = null;

    public int test = 0;

    //view model
    private BleOperationsViewModel bleViewModel = null;

    //gui elements
    private View operationPanel = null;
    private View scanPanel = null;

    private ListView scanResults = null;
    private TextView emptyScanResults = null;

    private TextView temperature = null;
    private Button temperatureButton = null;

    private TextView clickedButtons = null;

    private EditText integer = null;
    private Button integerBtn = null;

    private TextView time = null;
    private Button timeBtn = null;

    //menu elements
    private MenuItem scanMenuBtn = null;
    private MenuItem disconnectMenuBtn = null;

    //adapters
    private ResultsAdapter scanResultsAdapter = null;

    //states
    private Handler handler = null;
    private boolean isScanning = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        this.handler = new Handler();

        //enable and start bluetooth - initialize bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        //link GUI
        this.operationPanel = findViewById(R.id.ble_operation);
        this.scanPanel = findViewById(R.id.ble_scan);

        this.scanResults = findViewById(R.id.ble_scanresults);
        this.emptyScanResults = findViewById(R.id.ble_scanresults_empty);

        this.temperature = findViewById(R.id.ble_temperature);
        this.temperatureButton = findViewById(R.id.ble_temperature_btn);
        this.clickedButtons = findViewById(R.id.ble_clicked_buttons);
        this.integer = findViewById(R.id.ble_integer);
        this.integerBtn = findViewById(R.id.ble_integer_btn);
        this.time = findViewById(R.id.ble_time);
        this.timeBtn = findViewById(R.id.ble_time_btn);

        //manage scanned item
        this.scanResultsAdapter = new ResultsAdapter(this);
        this.scanResults.setAdapter(this.scanResultsAdapter);
        this.scanResults.setEmptyView(this.emptyScanResults);

        //connect to view model
        this.bleViewModel = new ViewModelProvider(this).get(BleOperationsViewModel.class);

        updateGui();

        //events
        this.scanResults.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            runOnUiThread(() -> {
                //we stop scanning
                scanLeDevice(false);
                //we connect
                bleViewModel.connect(((ScanResult) scanResultsAdapter.getItem(position)).getDevice());
            });
        });

        this.temperatureButton.setOnClickListener(view -> {
            if (this.bleViewModel.readTemperature()) {
                this.bleViewModel.temperatureChanged().observe(this, (temperature) -> updateTemperature());
            }
        });

        this.integerBtn.setOnClickListener(view -> {
            if(!integer.getText().toString().isEmpty()) {
                long value = Long.parseLong(integer.getText().toString());
                if (value >= 0 && this.bleViewModel.writeInteger((int) value)) {
                    Log.d(TAG, "written successfully");
                }
            }
        });

        this.timeBtn.setOnClickListener(view -> {
            LocalDateTime localDateTime = LocalDateTime.now();
            if (this.bleViewModel.writeCurrentTime(localDateTime)) {
                Log.d(TAG, "written successfully");
            }
        });

        //ble events
        this.bleViewModel.isConnected().observe(this, (isConnected) -> updateGui());
        this.bleViewModel.buttonClickedChanged().observe(this, (clickedButton) -> updateClickedButtons());
        this.bleViewModel.timeChanged().observe(this, (newTime) -> updateTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_menu, menu);
        //we link the two menu items
        this.scanMenuBtn = menu.findItem(R.id.menu_ble_search);
        this.disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect);
        //we update the gui
        updateGui();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_ble_search) {
            if (isScanning)
                scanLeDevice(false);
            else
                scanLeDevice(true);
            return true;
        } else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.isScanning)
            scanLeDevice(false);
        if (isFinishing())
            this.bleViewModel.disconnect();
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result list
     */
    private void updateGui() {
        Boolean isConnected = this.bleViewModel.isConnected().getValue();
        if (isConnected != null && isConnected) {
            this.scanPanel.setVisibility(View.GONE);
            this.operationPanel.setVisibility(View.VISIBLE);

            if (this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.scanMenuBtn.setVisible(false);
                this.disconnectMenuBtn.setVisible(true);
            }
        } else {
            this.operationPanel.setVisibility(View.GONE);
            this.scanPanel.setVisibility(View.VISIBLE);
            if (this.scanMenuBtn != null && this.disconnectMenuBtn != null) {
                this.disconnectMenuBtn.setVisible(false);
                this.scanMenuBtn.setVisible(true);
            }
        }
    }

    /**
     * Update the GUI with new temperature info
     */
    private void updateTemperature() {
        this.temperature.setText(String.format(
                "Temperature: %.1f°C",
                this.bleViewModel.temperatureChanged().getValue() / 10f)
        );
    }

    /**
     * Update the GUI with new number of buttons pressed
     */
    private void updateClickedButtons() {
        this.clickedButtons.setText(String.format(
                "Buttons clicked: %d times",
                this.bleViewModel.buttonClickedChanged().getValue()
        ));
    }

    /**
     * Update the GUI with new time
     */
    private void updateTime() {
        this.time.setText(String.format(
                "Peripheral time: %ta %<tb %<te  %<tY  %<tT%n",
                this.bleViewModel.timeChanged().getValue()
        ));
    }

    //this method needs user grant localisation and/or bluetooth permissions, our demo app is requesting them on MainActivity
    private void scanLeDevice(final boolean enable) {
        List<ScanFilter> filters = new ArrayList<>();
        final BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (enable) {

            //config
            ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            builderScanSettings.setReportDelay(0);

            //we scan for any BLE device
            //we don't filter them based on advertised services...
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")))
                    .build();
            filters.add(filter);

            //reset display
            scanResultsAdapter.clear();

            bluetoothScanner.startScan(filters, builderScanSettings.build(), leScanCallback);
            Log.d(TAG, "Start scanning...");
            isScanning = true;

            //we scan only for 15 seconds
            handler.postDelayed(() -> {
                scanLeDevice(false);
            }, 15 * 1000L);

        } else {
            bluetoothScanner.stopScan(leScanCallback);
            isScanning = false;
            Log.d(TAG, "Stop scanning (manual)");
        }
    }

    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            runOnUiThread(() -> {
                scanResultsAdapter.addDevice(result);
            });
        }
    };

}
