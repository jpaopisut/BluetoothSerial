cordova.define("com.megster.cordova.bluetoothserial.bluetoothSerial", function (require, exports, module) { /*global cordova*/
    module.exports = {

        connect: function (macAddress, success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "connect", [macAddress]);
        },

        // Android only - see http://goo.gl/1mFjZY
        connectInsecure: function (macAddress, success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "connectInsecure", [macAddress]);
        },

        disconnect: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "disconnect", []);
        },

        isDiscovering: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "isDiscovering", []);
        },

        startDiscovery: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "startDiscovery", []);
        },

        stopDiscovery: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "stopDiscovery", []);
        },

        stopDiscovery: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "stopDiscovery", []);
        },

        isPaired: function (address,success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "isPaired", [address]);
        },

        pair: function (address,success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "pair", [address]);
        },

        // list bound devices
        list: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "list", []);
        },

        isEnabled: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "isEnabled", []);
        },

        isConnected: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "isConnected", []);
        },

        // the number of bytes of data available to read is passed to the success function
        available: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "available", []);
        },

        // read all the data in the buffer
        read: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "read", []);
        },

        // reads the data in the buffer up to and including the delimiter
        readUntil: function (delimiter, success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "readUntil", [delimiter]);
        },

        // writes data to the bluetooth serial port - data must be a string
        write: function (data, success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "write", [data]);
        },

        // calls the success callback when new data is available
        subscribe: function (delimiter, success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "subscribe", [delimiter]);
        },

        // removes data subscription
        unsubscribe: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "unsubscribe", []);
        },

        // clears the data buffer
        clear: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "clear", []);
        },

        // reads the RSSI of the *connected* peripherial
        readRSSI: function (success, failure) {
            cordova.exec(success, failure, "BluetoothSerial", "readRSSI", []);
        }

    };

});
