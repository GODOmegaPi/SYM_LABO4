package ch.heigvd.iict.sym_labo4.viewmodels;

import android.app.Application;
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

import no.nordicsemi.android.ble.BleManager;
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

    //Services and Characteristics of the SYM Pixl
    private BluetoothGattService timeService = null, symService = null;
    private BluetoothGattCharacteristic currentTimeChar = null, integerChar = null, temperatureChar = null, buttonClickChar = null;

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

                        return false; //FIXME si tout est OK, on retourne true, sinon la librairie appelera la méthode onDeviceDisconnected() avec le flag REASON_NOT_SUPPORTED
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

        public boolean readTemperature() {
            /*  TODO
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            return false; //FIXME
        }

    }

}
