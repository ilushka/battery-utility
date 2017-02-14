package com.moskovko.myfirstapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import android.bluetooth.BluetoothAdapter;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CODE_BT_ENABDLE    = 1;    // enabling bluetooth
    private static final int REQ_CODE_SELECT_DEVICE = 2;    // select bluetooth device

    private BatteryChargeView mBatteryCharge;   // battery charge bar
    private BatteryHealthView mBatterHealth;    // battery health bar
    private Random mRandomChargeGenerator;      // random values for charge & health
    private BluetoothAdapter mBtAdapter;        // bluetooth adapter
    private BluetoothDevice mBtDevice;          // bluetooth device
    private UartService mUartService;           // UART-over-bluetooth service

    // callbacks for service connect/disconnect
    private ServiceConnection mUartServiceConn =  new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mUartService = ((UartService.LocalBinder)service).getService();
            if (!mUartService.initialize()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Unable to initialize UART service",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mUartService = null;
        }
    };

    // broadcast event receiver for UART service
    private final BroadcastReceiver mUartBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connected to " + mBtDevice.getName(),
                                Toast.LENGTH_LONG).show();
                        // set button name to disconnect
                        Button b = (Button)findViewById(R.id.connect_disconnect_button);
                        b.setText(R.string.button_disconnect);
                    }
                });
            } else if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Disconnected from " + mBtDevice.getName(),
                                Toast.LENGTH_LONG).show();
                        mBtDevice = null;
                        // set button name to connect
                        Button b = (Button)findViewById(R.id.connect_disconnect_button);
                        b.setText(R.string.button_connect);
                    }
                });
            } else if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mUartService.enableTXNotification();
            } else if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Data available",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "UART is not supported",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }   // public void onReceive(Context context, Intent intent)
    };  // private final BroadcastReceiver mUartBroadcastReceiver = new BroadcastReceiver()

    private void initUartService() {
        // broadcast filter - what events does receiver want
        final IntentFilter filter = new IntentFilter();
        filter.addAction(UartService.ACTION_GATT_CONNECTED);
        filter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        filter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(UartService.ACTION_DATA_AVAILABLE);
        filter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);

        // bind to service
        bindService(new Intent(this, UartService.class), mUartServiceConn,
                Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUartBroadcastReceiver, filter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // initialization
        mBatteryCharge = (BatteryChargeView)findViewById(R.id.battery_charge);
        mBatterHealth = (BatteryHealthView)findViewById(R.id.battery_health);
        mRandomChargeGenerator = new Random();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtDevice = null;
        if (mBtAdapter == null) {
            Toast.makeText(this, "Cannot get default bluetooth adapter", Toast.LENGTH_LONG).show();
        }
        initUartService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_CODE_BT_ENABDLE:
                break;
            case REQ_CODE_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // connect to device at specified address
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBtDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    mUartService.connect(deviceAddress);
                }
                break;
            default:
                break;
        }
    }

    /*
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText)findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
    */

    public void startAnimation(View view) {
        /*
        ValueAnimator animation = ValueAnimator.ofFloat(mBatteryCharge.getCurrentChargeLevel(),
                        mRandomChargeGenerator.nextFloat());
        animation.setDuration(1500);
        animation.addUpdateListener(mBatteryCharge);
        animation.start();
        */
        mBatteryCharge.setCurrentChargeLevel(mRandomChargeGenerator.nextFloat());
        mBatterHealth.setCurrentHealthLevel(mRandomChargeGenerator.nextFloat());
    }

    public void connectDisconnect(View view) {
        if (!mBtAdapter.isEnabled()) {
            // bluetooth not enabled, prompt user to enable it
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQ_CODE_BT_ENABDLE);
        } else {
            // check if button says disconnect or connect
            Button b = (Button)findViewById(R.id.connect_disconnect_button);
            if (b.getText().equals(getResources().getString(R.string.button_connect))) {
                // button is a connect button - open connection list
                startActivityForResult(new Intent(MainActivity.this, DeviceListActivity.class),
                        REQ_CODE_SELECT_DEVICE);
            } else {
                // button is a disconnect button
                if (mBtDevice != null) {
                    mUartService.disconnect();
                }
            }
        }
    }

    public void startStopLoopback(View view) {
        try {
            String data = "MONKEY";
            mUartService.writeRXCharacteristic(data.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
