package com.smartmeeting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.support.annotation.Nullable;
import android.widget.Toast;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeaconModule extends ReactContextBaseJavaModule {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";
    public static final String MANUFACTURE_UUID = "0CF052C2-97CA-407C-84F8-B62AAC4E9020";
    public static final int MANUFACTURE_ID = 224;
    private boolean isListening;
    private boolean isBroadcasting;
    private AdvertiseSettings mAdvertiseSettings;
    private AdvertiseData mAdvertiseData;
    private ScanSettings mScanSettings;
    private ScanFilter mScanFilter;
    BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private ScanCallback mScanCallback;

    public BeaconModule(ReactApplicationContext reactContext) {
        super(reactContext);
        prepareCallback(reactContext);
    }

    private void prepareCallback(final ReactApplicationContext reactContext) {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord mScanRecord = result.getScanRecord();
                byte[] manufacturerData = mScanRecord.getManufacturerSpecificData(MANUFACTURE_ID);
                int mRssi = result.getRssi();
                double distance = calculateDistance(manufacturerData[22], mRssi);
                WritableMap params = Arguments.createMap();
                params.putDouble("distance", distance);
                sendEvent(reactContext, "Beacon", params);
            }

            @Override
            public void onScanFailed(int errorCode) {
                WritableMap params = Arguments.createMap();
                params.putInt("code", errorCode);
                params.putString("message", "Scan failed");
                sendEvent(reactContext, "Error", params);
            }
        };

        mAdvertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                WritableMap params = Arguments.createMap();
                params.putString("message", "Broadcasting started");
                sendEvent(reactContext, "Error", params);
            }

            @Override
            public void onStartFailure(int errorCode) {
                WritableMap params = Arguments.createMap();
                params.putInt("code", errorCode);
                params.putString("message", "Broadcasting failed");
                params.putBoolean("1", bluetoothAdapter.isMultipleAdvertisementSupported());
                params.putBoolean("2", bluetoothAdapter.isOffloadedFilteringSupported());
                params.putBoolean("3", bluetoothAdapter.isOffloadedScanBatchingSupported());
                sendEvent(reactContext, "Error", params);
            }
        };
    }

    @Override
    public String getName() {
        return "Beacon";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @ReactMethod
    public void init(Promise promise) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            promise.resolve(null);
        } else {
            promise.reject("-1", "Bluetooth is not available");
        }
    }

    @ReactMethod
    public void listen(Promise promise) {
        if (mBluetoothLeScanner == null) {
            promise.reject("-1", "Bluetooth is not available");
            return;
        }
        if (!isListening) {
            isListening = true;
            setScanFilter();
            setScanSettings();
            mBluetoothLeScanner.startScan(Collections.singletonList(mScanFilter), mScanSettings, mScanCallback);
            promise.resolve(null);
        } else {
            promise.reject("-1", "It is already listening");
        }
    }

    @ReactMethod
    public void stopListen(Promise promise) {
        if (mBluetoothLeScanner == null) {
            promise.reject("-1", "Bluetooth is not available");
            return;
        }
        mBluetoothLeScanner.stopScan(mScanCallback);
        isListening = false;
        promise.resolve(null);
    }

    @ReactMethod
    public void broadcast(Promise promise) {
        if (mBluetoothLeAdvertiser == null) {
            promise.reject("-1", "Bluetooth is not available");
            return;
        }
        if (!isBroadcasting) {
            isBroadcasting = true;
            setAdvertiseData();
            setAdvertiseSettings();
            mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseCallback);
            promise.resolve(null);
        } else {
            promise.reject("-1", "It is already broadcasting");
        }
    }

    @ReactMethod
    public void stopBroadcast(Promise promise) {
        if (mBluetoothLeAdvertiser == null) {
            promise.reject("-1", "Bluetooth is not available");
            return;
        }
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        isBroadcasting = false;
        promise.resolve(null);
    }

    private void setScanFilter() {
        ScanFilter.Builder mBuilder = new ScanFilter.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
        ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
        byte[] uuid = getIdAsByte(UUID.fromString(MANUFACTURE_UUID));
        mManufacturerData.put(0, (byte) 0xBE);
        mManufacturerData.put(1, (byte) 0xAC);
        for (int i = 2; i <= 17; i++) {
            mManufacturerData.put(i, uuid[i - 2]);
        }
        for (int i = 0; i <= 17; i++) {
            mManufacturerDataMask.put((byte) 0x01);
        }
        mBuilder.setManufacturerData(MANUFACTURE_ID, mManufacturerData.array(), mManufacturerDataMask.array());
        mScanFilter = mBuilder.build();
    }

    private void setScanSettings() {
        ScanSettings.Builder mBuilder = new ScanSettings.Builder();
        mBuilder.setReportDelay(0);
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        mScanSettings = mBuilder.build();
    }

    private double calculateDistance(int txPower, int rssi) {
        if (rssi == 0) {
            return -1.0;
        }
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    //////////////Emitter
    private void setAdvertiseData() {
        AdvertiseData.Builder mBuilder = new AdvertiseData.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(24);
        byte[] uuid = getIdAsByte(UUID.fromString(MANUFACTURE_UUID));
        mManufacturerData.put(0, (byte) 0xBE); // Beacon Identifier
        mManufacturerData.put(1, (byte) 0xAC); // Beacon Identifier
        for (int i = 2; i <= 17; i++) {
            mManufacturerData.put(i, uuid[i - 2]); // adding the UUID
        }
        mManufacturerData.put(18, (byte) 0x00); // first byte of Major
        mManufacturerData.put(19, (byte) 0x09); // second byte of Major
        mManufacturerData.put(20, (byte) 0x00); // first minor
        mManufacturerData.put(21, (byte) 0x06); // second minor
        mManufacturerData.put(22, (byte) 0xB5); // txPower
        mBuilder.addManufacturerData(MANUFACTURE_ID, mManufacturerData.array());
        mAdvertiseData = mBuilder.build();
    }

    private void setAdvertiseSettings() {
        AdvertiseSettings.Builder mBuilder = new AdvertiseSettings.Builder();
        mBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        mBuilder.setConnectable(false);
        mBuilder.setTimeout(0);
        mBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        mAdvertiseSettings = mBuilder.build();
    }

    private static byte[] getIdAsByte(java.util.UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}