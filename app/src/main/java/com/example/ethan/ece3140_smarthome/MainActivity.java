package com.example.ethan.ece3140_smarthome;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * MainActivity is an Activity that runs on startup of the app. When the app is open, the GUI is
 * displayed, and the two GUI components (button and switch) are identified in the code and
 * assigned listeners.
 * The listeners handle actions by the user and are commented extensively below.
 */
public class MainActivity extends ActionBarActivity {

    // GUI components
    Button connect;
    Switch light;
    // Bluetooth adapter used for fundamental Bluetooth tasks
    BluetoothAdapter adapter;
    // Bluetooth socket - a connecting or connected Bluetooth socket
    BluetoothSocket socket;
    // Writable sink for bytes
    OutputStream outputStream = null;
    // Flag true if connection between device and board module
    boolean isConnected;

    /**
     * onCreate method is the function that is called when this activity is initially created (when
     * the app is opened).
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isConnected = false;
        // Initialize GUI components in code by identifying them with their respective IDs
        connect = (Button) findViewById(R.id.connect);
        light = (Switch) findViewById(R.id.light);
        // Device is initially not connected; do not enable light toggle
        light.setEnabled(false);
        // Gets Bluetooth adapter of the device. If device doesn't support Bluetooth, return null.
        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            toastWithMessage("Device does not support Bluetooth.");
            return;
        }

        setConnectListener();
        setLightListener();
    }

    /**
     * Creates and displays a toast (temporary popup message).
     * @param message Message displayed by the toast.
     */
    private void toastWithMessage(String message) {
        Context context = getApplicationContext();
        CharSequence text = message;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    /**
     * Create a click-listener for the connect button. If the device is connected, then disconnect
     * from the Bluetooth module by closing the socket, setting our fields to appropriate values,
     * and adjusting the GUI. If the device is disconnected, then loop through the paired devices,
     * find the device with the name of the Bluetooth module ("HC-06"), enable the adapter if it
     * is not already enabled, and create the socket. Once the socket is created, we can call the
     * connect() method, which establishes the Bluetooth connection, and we then set the fields to
     * appropriate values and adjust the GUI.
     */
    private void setConnectListener() {
        connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(isConnected) {
                    if (socket != null) {
                        try {
                            socket.close();
                            socket = null;
                            outputStream = null;
                            isConnected = false;
                            connect.setText("Connect to Device");
                            connect.setBackgroundColor(Color.LTGRAY);
                            isConnected = false;

                            light.setEnabled(false);
                        }
                        catch (Exception e) {

                        }
                    }
                }
                else {
                    Set<BluetoothDevice> paired = adapter.getBondedDevices();
                    BluetoothDevice module = null;
                    for (BluetoothDevice device : paired) {
                        if (device.getName().equals("HC-06"))
                            module = device;
                    }
                    if (module == null) {
                        toastWithMessage("Please pair with the device.");
                        return;
                    }
                    if (!adapter.isEnabled()) {
                        Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableAdapter, 0);
                    }
                    UUID uuid = module.getUuids()[0].getUuid();
                    try {
                        socket = module.createRfcommSocketToServiceRecord(uuid);
                        socket.connect();
                        light.setEnabled(true);
                        connect.setText("Disconnect");
                        connect.setBackgroundColor(Color.CYAN);
                        isConnected = true;
                        outputStream=socket.getOutputStream();
                    }
                    catch (Exception e) {

                    }
                }
            }
        });
    }

    /**
     * Create a listener for the light toggle switch that is called whenever the light toggle is
     * either turned on or off. When the light is off, the message to send to the module is "B",
     * and the message for turning on the light is "A". These are converted to ASCII values when
     * received by the MCU. Messages sent to the module are written out by byte by the OutputStream.
     */
    private void setLightListener() {
        light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String message = "";
                if (isChecked) { // turn on light
                    toastWithMessage("Turning on light.");
                    message = "A";
                } else { // turn off light
                    toastWithMessage("Turning off light.");
                    message = "B";
                }
                try {
                    outputStream.write(message.getBytes()[0]);
                    outputStream.flush();
                } catch (Exception e) {

                }
            }
        });
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
