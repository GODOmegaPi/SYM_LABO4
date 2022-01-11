package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 19.10.2021
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

    //Services and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

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

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */

    // FIXME I have no idea on how to use this
    public boolean readTemperature() {
        if(!isConnected().getValue() || temperatureChar == null) return false;
        return ble.readTemperature();
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
                        Log.d(TAG, "isRequiredServiceSupported - TODO");

                        /* TODO
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 40 et 41)
                        */

                        boolean hasTimeService = hasServiceAndCharacteristics(gatt, timeServiceShortUUID, timeServiceCharacteristicShortUUID, false);
                        if (hasTimeService) {
                            timeService = gatt.getService(UUID.fromString(getLongUUIDStandard(timeServiceShortUUID)));
                            currentTimeChar = timeService.getCharacteristic(UUID.fromString(getLongUUIDStandard(timeServiceCharacteristicShortUUID[0])));
                        }
                        boolean hasSymService = hasServiceAndCharacteristics(gatt, symServiceShort, symServiceCharacteristicShortUUID, true);
                        if (hasSymService) {
                            symService = gatt.getService(UUID.fromString(getLongUUIDSYM(symServiceShort)));
                            integerChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[0])));
                            temperatureChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[1])));
                            buttonClickChar = symService.getCharacteristic(UUID.fromString(getLongUUIDSYM(symServiceCharacteristicShortUUID[2])));
                        }

                        return hasTimeService && hasSymService;
                    }

                    @Override
                    protected void initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */
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

        private String getLongUUIDStandard(String shortUUID) {
            return String.format("0000%s-0000-1000-8000-00805f9b34fb", shortUUID);
        }

        private String getLongUUIDSYM(String shortUUID) {
            return String.format("3c0a%s-281d-4b48-b2a7-f15579a1c38f", shortUUID);
        }

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
            /*  TODO
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            return true; //FIXME
        }

    }

}
