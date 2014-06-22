package com.megster.cordova;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

// kludgy imports to support 2.9 and 3.0 due to package changes
// import org.apache.cordova.CordovaArgs;
// import org.apache.cordova.CordovaPlugin;
// import org.apache.cordova.CallbackContext;
// import org.apache.cordova.PluginResult;
// import org.apache.cordova.LOG;

/**
 * PhoneGap Plugin for Serial Communication over Bluetooth
 */
public class BluetoothSerial extends CordovaPlugin {

    // actions
    private static final String LIST = "list";
    private static final String CONNECT = "connect";
    private static final String CONNECT_INSECURE = "connectInsecure";
    private static final String DISCONNECT = "disconnect";
    private static final String WRITE = "write";
    private static final String AVAILABLE = "available";
    private static final String READ = "read";
    private static final String READ_UNTIL = "readUntil";
    private static final String SUBSCRIBE = "subscribe";
    private static final String UNSUBSCRIBE = "unsubscribe";
    private static final String IS_ENABLED = "isEnabled";
    private static final String IS_CONNECTED = "isConnected";
    private static final String CLEAR = "clear";

    private static final String ACTION_IS_DISCOVERING = "isDiscovering";
    private static final String ACTION_START_DISCOVERY = "startDiscovery";
    private static final String ACTION_STOP_DISCOVERY = "stopDiscovery";
    private static final String ACTION_IS_PAIRED = "isPaired";
    private static final String ACTION_PAIR = "pair";

    // callbacks
    private CallbackContext connectCallback;
    private CallbackContext dataAvailableCallback;
    private CallbackContext discoveryCallback;
    private CallbackContext pairingCallback;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSerialService bluetoothSerialService;

    // Debugging
    private static final String TAG = "BluetoothSerial";
    private static final boolean D = true;

    // Message types sent from the BluetoothSerialService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_DISCOVERY_STARTED = 6;
    public static final int MESSAGE_DISCOVERY_FINISHED = 7;
    public static final int MESSAGE_DEVICE_FOUND = 8;
    public static final int MESSAGE_DEVICE_BONDED = 9;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String DATA_DEVICE_NAME = "name";
    public static final String DATA_DEVICE_ADDRESS = "address";

    public static int ERR_UNKNOWN = 404;

    private boolean _wasDiscoveryCanceled;

    StringBuffer buffer = new StringBuffer();
    private String delimiter;

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        LOG.d(TAG, "action = " + action);

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (bluetoothSerialService == null) {
            bluetoothSerialService = new BluetoothSerialService(cordova.getActivity().getBaseContext(), mHandler);
        }

        boolean validAction = true;

        if (action.equals(LIST)) {

            listBondedDevices(callbackContext);

        } else if (action.equals(CONNECT)) {

            boolean secure = true;
            connect(args, secure, callbackContext);

        } else if (action.equals(CONNECT_INSECURE)) {

            // see Android docs about Insecure RFCOMM http://goo.gl/1mFjZY
            boolean secure = false;
            connect(args, false, callbackContext);

        } else if (action.equals(DISCONNECT)) {

            connectCallback = null;
            bluetoothSerialService.stop();
            callbackContext.success();

        } else if (action.equals(WRITE)) {

            String data = args.getString(0);
            bluetoothSerialService.write(data.getBytes());
            callbackContext.success();

        } else if (action.equals(AVAILABLE)) {

            callbackContext.success(available());

        } else if (action.equals(READ)) {

            callbackContext.success(read());

        } else if (action.equals(READ_UNTIL)) {

            String interesting = args.getString(0);
            callbackContext.success(readUntil(interesting));

        } else if (action.equals(SUBSCRIBE)) {

            delimiter = args.getString(0);
            dataAvailableCallback = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else if (action.equals(UNSUBSCRIBE)) {

            delimiter = null;
            dataAvailableCallback = null;

            callbackContext.success();

        } else if (action.equals(IS_ENABLED)) {

            if (bluetoothAdapter.isEnabled()) {
                callbackContext.success();
            } else {
                callbackContext.error("Bluetooth is disabled.");
            }

        } else if (action.equals(IS_CONNECTED)) {

            if (bluetoothSerialService.getState() == BluetoothSerialService.STATE_CONNECTED) {
                callbackContext.success();
            } else {
                callbackContext.error("Not connected.");
            }

        } else if (action.equals(CLEAR)) {

            buffer.setLength(0);
            callbackContext.success();

        } else if (action.equals(ACTION_IS_DISCOVERING)) {
            try {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, bluetoothSerialService.isDiscovering()));
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                e.printStackTrace();
            }
        } else if (action.equals(ACTION_START_DISCOVERY)) {
            startDiscovery(args, callbackContext);
        } else if (action.equals(ACTION_STOP_DISCOVERY)) {
            stopDiscovery(args, callbackContext);
        } else if (ACTION_IS_PAIRED.equals(action)) {
            try {
                String address = args.getString(0);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, bluetoothSerialService.isBonded(address)));
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                e.printStackTrace();
            }
        } else if (ACTION_PAIR.equals(action)) {
            if (pairingCallback != null) {
                this.error(callbackContext, "Pairing process is already in progress.", 404);
            } else {
                try {
                    String address = args.getString(0);
                    bluetoothSerialService.createBond(address);
                    pairingCallback = callbackContext;
                } catch (Exception e) {
                    pairingCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    pairingCallback = null;
                    e.printStackTrace();
                }
            }
        } else {

            validAction = false;

        }

        return validAction;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothSerialService != null) {
            bluetoothSerialService.stop();
        }
    }


    /**
     * Start a device discovery.
     *
     * @param args        Arguments given.
     * @param callbackCtx Where to send results.
     */
    private void startDiscovery(CordovaArgs args, CallbackContext callbackCtx) {
        // TODO Someday add an option to fetch UUIDs at the same time

        try {
            if (bluetoothSerialService.isConnecting()) {
                this.error(callbackCtx, "A Connection attempt is in progress.", 404);
            } else {
                if (bluetoothSerialService.isDiscovering()) {
                    _wasDiscoveryCanceled = true;
                    bluetoothSerialService.stopDiscovery();

                    if (discoveryCallback != null) {
                        this.error(discoveryCallback,
                                "Discovery was stopped because a new discovery was started.",
                                505
                        );
                        discoveryCallback = null;
                    }
                }

                bluetoothSerialService.startDiscovery();

                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true);
                callbackCtx.sendPluginResult(result);

                discoveryCallback = callbackCtx;
            }
        } catch (Exception e) {
            this.error(callbackCtx, e.getMessage(), 404);
        }
    }


    /**
     * Stop device discovery.
     *
     * @param args        Arguments given.
     * @param callbackCtx Where to send results.
     */
    private void stopDiscovery(CordovaArgs args, CallbackContext callbackCtx) {
        try {
            if (bluetoothSerialService.isDiscovering()) {
                _wasDiscoveryCanceled = true;
                bluetoothSerialService.stopDiscovery();

                if (discoveryCallback != null) {
                    this.error(discoveryCallback,
                            "Discovery was cancelled.",
                            500
                    );

                    discoveryCallback = null;
                }

                callbackCtx.success();
            } else {
                this.error(callbackCtx, "There is no discovery to cancel.", 404);
            }
        } catch (Exception e) {
            this.error(callbackCtx, e.getMessage(), 404);
        }
    }

    private void listBondedDevices(CallbackContext callbackContext) throws JSONException {
        JSONArray deviceList = new JSONArray();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : bondedDevices) {
            JSONObject json = new JSONObject();
            json.put("name", device.getName());
            json.put("address", device.getAddress());
            json.put("id", device.getAddress());
            if (device.getBluetoothClass() != null) {
                json.put("class", device.getBluetoothClass().getDeviceClass());
            }
            deviceList.put(json);
        }
        callbackContext.success(deviceList);
    }

    private void connect(CordovaArgs args, boolean secure, CallbackContext callbackContext) throws JSONException {
        String macAddress = args.getString(0);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

        if (device != null) {
            connectCallback = callbackContext;
            bluetoothSerialService.connect(device, secure);

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

        } else {
            callbackContext.error("Could not connect to " + macAddress);
        }
    }

    // The Handler that gets information back from the BluetoothSerialService
    // Original code used handler for the because it was talking to the UI.
    // Consider replacing with normal callbacks
    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    buffer.append((String) msg.obj);

                    if (dataAvailableCallback != null) {
                        sendDataToSubscriber();
                    }
                    break;
                case MESSAGE_STATE_CHANGE:

                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            Log.i(TAG, "BluetoothSerialService.STATE_CONNECTED");
                            notifyConnectionSuccess();
                            break;
                        case BluetoothSerialService.STATE_CONNECTING:
                            Log.i(TAG, "BluetoothSerialService.STATE_CONNECTING");
                            break;
                        case BluetoothSerialService.STATE_LISTEN:
                            Log.i(TAG, "BluetoothSerialService.STATE_LISTEN");
                            break;
                        case BluetoothSerialService.STATE_NONE:
                            Log.i(TAG, "BluetoothSerialService.STATE_NONE");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    //  byte[] writeBuf = (byte[]) msg.obj;
                    //  String writeMessage = new String(writeBuf);
                    //  Log.i(TAG, "Wrote: " + writeMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    Log.i(TAG, msg.getData().getString(DEVICE_NAME));
                    break;
                case MESSAGE_TOAST:
                    String message = msg.getData().getString(TOAST);
                    notifyConnectionLost(message);
                    break;
                case MESSAGE_DISCOVERY_STARTED:
                    _wasDiscoveryCanceled = false;
                    break;
                case MESSAGE_DISCOVERY_FINISHED:
                    if (!_wasDiscoveryCanceled) {
                        if (discoveryCallback != null) {
                            PluginResult result = new PluginResult(PluginResult.Status.OK, false);
                            discoveryCallback.sendPluginResult(result);
                            discoveryCallback = null;
                        }
                    }
                    break;
                case MESSAGE_DEVICE_FOUND:
                    try {
                        String name = msg.getData().getString(DATA_DEVICE_NAME);
                        String address = msg.getData().getString(DATA_DEVICE_ADDRESS);

                        JSONObject device = new JSONObject();
                        device.put("name", name);
                        device.put("address", address);

                        // Send one device at a time, keeping callback to be used again
                        if (discoveryCallback != null) {
                            PluginResult result = new PluginResult(PluginResult.Status.OK, device);
                            result.setKeepCallback(true);
                            discoveryCallback.sendPluginResult(result);
                        } else {
                            Log.e(TAG, "CallbackContext for discovery doesn't exist.");
                        }
                    } catch (JSONException e) {
                        if (discoveryCallback != null) {
                            BluetoothSerial.this.error(discoveryCallback,
                                    e.getMessage(),
                                    ERR_UNKNOWN
                            );
                            discoveryCallback = null;
                        }
                    }

                    break;

                case MESSAGE_DEVICE_BONDED:

                    try {
                        String name = msg.getData().getString(DATA_DEVICE_NAME);
                        String address = msg.getData().getString(DATA_DEVICE_ADDRESS);

                        JSONObject bondedDevice = new JSONObject();
                        bondedDevice.put("name", name);
                        bondedDevice.put("address", address);

                        if (pairingCallback != null) {
                            pairingCallback.success(bondedDevice);
                            pairingCallback = null;
                        } else {
                            Log.e(TAG, "CallbackContext for pairing doesn't exist.");
                        }
                    } catch (Exception e) {
                        if (pairingCallback != null) {
                            BluetoothSerial.this.error(pairingCallback,
                                    e.getMessage(), 500
                            );
                            pairingCallback = null;
                        }
                    }

                    break;

            }
        }
    };

    private void notifyConnectionLost(String error) {
        if (connectCallback != null) {
            connectCallback.error(error);
            connectCallback = null;
        }
        if (dataAvailableCallback != null) {
            dataAvailableCallback.error(error);
        }
    }

    private void notifyConnectionSuccess() {
        if (connectCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            result.setKeepCallback(true);
            connectCallback.sendPluginResult(result);
        }
    }

    private void sendDataToSubscriber() {
        String data = readUntil(delimiter);
        if (data != null && data.length() > 0) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            dataAvailableCallback.sendPluginResult(result);

            sendDataToSubscriber();
        }
    }

    private int available() {
        return buffer.length();
    }

    private String read() {
        int length = buffer.length();
        String data = buffer.substring(0, length);
        buffer.delete(0, length);
        return data;
    }

    private String readUntil(String c) {
        String data = "";
        int index = buffer.indexOf(c, 0);
        if (index > -1) {
            data = buffer.substring(0, index + c.length());
            buffer.delete(0, index + c.length());
        }
        return data.toString();
    }

    private void error(CallbackContext ctx, String msg, int code) {
        try {
            JSONObject result = new JSONObject();
            result.put("message", msg);
            result.put("code", code);

            ctx.error(result);
        } catch (Exception e) {
            Log.e(TAG, "Error with... error raising, " + e.getMessage());
        }
    }
}
