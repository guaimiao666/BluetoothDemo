package com.example.bluetoothdemo;

import android.annotation.SuppressLint;
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
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.UUID;


@SuppressLint("MissingPermission")
public class BleConnector {

    private final String TAG = getClass().getSimpleName();

    private static BleConnector bleConnector;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private ScanCallback scanCallback;
    private BluetoothGatt bluetoothGatt;
    private boolean isConnected;
    private String uuid;
    private String deviceName;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public static BleConnector getInstance(Context context) {
        if (bleConnector == null) {
            bleConnector = new BleConnector(context);
        }
        return bleConnector;
    }

    public BleConnector(Context context) {
        this.context = context;
    }

    public void init() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            mScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        initScanCallback();
    }

    /**
     * 初始化蓝牙扫描回调
     */
    private void initScanCallback() {
        if (scanCallback != null) {
            return;
        }
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.i(TAG, "onScanResult");
                BluetoothDevice device = result.getDevice();
                Log.i(TAG, "deviceName:" + device.getName());
                Log.i(TAG, "deviceAddr:" + device.getAddress());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.i(TAG, "onBatchScanResults");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.i(TAG, "onScanFailed errorCode:" + errorCode);
            }
        };
    }

    //开始扫描蓝牙
    public void startScan() {
        if (mScanner != null && scanCallback != null) {
            mScanner.startScan(scanCallback);
            Log.i(TAG, "startScan");
        }
    }

    //停止扫描蓝牙
    public void stopScan() {
        if (mScanner != null && scanCallback != null) {
            mScanner.stopScan(scanCallback);
            Log.i(TAG, "stopScan");
        }
    }

    //断开连接
    private void disConnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            Log.i(TAG, "disConnectGatt");
        }
    }

    //连接蓝牙(客户端)
    private void connectGatt(BluetoothDevice device) {
        stopScan();
        disConnectGatt();
        bluetoothGatt = device.connectGatt(context, false, callback);
    }

    /**
     * 连接和读写数据回调
     */
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, String.format("onConnectionStateChange %s%s%s%s",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //启用发现服务
                Log.i(TAG, "discoverServices enable");
            } else {
                isConnected = false;
                stopScan();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, String.format("onCharacteristicRead %s%s%s%s",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            Log.i(TAG, "value:" + characteristic.getValue().toString());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, String.format("onCharacteristicWrite %s%s%s%s",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            Log.i(TAG, "value:" + characteristic.getValue().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, String.format("onCharacteristicChanged %s%s%s%s",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress()));
            Log.i(TAG, "value:" + characteristic.getValue().toString());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //可以发现服务回调

        }
    };

    /**
     * 根据UUID获取对应服务
     * @return
     */
    private BluetoothGattService getGattService() {
        BluetoothGattService service = null;
        if (bluetoothGatt != null && !TextUtils.isEmpty(uuid)) {
            service = bluetoothGatt.getService(UUID.fromString(uuid));
        }
        if (service == null) {
            Log.i(TAG, "无法获取对应uuid的服务  " + uuid);
        }
        return service;
    }

    /**
     * 根据服务和UUID获取特性
     * @param service
     * @return
     */
    private BluetoothGattCharacteristic getGattCharacteristic(BluetoothGattService service) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        return service.getCharacteristic(UUID.fromString(uuid));
    }
}
