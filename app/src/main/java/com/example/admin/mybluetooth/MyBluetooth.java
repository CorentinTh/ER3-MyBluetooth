package com.example.admin.mybluetooth;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 *  -- MyBluetooth --
 *
 *  Created by THOMASSET Corentin and DUPONT Cyrille
 *  GEII IUT Annecy - January 2017
 *
 */


public class MyBluetooth {


    /* ----------------------------------- Attributes ----------------------------------- */

    private BluetoothAdapter mBluetoothAdapter;         // The bluetooth adapter
    private BluetoothDevice[] mPairedDevices;           // List of the paired devices
    public String[] mPairedDevicesName;                 // List of the paired devices names
    public ArrayList<BluetoothDevice> mVisibleDevices;  // List of detectable devices
    public ArrayList<String> mVisibleDevicesName;       // List of detectable devices names
    private ConnectedThread mConnectedThread = null;    // The thread which manage the connection

    private boolean bDataSendingAllowed = false;        // Boolean which allow the sending of data

    private Handler mHandler; // Handler that permit to send data to the main activity

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  // Dummy UUID

    // Constants of the application
    public interface Constants {
        int REQUEST_CODE_ENABLE_BLUETOOTH = 0;  // A constant needed to turn the bluetooth on

        int MESSAGE_TOAST = 1;                  // To send a message that have to be displayed as a Toast
        int MESSAGE_DATA = 2;                   // To send bluetooth received data to the main
        int MESSAGE_ERROR_NO_BLUETOOTH = 3;     // To inform the main activity that the device those not have a bluetooth module
        int MESSAGE_ASK_TURN_BLUETOOTH_ON = 4;  // To show a pop up that ask the user if we can turn the bluetooth on
        int MESSAGE_SHOW_PAIRED_DEVICES = 5;    // To display a the list of the paired devices

        int NUM_BYTES_MAX_RECEPTION = 20;       // The number of bytes that can be received by reception
    }


    /**
     * Usable to detect unpaired visible devices 
     */
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // If a device is detectable ...
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // ... we add it in a BluetoothDevice array
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mVisibleDevices.add(device);
                mVisibleDevicesName.add(device.getName());
            }
        }
    };


    /* ----------------------------------- Methods ----------------------------------- */

    /**
     * Constructor of the MyBluetooth class.
     * He check if the device has a bluetooth module and ask to turn the Bluetooth on if it's off
     *
     * @param mainHandler An mComHandler to communicate from MyBluetooth to MainActivity
     */
    public MyBluetooth(Handler mainHandler){

        // This mComHandler is used to send message to the main activity
        this.mHandler = mainHandler;

        // We get the Bluetooth adapter 
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        
        // We test if the device support Bluetooth
        if (null == mBluetoothAdapter){

            // We inform the user
            this.sendToMain(Constants.MESSAGE_ERROR_NO_BLUETOOTH);

            Log.i("debug", "MyBluetooth : No bluetooth module on this device");

        }

        // If the bluetooth is off ...
        if (!this.mBluetoothAdapter.isEnabled()) {

            // ... we ask the user to turn it on
            this.sendToMain(Constants.MESSAGE_ASK_TURN_BLUETOOTH_ON);

            Log.i("debug", "MyBluetooth : Asking to turn the bluetooth on");
        }
    }


    /**
     * This method allow us to send messages to the main activity,
     * like Toasts or pop-ups, but also received data
     *
     * @param iType Type of the message (refer to Constants.MESSAGE_...)
     * @param object The message itself
     */
    public void sendToMain(int iType, Object object){
        Message myMessage = mHandler.obtainMessage();

        myMessage.what = iType;
        myMessage.obj = object;

        mHandler.sendMessage(myMessage);
    }

    /**
     * This method allow us to send "flags" to the main activity,
     * like the fact that the device does not have bluetooth module
     *
     * @param iType Type of the message (refer to Constants.MESSAGE_...)
     */
    public void sendToMain(int iType){
        this.sendToMain(iType, "");
    }


    /**
     * This function send an array of bytes via the Bluetooth connection
     * It call the write() method in ConnectedThread class
     *
     *  @param abData Byte array to send
     */
    public void write(byte[] abData){

        if (bDataSendingAllowed && mConnectedThread != null){
            mConnectedThread.write(abData);
        }
    }


    /**
     * This method send a bytes via the Bluetooth connection
     * It overload the previous method
     *
     * @param bData Byte to send
     */
    public void write(byte bData){

        // We convert a byte in an array of byte

        byte[] abData;

        abData = new byte[2];
        abData[0] = bData;
        abData[1] = 0;

        this.write(abData);
    }

    /**
     * This method send a bytes via the Bluetooth connection
     * It overload the previous method
     *
     * @param strData String to send
     */
    public void write(String strData){
        // We convert a string into an array of bytes with .getBytes() method
        this.write(strData.getBytes());
    }


    /**
     * This method get a list of the paired devices and send it to the MainActivity (via the Handler)
     */
    public void setPairedDevices(){

        // Before getting the paired devices list, we check is the bluetooth is enable
        if (!this.mBluetoothAdapter.isEnabled()) {

            // We ask the user to turn it on
            this.sendToMain(Constants.MESSAGE_ASK_TURN_BLUETOOTH_ON);

            Log.i("debug", "MyBluetooth : Asking to turn the bluetooth on");

            return;
        }


        // Recovery of the list of known devices in an array of BluetoothDevice
        Set<BluetoothDevice> aKnownDevices = mBluetoothAdapter.getBondedDevices();

        if (aKnownDevices.size() > 0) {
            // Array of known devices name
            this.mPairedDevicesName = new String[aKnownDevices.size()];
            // Array of known Devices
            this.mPairedDevices = new BluetoothDevice[aKnownDevices.size()];

            // Iteration integer
            int iLoop = 0;

            // We browse the paired devices array in order to get the device and his name
            for(BluetoothDevice bclDevices : aKnownDevices) {
                this.mPairedDevices[iLoop] = bclDevices;
                this.mPairedDevicesName[iLoop] = bclDevices.getName();
                ++iLoop;
            }
            this.sendToMain(Constants.MESSAGE_SHOW_PAIRED_DEVICES);
        }else {
            this.sendToMain(Constants.MESSAGE_TOAST, "No known devices");
        }


    }


    /**
     * This method create a connexion with the selected device
     */
    public void Connexion(int mSelectedDeviceID){
            // We start a ConnectThread with the selected device
            new ConnectThread(mPairedDevices[mSelectedDeviceID]).start();
    }





    /**
     * This thread create a connexion with a device passed in argument
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        /**
         * @param deviceToConnect The device you want to connect
         */
        public ConnectThread(BluetoothDevice deviceToConnect) {
            // Use a temporary object that is later assigned to mmSocket because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = deviceToConnect;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice
                // MY_UUID is the app's UUID string, also used in the server code
                tmp = deviceToConnect.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                sendToMain(Constants.MESSAGE_TOAST,"Failed to connect");
                Log.i("debug", "MyBluetooth : Socket's create() method failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket
                // This call blocks until it succeeds or throws an exception
                mmSocket.connect();
                Log.i("debug", "MyBluetooth : Trying to connect");
            } catch (IOException connectException) {
                // Unable to connect, we close the socket and return
                sendToMain(Constants.MESSAGE_TOAST,"Failed to connect to " + mmDevice.getName());

                Log.i("debug", "MyBluetooth : Failed to connect to " + mmDevice.getName());

                try {
                    mmSocket.close();
                    Log.i("debug", "MyBluetooth : Closing the socket");
                } catch (IOException closeException) {
                    sendToMain(Constants.MESSAGE_TOAST,"An error has occurred, please restart");
                    Log.i("debug", "MyBluetooth : Could not close the client socket");
                }

                // We delete the actual connexion if it exists
                if (mConnectedThread != null){
                    mConnectedThread.reset();
                }

                return;
            }

            // At this point, a connection is established

            // We inform the user that a connexion has succeeded
            sendToMain(Constants.MESSAGE_TOAST,"Connected to " + mmDevice.getName());

            Log.i("debug", "MyBluetooth : Connected to : " + mmDevice.getName());


            // We create a looping Thread that constantly wait to received data
            // And allow us to send data
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        // Closes the client socket and causes the thread to finish
        public void cancel() {
            try {
                mmSocket.close();
                Log.i("debug", "MyBluetooth : Closing the client socket");
            } catch (IOException e) {
                Log.i("debug", "MyBluetooth : Could not close the client socket");
            }
        }
    }


    /**
     * This thread manage a connexion with the client socket passed in argument
     */
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;     // Client socket
        private final InputStream mmInStream;       // Stream to read
        private final OutputStream mmOutStream;     // Stream to write
        private byte[] mmBuffer;                    // mmBuffer store for the stream
        private boolean bLooping = true;            // Boolean in order to stop the reading loop

        /**
         * @param socket The socket to connect
         */
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            bDataSendingAllowed = true;

            // Get the input and output streams; using temp objects because member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.i("debug", "MyBluetooth : Error occurred when creating input stream");
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.i("debug", "MyBluetooth : Error occurred when creating output stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[Constants.NUM_BYTES_MAX_RECEPTION];
            int numBytes; // Number of bytes received

            // We listen permanently (read() blocks)
            while (bLooping) {
                try {
                    // We read incoming data
                    numBytes = mmInStream.read(mmBuffer);

                    // If their's at least one Byte, we send them to the main
                    if (numBytes > 0){
                        sendToMain(Constants.MESSAGE_DATA, new String(mmBuffer));
                        Log.i("debug", "MyBluetooth : " + numBytes + " bytes received");
                    }

                } catch (IOException e) {
                    Log.i("debug", "MyBluetooth : Input stream was disconnected");
                    sendToMain(Constants.MESSAGE_TOAST, "Disconnected");
                    break;
                }
            }
        }

        /**
         * Call this to send data to the remote device
         *
         * @param abData Data to send
         */
        public void write(byte[] abData) {
            try {
                Log.i("debug","MyBluetooth : Sending data");
                mmOutStream.write(abData);
            } catch (IOException e) {
                bDataSendingAllowed = false;

                Log.i("debug", "MyBluetooth : Error occurred when sending data");
                sendToMain(Constants.MESSAGE_TOAST, "Couldn't send data to the other device");
            }
        }

        /**
         * Call this method to shut down the connection
         */
        public void reset() {

            // If we close the connexion we disallow data sending
            bDataSendingAllowed = false;

            Log.i("debug","MyBluetooth : Reset connection");

            // Closing the client socket if it exists
            if (mmSocket != null){
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.i("debug", "MyBluetooth : Could not close the connect socket");
                }
            }

            // Closing the output stream if it has been set
            if (mmInStream != null){
                try{
                    mmInStream.close();
                }catch (IOException e){
                    Log.i("debug", "MyBluetooth : Failed to close input stream");
                }
            }

            // Closing the input stream if it has been set
            if (mmOutStream != null){
                try{
                    mmOutStream.close();
                }catch (IOException e){
                    Log.i("debug", "MyBluetooth : Failed to close output stream");
                }
            }

            // Stopping the reading loop
            bLooping = false;
        }
    }
}


/**
 *  -- MyBluetooth --
 *
 *  Created by THOMASSET Corentin and DUPONT Cyrille
 *  GEII IUT Annecy - January 2017
 *
 */