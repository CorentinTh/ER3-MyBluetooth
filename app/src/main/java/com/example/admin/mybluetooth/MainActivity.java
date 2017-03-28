package com.example.admin.mybluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 *  -- MyBluetooth --
 *
 *  Created by THOMASSET Corentin and DUPONT Cyrille
 *  GEII IUT Annecy - January 2017
 *
 *  App icon created with Android Asset Studio under a creative common license
 *  https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
 */


public class MainActivity extends AppCompatActivity {

    /* ----------------------------------- Attributes ----------------------------------- */

    public MyBluetooth mBluetooth;

    public byte[] bufferReceivedData;

    public TextView outputView;
    public EditText inputData;


    /* ----------------------------------- Methods ----------------------------------- */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        // Instancing the bluetooth
        this.mBluetooth = new MyBluetooth(mComHandler);

        // Instancing our reception array
        bufferReceivedData = new byte[MyBluetooth.Constants.NUM_BYTES_MAX_RECEPTION];

        // Instancing our user-interface objects
        outputView= (TextView) findViewById(R.id.outputView);
        inputData= (EditText) findViewById(R.id.inputData);
    }


    //Transmission de message par mComHandler
    public Handler mComHandler = new Handler() {
        @Override
        public void handleMessage(Message receivedMessage) {

            super.handleMessage(receivedMessage); // Messages processed by mother class
            final Object objData = receivedMessage.obj;

            // Depending of the '.what' of the message ...
            switch (receivedMessage.what){

                case MyBluetooth.Constants.MESSAGE_TOAST : // Displaying a toast
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, (String) objData, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;

                case MyBluetooth.Constants.MESSAGE_DATA : // Receiving a data from the bluetooth connexion
                    // Here you get your received data : in objData (

                    outputView.setText((String) objData);
                    break;

                case MyBluetooth.Constants.MESSAGE_ERROR_NO_BLUETOOTH: // Message d'erreur pas de bluetooth

                    // Pop-up qui informe a l'utilisateur qu'il n'a pas de bluetooth sur son telephone et qui ferme l'application lorsqu'on ferme ce pop-up
                    // Pop up which inform the user that his device doesn't handle Bluetooth and close the apps
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog
                                    .Builder(MainActivity.this)
                                    .setTitle("Little problem ...")
                                    .setMessage("Your phone does not support bluetooth. Thereby, this application is unusable."+System.getProperty("line.separator")+"You can delete it :(")
                                    .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialoginterface, int i) {
                                            MainActivity.this.finish();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            MainActivity.this.finish();
                                        }
                                    })
                                    .show();
                        }
                    });

                    break;

                case MyBluetooth.Constants.MESSAGE_ASK_TURN_BLUETOOTH_ON:
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, MyBluetooth.Constants.REQUEST_CODE_ENABLE_BLUETOOTH);
                    break;

                case MyBluetooth.Constants.MESSAGE_SHOW_PAIRED_DEVICES:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setSingleChoiceItems(mBluetooth.mPairedDevicesName, 0, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mBluetooth.Connexion(which);
                                            dialog.dismiss();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {

                                        }
                                    })
                                    .create()
                                    .show();
                        }
                    });
                    break;
            }
        }
    };


    // Manging buttons
    public void onclick(View myView){

        // According to the ID of the button ...
        switch(myView.getId()) {

            case R.id.btnConnexion: // Setting a connection
                this.mBluetooth.setPairedDevices();
                break;

            case R.id.btnSend:
                // Sending the data entered by the user
                String strData = inputData.getText().toString();

                if (strData.length() > 0){
                    this.mBluetooth.write((byte) 65);
                }else{
                    Toast.makeText(MainActivity.this, "Please enter some text", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


/**
 *  -- MyBluetooth --
 *
 *  Created by THOMASSET Corentin and DUPONT Cyrille
 *  GEII IUT Annecy - January 2017
 *
 */