package com.example.airduino;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 456;
    private static final int REQUEST_ENABLE_LOCATION = 457;
    private static final String TAG = "TAG_ANDROID";

    private Button btn_start, btn_cancel;
    private RecyclerView recyclerView;
    private DevicesAdapter devicesAdapter;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayList<String> adrMacList = new ArrayList<>();

    private TextView tv_device_connected;
    private Button btn_disconnect;
    private TextView tv_information_device_connected;
    private LinearLayout ll_device_connected;

    private String nameDeviceConnected = "", adrMAC = "";
    private BluetoothGatt bluetoothGatt = null;
    private Boolean isConnected = false;
    private Background background = new Background();
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread thread = new Thread(background);

    public class Background implements Runnable {
        @Override
        public void run() {}

        public void reset(String name, String adr, String services) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("nameDeviceConnected", name);
            bundle.putString("adresseMAC", adr);
            bundle.putString("services", services);

            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String name = msg.getData().getString("nameDeviceConnected");
            String adr = msg.getData().getString("adresseMAC");
            String services = msg.getData().getString("services");

            tv_device_connected.setText(name + " (" + adr + ")");
            tv_information_device_connected.setText(services);
        }
    };

    private final ScanCallback btle = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            // On récupère le périphérique bluetooth détecté durant le scan
            BluetoothDevice device = result.getDevice();

            String name = device.getName();
            String adr = device.getAddress(); // Mac address

            if (adrMacList.contains(adr)) {
                return;
            }

            adrMacList.add(adr);

            devicesAdapter.ajoute(R.drawable.bluetooth, name, adr);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server");
                gatt.discoverServices(); // Appel la méthode redéfinie onServicesDiscovered
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered");

                String serv = "";
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.i(TAG, "New service : " + service.getUuid().toString());
                    String nameServ = Sample_gatt_attributes.lookup(service.getUuid().toString(), "");
                    serv += "New service : " + nameServ;
                    serv += "\n";

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.i(TAG, "   New characteristic : " + characteristic.getUuid().toString());
                        String nameCharact = Sample_gatt_attributes.lookup(characteristic.getUuid().toString(), "");

                        if (nameCharact.equals("HM-10 characteristic")) {
                            gatt.setCharacteristicNotification(characteristic, true);
                            Log.i("CHAR", nameCharact);
                        }

                        serv += "    New characteristic : " + nameCharact;
                        serv += "\n";
                    }
                }
                background.reset(nameDeviceConnected, adrMAC, serv);
            } else {
                Log.i(TAG, "Services not discovered");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("CHAR","onCharacteristicChanged");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // On cherche à récupérer l'interface bluetooth du périphérique Android
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Si pas de module (d'interface) bluetooth sur le périphérique ...
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();
            return;
        }

        // Si le bluetooth n'est pas activé, on propose de l'activer
        if (!bluetoothAdapter.isEnabled()) {
            // Demande à activer l'interface bluetooth
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

        // On vérifie les autorisations pour la localisation
        // Cette permission est requise sur les versions récentes d'Android
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_ENABLE_LOCATION);
        }

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        recyclerView = (RecyclerView)findViewById(R.id.rv_devices);
        recyclerView.setLayoutManager( new LinearLayoutManager(this));
        devicesAdapter = new DevicesAdapter(this);
        recyclerView.setAdapter(devicesAdapter);

        btn_start = (Button)findViewById(R.id.btn_start);
        btn_cancel = (Button)findViewById(R.id.btn_cancel);
        tv_device_connected = (TextView)findViewById(R.id.tv_device_connected);
        btn_disconnect = (Button)findViewById(R.id.btn_disconnect);
        tv_information_device_connected = (TextView)findViewById(R.id.tv_information_device_connected);
        ll_device_connected = (LinearLayout)findViewById(R.id.ll_device_connected);

        ItemClickSupport.addTo(recyclerView, R.layout.activity_main).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                DevicesAdapter.DeviceItem item = devicesAdapter.getItem(position);

                nameDeviceConnected = item.name;
                adrMAC = item.adr;

                if (isConnected) {
                    return;
                }

                isConnected = connect(adrMAC);

                if (isConnected) {
                    ll_device_connected.setVisibility(LinearLayout.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        btn_start.setEnabled(false);
        btn_cancel.setEnabled(true);

        scan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.cancelDiscovery();
        isRunning.set(false);
    }

    public void onStartScan(View view) {
        Toast msg = Toast.makeText(this, R.string.btn_start_action, Toast.LENGTH_LONG);
        msg.show();

        btn_start.setEnabled(false);
        btn_cancel.setEnabled(true);

        scan();
    }

    public void onCancelScan(View view) {
        Toast msg = Toast.makeText(this, R.string.btn_cancel_action, Toast.LENGTH_LONG);
        msg.show();

        btn_start.setEnabled(true);
        btn_cancel.setEnabled(false);

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.stopScan(btle);

        // On arrête le scan bluetooth
        bluetoothAdapter.cancelDiscovery();
    }

    public void scan() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(btle);

        // Si un scan bluetooth est en cours, on le coupe
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // On lance un nouveau scan bluetooth
        bluetoothAdapter.startDiscovery();
    }

    public void onDisconnect(View view) {
        Toast.makeText(this, R.string.disconnected_device, Toast.LENGTH_LONG).show();

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        bluetoothGatt.disconnect();
        isConnected = false;
        ll_device_connected.setVisibility(LinearLayout.GONE);
    }

    public boolean connect(String address) {
        // Le Bluetooth Adapter n'est pas initialisé || pas d'adresse MAC
        if (bluetoothAdapter == null || address == null) {
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(adrMAC);

        // vérifie si le device existe
        if (device == null) {
            Toast.makeText(this, R.string.not_found_device, Toast.LENGTH_LONG).show();
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);

        if(bluetoothGatt.connect()) {
            Toast.makeText(this, R.string.connected_device, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.connection_impossible, Toast.LENGTH_LONG).show();
        }

        btn_disconnect.setEnabled(true);

        if (!isRunning.get()) {
            thread.start();
            isRunning.set(true);
        }

        background.reset(nameDeviceConnected, adrMAC, "");

        return true;
    }
}
