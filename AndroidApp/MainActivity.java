
package com.example.seniorproject;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {


    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();

    // Variables to access objects
    private static Button start_button;
    private static Button search_button;
    private static Button connect_button;
    private static Button discover_button;
    private static Button disconnect_button;
    private static Switch led_switch;
    private static SeekBar seekBarWC;
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static BluetoothComm mBluetoothComm;
    private static final int REQUEST_ENABLE_BLE = 1;


    //service initialization
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothComm = ((BluetoothComm.LocalBinder) service).getService();
            mServiceConnected = true;
            mBluetoothComm.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mBluetoothComm = null;
        }
    };

//creation of first activity
    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //interactables
        start_button = (Button) findViewById(R.id.start_button);
        search_button = (Button) findViewById(R.id.search_button);
        connect_button = (Button) findViewById(R.id.connect_button);
        discover_button = (Button) findViewById(R.id.discoverSvc_button);
        disconnect_button = (Button) findViewById(R.id.disconnect_button);
        led_switch = (Switch) findViewById(R.id.led_switch);
        seekBarWC = (SeekBar) findViewById(R.id.seekBarWC);

        // Initialize service and connection state variable
        mServiceConnected = false;
        mConnectState = false;


        /* This will be called when the LED On/Off switch is touched */
        led_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Turn the LED on or OFF based on the state of the switch
                mBluetoothComm.writeLedCharacteristic(isChecked);
            }
        });

        seekBarWC.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int PWMValue = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PWMValue = progress;
                if(PWMValue > 0){
                    mBluetoothComm.writeLedCharacteristic(fromUser);
                } else {
                    mBluetoothComm.writeLedCharacteristic(fromUser);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothComm.ACTION_BLESCAN_CALLBACK);
        filter.addAction(BluetoothComm.ACTION_CONNECTED);
        filter.addAction(BluetoothComm.ACTION_DISCONNECTED);
        filter.addAction(BluetoothComm.ACTION_SERVICES_DISCOVERED);
        filter.addAction(BluetoothComm.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothComm.close();
        unbindService(mServiceConnection);
        mBluetoothComm = null;
        mServiceConnected = false;
    }

    //start bluetooth
    public void startBluetooth(View view) {

        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        //ensures bluetooth is on target device
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, BluetoothComm.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Disable the start button and turn on the search  button
        start_button.setEnabled(false);
        search_button.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");
    }

    //search button method
    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mBluetoothComm.scan();
        }

    }

    //connect to device button
    public void connectBluetooth(View view) {
        mBluetoothComm.connect();

    }

    //discover services button
    public void discoverServices(View view) {
        /* This will discover both services and characteristics */
        mBluetoothComm.discoverServices();

    }

    //disconnect button
    public void Disconnect(View view) {
        mBluetoothComm.disconnect();

    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothComm.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    search_button.setEnabled(false);
                    connect_button.setEnabled(true);
                    break;

                case BluetoothComm.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Capsense notifications */
                    if (!mConnectState) {
                        // Dsable the connect button, enable the discover services and disconnect buttons
                        connect_button.setEnabled(false);
                        discover_button.setEnabled(true);
                        disconnect_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected to Device");
                    }
                    break;
                case BluetoothComm.ACTION_DISCONNECTED:
                    // Disable the disconnect, discover svc, discover char button, and enable the search button
                    disconnect_button.setEnabled(false);
                    discover_button.setEnabled(false);
                    search_button.setEnabled(true);
                    // Turn off and disable the LED and CapSense switches
                    led_switch.setChecked(false);
                    led_switch.setEnabled(false);
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;
                case BluetoothComm.ACTION_SERVICES_DISCOVERED:
                    // Disable the discover services button
                    discover_button.setEnabled(false);
                    // Enable the LED and CapSense switches
                    led_switch.setEnabled(true);
                    Log.d(TAG, "Services Discovered");
                    break;
                case BluetoothComm.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    // Check LED switch Setting
                    if(mBluetoothComm.getLedSwitchState()){
                        led_switch.setChecked(true);
                    } else {
                        led_switch.setChecked(false);
                    }
                default:
                    break;
            }
        }
    };
}
