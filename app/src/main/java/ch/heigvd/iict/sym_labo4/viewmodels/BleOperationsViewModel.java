package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by Guillaume Laubscher, Ilias Goujgali, Eric Bousbaa on 30.01.2022
 * (C) 2019 - HEIG-VD, IICT
 */
public class BleOperationsViewModel extends AndroidViewModel {

    private static final String TAG = BleOperationsViewModel.class.getSimpleName();

    private SYMBleManager ble = null;
    private BluetoothGatt mConnection = null;

    //live data - observer
    private final MutableLiveData<Boolean> mIsConnected = new MutableLiveData<>(false);
    public LiveData<Boolean> isConnected() {
        return mIsConnected;
    }

    private final MutableLiveData<Integer> mTemperature = new MutableLiveData<>(0);
    public LiveData<Integer> temperatureChanged() { return mTemperature; }

    private final MutableLiveData<Integer> mButtonClicked = new MutableLiveData<>(0);
    public LiveData<Integer> buttonClickedChanged() { return mButtonClicked; }

    private final MutableLiveData<LocalDateTime> mTime = new MutableLiveData<>();
    public LiveData<LocalDateTime> timeChanged() { return mTime; }

    //Services and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

    // Known UUID from services and characteristics
    private final String timeServiceShortUUID = "1805";
    private final String[] timeServiceCharacteristicShortUUID = {"2a2b"};
    private final String symServiceShort = "1000";
    private final String[] symServiceCharacteristicShortUUID = {"1001", "1002", "1003"};

    public BleOperationsViewModel(Application application) {
        super(application);
        this.ble = new SYMBleManager(application.getApplicationContext());
        this.ble.setConnectionObserver(this.bleConnectionObserver);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared");
        this.ble.disconnect();
    }

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "User request connection to: " + device);
        if(!mIsConnected.getValue()) {
            this.ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue();
        }
    }

    public void disconnect() {
        Log.d(TAG, "User request disconnection");
        this.ble.disconnect();
        if(mConnection != null) {
            mConnection.disconnect();
        }
    }

    public boolean readTemperature() {
        if(!isConnected().getValue() || temperatureChar == null) return false;
        return ble.readTemperature();
    }

    public boolean writeInteger(int value) {
        if(!isConnected().getValue() || integerChar == null) return false;
        return ble.writeInteger(value);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean writeCurrentTime(LocalDateTime value) {
        if(!isConnected().getValue() || currentTimeChar == null) return false;
        return ble.writeCurrentTime(value);
    }

    private final ConnectionObserver bleConnectionObserver = new ConnectionObserver() {
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnected");
            mIsConnected.setValue(true);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceDisconnecting");
            mIsConnected.setValue(false);
        }

        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceReady");
        }

        @Override
        public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
            Log.d(TAG, "onDeviceFailedToConnect");
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
            if(reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported");
                Toast.makeText(getApplication(), "Device not supported - implement method isRequiredServiceSupported()", Toast.LENGTH_LONG).show();
            }
            else
                Log.d(TAG, "onDeviceDisconnected");

            mIsConnected.setValue(false);
        }
    };

    private class SYMBleManager extends BleManager {

        private SYMBleManager(Context applicationContext) {
            super(applicationContext);
        }

        /**
         * BluetoothGatt callbacks object.
         */
        private BleManagerGattCallback mGattCallback = null;
        @Override
        @NonNull
        public BleManagerGattCallback getGattCallback() {
            //we initiate the mGattCallback on first call
            if(mGattCallback == null) {
                this.mGattCallback = new BleManagerGattCallback() {

                    @Override
                    public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
                        mConnection = gatt; //trick to force disconnection

                        // Initiate time service
                        boolean hasTimeService = hasServiceAndCharacteristics(gatt, timeServiceShortUUID, timeServiceCharacteristicShortUUID, false);
                        if (hasTimeService) {
                            timeService = gatt.getService(UUID.fromString(getLongUUIDStandard(timeServiceShortUUID)));
                            currentTimeChar = timeService.getCharacteristic(UUID.fromString(getLongUUIDStandard(timeServiceCharacteristicShortUUID[0])));
                        }

                        // Initiate custom SYM service
                        boolean hasSymService = hasServiceAndCharacteristics(gatt, symServiceShort, symServiceCharacteristicShortUUID, true);
                        if (hasSymService) {
                            symService = gatt.getService(UUID.fromString(getLongUUIDSYM(symServiceShort)));
                            integerChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[0])));
                            temperatureChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[1])));
                            buttonClickChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[2])));
                        }

                        return hasTimeService && hasSymService;
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    protected void initialize() {
                        setNotificationCallback(buttonClickChar).with(
                                (BluetoothDevice b, Data d) ->
                                        mButtonClicked.setValue(d.getIntValue(Data.FORMAT_UINT8, 0))
                        );
                        setNotificationCallback(currentTimeChar).with(
                                (BluetoothDevice b, Data d) ->
                                        readCurrentTime(d)
                        );

                        beginAtomicRequestQueue()
                                .add(enableNotifications(buttonClickChar)
                                        .fail((device, status) -> {
                                            Log.e(TAG, "Could not subscribe: " + status);
                                        }))
                                .done(device -> {
                                    Log.d(TAG, "Subscribed!");
                                })
                                .enqueue();
                        beginAtomicRequestQueue()
                                .add(enableNotifications(currentTimeChar)
                                        .fail((device, status) -> {
                                            Log.e(TAG, "Could not subscribe: " + status);
                                        }))
                                .done(device -> {
                                    Log.d(TAG, "Subscribed!");
                                })
                                .enqueue();
                    }

                    @Override
                    protected void onServicesInvalidated() {
                        //we reset services and characteristics
                        timeService = null;
                        currentTimeChar = null;

                        symService = null;
                        integerChar = null;
                        temperatureChar = null;
                        buttonClickChar = null;
                    }
                };
            }
            return mGattCallback;
        }

        /**
         * Transform short UUID to long UUID
         * @param shortUUID the short UUID to be transformed
         * @return the long UUID from the short one
         */
        private String getLongUUIDStandard(String shortUUID) {
            return String.format("0000%s-0000-1000-8000-00805f9b34fb", shortUUID);
        }

        /**
         * Transform short UUID to long UUID (specific to SYM service)
         * @param shortUUID the short UUID to be transformed
         * @return the long UUID from the short one
         */
        private String getLongUUIDSYM(String shortUUID) {
            return String.format("3c0a%s-281d-4b48-b2a7-f15579a1c38f", shortUUID);
        }

        /**
         * Check if the service and his characteristics exist
         * @param services Gatt service
         * @param shortServiceUUID UUID of the service to be checked
         * @param shortCharacteristicsUUID UUIDs of the characteristics related to the service to be checked
         * @param symService if it's a custom SYM service or not
         * @return if everything was find
         */
        private boolean hasServiceAndCharacteristics(BluetoothGatt services, String shortServiceUUID, String[] shortCharacteristicsUUID, boolean symService) {
            boolean hasService = false;
            for (BluetoothGattService s : services.getServices()) {
                if(s.getUuid().toString().equals(symService ?
                        getLongUUIDSYM(shortServiceUUID) :
                        getLongUUIDStandard(shortServiceUUID))) {
                    hasService = true;
                    for (String shortCharacteristicUUID : shortCharacteristicsUUID) {
                        boolean hasCharacteristic = false;
                        for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                            if (c.getUuid().toString().equals(symService ?
                                    getLongUUIDSYM(shortCharacteristicUUID) :
                                    getLongUUIDStandard(shortCharacteristicUUID))) {
                                hasCharacteristic = true;
                            }
                        }
                        if (!hasCharacteristic) {
                            return false;
                        }
                    }
                }
            }
            return hasService;
        }

        public boolean readTemperature() {
            readCharacteristic(temperatureChar).with(
                    (BluetoothDevice b, Data d) ->
                        mTemperature.setValue(d.getIntValue(Data.FORMAT_UINT16, 0))
            ).enqueue();
            return true;
        }

        public boolean writeInteger(int value) {
            // Transform int 32 bit BE to int 32 bits LE
            ByteBuffer bb = ByteBuffer.wrap(new byte[]{(byte) value});
            bb.order(ByteOrder.LITTLE_ENDIAN);

            Data data = new Data(bb.array());
            writeCharacteristic(integerChar, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE).enqueue();
            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void readCurrentTime(Data d) {
            int year = 0, month = 0, day = 0, hour = 0, minutes = 0,
                    seconds = 0, dayOfWeek = 0, fractions256 = 0, raison = 0;

            // Here we only use what the constructor of LocalDateTime can take as arguments
            try {
                year = d.getIntValue(Data.FORMAT_UINT16, 0);
                month = d.getIntValue(Data.FORMAT_UINT8, 2);
                day = d.getIntValue(Data.FORMAT_UINT8, 3);
                hour = d.getIntValue(Data.FORMAT_UINT8, 4);
                minutes = d.getIntValue(Data.FORMAT_UINT8, 5);
                seconds = d.getIntValue(Data.FORMAT_UINT8, 6);
                dayOfWeek = d.getIntValue(Data.FORMAT_UINT8, 7);
                fractions256 = d.getIntValue(Data.FORMAT_UINT8, 8);
                raison = d.getIntValue(Data.FORMAT_UINT8, 9);
            } catch (NullPointerException e) {
                Log.d(TAG, e.getMessage());
            }

            mTime.setValue(LocalDateTime.of(year, Month.of(month), day, hour, minutes, seconds));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public boolean writeCurrentTime(LocalDateTime localDateTime) {
            // Transform the year value to a string of bits
            String year = Integer.toBinaryString(localDateTime.getYear());

            // Take the lower parts of the bits
            int lowerBits;
            if(year.length() <= 8)
                lowerBits = Integer.parseInt(year, 2);
            else
                lowerBits = Integer.parseInt(year.substring(year.length() - 8), 2);

            // Take the upper part of the bits
            int upperBits = 0;
            if(year.length() > 8)
                upperBits = Integer.parseInt(year.substring(0, year.length() - 8), 2);

            // Calculate the fraction number
            int fractions256 = (int)
                    (localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    * 256);

            Data newTime = new Data(new byte[]{
                    (byte) lowerBits,
                    (byte) upperBits,
                    (byte) localDateTime.getMonthValue(),
                    (byte) localDateTime.getDayOfMonth(),
                    (byte) localDateTime.getHour(),
                    (byte) localDateTime.getMinute(),
                    (byte) localDateTime.getSecond(),
                    (byte) localDateTime.getDayOfWeek().getValue(),
                    (byte) fractions256,
                    (byte) 0x03,
            });

            writeCharacteristic(currentTimeChar, newTime, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE).enqueue();
            return true;
        }
    }

}
