package com.moskovko.battmon;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.util.Random;
//import java.util.logging.Handler;
import android.os.Handler;
import java.util.logging.LogRecord;

import android.bluetooth.BluetoothAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQ_CODE_BT_ENABDLE    = 1;    // enabling bluetooth
    private static final int REQ_CODE_SELECT_DEVICE = 2;    // select bluetooth device

    private static final int JOB_PERIOD_MS          = 5000; // periodic job execution period in ms

    private BatteryChargeView mBatteryCharge;   // battery charge bar
    private Random mRandomDataGenerator;        // random values for charge & health
    private BluetoothAdapter mBtAdapter;        // bluetooth adapter
    private BluetoothDevice mBtDevice;          // bluetooth device
    private SerialCommService mSerialCommService;       // SerialComm service
    private byte[] mLoopbackRequest;                    // data sent during loopback
    private ByteArrayOutputStream mLoopbackResponse;    // data received during loopback
    private TextView mLoopbackStatus;           // loopback status text
    private EditText mInputData;                // text field for SerialComm command
    private Runnable mJob;                      // gets executed periodically
    private Handler mJobHandler;                // UI thread handler for periodic job

    // callbacks for service connect/disconnect
    private ServiceConnection mSerialCommServiceConn =  new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSerialCommService = ((SerialCommService.LocalBinder)service).getService();
            if (!mSerialCommService.initialize()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Unable to initialize SerialComm service",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSerialCommService = null;
        }
    };

    // broadcast event receiver for SerialComm service
    private final BroadcastReceiver mUartBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SerialCommService.ACTION_GATT_CONNECTED)) {
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

            } else if (action.equals(SerialCommService.ACTION_GATT_DISCONNECTED)) {
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

            } else if (action.equals(SerialCommService.ACTION_GATT_SERVICES_DISCOVERED)) {

            } else if (action.equals(SerialCommService.ACTION_FRAME_AVAILABLE)) {
                final byte[] data = intent.getByteArrayExtra(SerialCommService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLoopbackStatus.setText(new String(data));
                    }
                });
            } else if (action.equals(SerialCommService.DEVICE_DOES_NOT_SUPPORT_SERIALCOMM)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "SerialComm is not supported",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }   // public void onReceive(Context context, Intent intent)
    };  // private final BroadcastReceiver mUartBroadcastReceiver = new BroadcastReceiver()

    private void initUartService() {
        // broadcast filter - what events does receiver want
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SerialCommService.ACTION_GATT_CONNECTED);
        filter.addAction(SerialCommService.ACTION_GATT_DISCONNECTED);
        filter.addAction(SerialCommService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(SerialCommService.DEVICE_DOES_NOT_SUPPORT_SERIALCOMM);
        filter.addAction(SerialCommService.ACTION_FRAME_AVAILABLE);

        // bind to service
        bindService(new Intent(this, SerialCommService.class), mSerialCommServiceConn,
                Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUartBroadcastReceiver, filter);
    }

    private void startPeriodicJob() {
        mJobHandler.postDelayed(mJob, JOB_PERIOD_MS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // initialization

        mBatteryCharge = (BatteryChargeView)findViewById(R.id.battery_charge);
        // MONKEY: mBatterHealth = (BatteryHealthView)findViewById(R.id.battery_health);
        mRandomDataGenerator = new Random();

        // init bluetooth and uart over ble service
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtDevice = null;
        mLoopbackRequest = null;
        mLoopbackResponse = new ByteArrayOutputStream();
        mLoopbackStatus = (TextView)findViewById(R.id.loopback_status);
        // MONKEY: mInputData = (EditText)findViewById(R.id.input_data);
        if (mBtAdapter == null) {
            Toast.makeText(this, "Cannot get default bluetooth adapter", Toast.LENGTH_LONG).show();
        }
        initUartService();

        mJobHandler = new Handler(Looper.myLooper());
        mJob = new Runnable() {
            @Override
            public void run() {
                startAnimation();
                mJobHandler.postDelayed(mJob, JOB_PERIOD_MS);
            }
        };
        startPeriodicJob();
    }

    private void sendRequest() {
        byte[] request = SerialCommService.convertHexStringToByteArray(mInputData.getText().toString());
        mLoopbackStatus.setText("none");
        mSerialCommService.writeData(request);
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
                    mSerialCommService.connect(deviceAddress);
                }
                break;
            default:
                break;
        }
    }

    public void startAnimation() {
        mBatteryCharge.setCurrentChargeLevel(mRandomDataGenerator.nextFloat());
        // MONKEY: mBatterHealth.setCurrentHealthLevel(mRandomDataGenerator.nextFloat());
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
                    mSerialCommService.disconnect();
                }
            }
        }
    }

    public void startStopLoopback(View view) {
        sendRequest();
    }
}
