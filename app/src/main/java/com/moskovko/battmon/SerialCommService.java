package com.moskovko.battmon;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.UUID;

public class SerialCommService extends Service {
    private final static String TAG = SerialCommService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private byte[] mWriteData = null;       // data to write/transmitted
    private int mWriteDataOffset = 0;       // offsets already written data in the write buffer
    private int mWritePendingCount = 0;     // write data that is currently being transmitted
    // data that is read/received
    private ByteArrayOutputStream mReadData = new ByteArrayOutputStream();
    private boolean mReceivingFrame = false;    // in process of receiving SerialComm frame

    private static final int CHARACTERISTIC_MAX_BYTE_COUNT  = 20;

    private static final char[] hexDigits = "0123456789ABCDEF".toCharArray();

    public final static String ACTION_GATT_CONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String EXTRA_DATA =
            "com.nordicsemi.nrfUART.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_SERIALCOMM =
            "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_SERIALCOMM";
    public final static String ACTION_FRAME_AVAILABLE =
            "com.nordicsemi.nrfUART.ACTION_FRAME_AVAILABLE";
    
    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final byte FRAME_START    = ':';
    public static final byte FRAME_END      = '\n';

   
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);

                enableTXNotification();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // returns true if byte is a valid hex digit (encoded in ASCII)
        private boolean isHexDigit(byte b) {
            if (    (b >= 'A' && b <= 'F') ||
                    (b >= 'a' && b <= 'f') ||
                    (b >= '0' && b <= '9')) {
                return true;
            }
            return false;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            for (byte b : data) {
                if (b == FRAME_START) {
                    mReadData.reset();
                    mReceivingFrame = true;
                }
                if (mReceivingFrame) {
                    if (isHexDigit(b)) {
                        mReadData.write(b);
                    } else {
                        // TODO: what is it? what to do?
                    }
                }
                // TODO: check for max frame size
                if (b == FRAME_END) {
                    mReceivingFrame = false;
                    // MONKEY:
                    Log.w(TAG, "received serialcomm frame: " + mReadData.toString());
                    // broadcast data
                    broadcastUpdate(ACTION_FRAME_AVAILABLE, mReadData.toByteArray());
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.w(TAG, "onCharacteristicWrite status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // successfully wrote a packet - offset written data in the write data buffer
                mWriteDataOffset += mWritePendingCount;
                if (mWriteDataOffset >= mWriteData.length) {
                    // wrote all data
                } else {
                    // write next packet
                    initiateWrite(getNextWritePacket());
                }
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final byte[] data) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        SerialCommService getService() {
            return SerialCommService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
       // mBluetoothGatt.close();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.w(TAG, "mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void enableTXNotification()
    {
    	BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
    	if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_SERIALCOMM);
            return;
        }
    	BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_SERIALCOMM);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        
        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    	
    }

    // convert byte array to ASCII hex string representing the byte array
    // http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    public static byte[] convertByteArrayToHex(byte[] data) {
        byte[] hexData = new byte[data.length * 2];
        for (int ii = 0; ii < data.length; ii++) {
            hexData[ii * 2] = (byte)hexDigits[(data[ii] >>> 4) & 0x0F];
            hexData[ii * 2 + 1] = (byte)hexDigits[data[ii] & 0x0F];
        }
        return hexData;
    }

    // convert ASCII hex string to byte array
    public static byte[] convertHexStringToByteArray(String str) {
        byte[] data = new byte[str.length() / 2];
        for (int ii = 0; ii < str.length(); ii += 2) {
            data[ii / 2] = (byte)(((Character.digit(str.charAt(ii), 16) << 4) & 0xF0) |
                    (Character.digit(str.charAt(ii + 1), 16) & 0x0F));
        }
        return data;
    }

    // kick off BLE write
    private void initiateWrite(byte[] value) {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        showMessage("mBluetoothGatt null"+ mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_SERIALCOMM);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_SERIALCOMM);
            return;
        }
        RxChar.setValue(value);
        mWritePendingCount = value.length;
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

        Log.d(TAG, "write TXchar - status=" + status);
    }

    // return a packet of write data of maximum size of 20 bytes
    private byte[] getNextWritePacket() {
        if (mWriteData.length <= CHARACTERISTIC_MAX_BYTE_COUNT) {
            // return all data
            return mWriteData;
        }
        // get maximum of 20 byte slice of data
        int sliceSize = Math.min(CHARACTERISTIC_MAX_BYTE_COUNT,
                (mWriteData.length - mWriteDataOffset));
        return Arrays.copyOfRange(mWriteData, mWriteDataOffset, (mWriteDataOffset + sliceSize));
    }

    // write data to characteristic
    public void writeData(byte[] data)
    {
        byte[] beginning = { FRAME_START };
        byte[] ending = { FRAME_END };
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // convert data to ASCII hex, add frame start and frame end characters
        byte[] asciiHex = convertByteArrayToHex(data);
        try {
            output.write(beginning);
            output.write(asciiHex);
            output.write(ending);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        mWriteData = output.toByteArray();
        mWritePendingCount = 0;
        mWriteDataOffset = 0;

        // send first packet
        initiateWrite(getNextWritePacket());
    }
    
    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }
}
