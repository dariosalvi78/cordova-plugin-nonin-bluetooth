/*
 * Android Interface to Nonin 9560BT
 *
 * @author Carmelo Velardo, Dario Salvi, Arvind Raghu, Oliver Gibson
 */

package org.apache.cordova.nonin;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;


/**
 * Control the Nonin and its status, records data, communicate with a User Interface
 *
 * @author Carmelo Velardo, Dario Salvi
 */
public class Nonin implements Serializable {

    private static final long serialVersionUID = 713444501843048481L;

    public static interface NoninHandler {
        public void handle(NoninPacket frame);
    }

    /**
     * Tells if it's busy connected or connecting
     */
    private boolean busy;
    /**
     * Used by the parsing thread
     */
    private boolean keepparsing;

    /**
     * GUI messages handler
     */
    private NoninHandler messageHandler;


    //Bluetooth variable
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private String deviceMACAddress = "";


    /**
     * Initialises the Nonin device.
     *
     * @param btAdapter        the bluetooth adapter
     * @param MacAddressDevice the address of the device
     * @param handler          the handler of the received packets
     */
    public Nonin(BluetoothAdapter btAdapter, String MacAddressDevice, NoninHandler handler) {

        mBluetoothAdapter = btAdapter;

        deviceMACAddress = MacAddressDevice;

        this.messageHandler = handler;
    }

    /**
     * Tells if the device, with specified address, has been bonded
     * If the BT adapter is not enabled, it will always return false
     * @param btAdapter a bluetooth adapter
     * @param address the specific address of the device
     * @return
     */
    public static boolean isBonded(BluetoothAdapter btAdapter, String address) {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (address.equalsIgnoreCase(device.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Starts the device
     */
    public synchronized void start() throws IOException {
        if (busy)
            return;

        Log.i(this.toString(), "Starting the device");
        busy = true;
        connect();
        setDataMode("D7");
        // Init state
        keepparsing = true;
        Parser parser = new Parser();
        new Thread(parser).start();
    }

    private class Parser implements Runnable {

        @Override
        public void run() {
            int PACKET_SIZE = 5;
            //circular buffer of bytes
            byte[] buffer = new byte[PACKET_SIZE];
            //position used in the buffer
            int buffPos = 0;
            NoninPacket packet = new NoninPacket();
            BufferedInputStream bufferIS = new BufferedInputStream(inStream);


            while (keepparsing) {
                if(buffPos >= PACKET_SIZE) {
                    //move bytes backwards
                    for (int k = 0; k < PACKET_SIZE-1; k++) {
                        buffer[k] = buffer[k+1];
                    }
                    buffPos--;
                }
                try {
                    buffer[buffPos] = (byte) (bufferIS.read() & 0xFF);
                    buffPos++;
                } catch (IOException ex) {
                    //probably the connection was closed, nothing to worry about
                    Log.d(Nonin.class.getName(), "IOException");
                }

                if (NoninFrame.IsValidFrame(buffer)) {
                    // Process this packet
                    if (NoninFrame.IsSyncFrame(buffer)) {
                        // This packet must always be the first in the packet
                        //Log.d(Nonin.class.getName(),"Got sync frame");
                        packet = new NoninPacket();
                    }
                    if(packet != null){
                        //Log.d(Nonin.class.getName(),"Got a frame");
                        packet.addFrame(new NoninFrame(buffer));

                        if (packet.isFull()) {
                            //Log.d(Nonin.class.getName(),"Got full packet spo2:"+packet.getDisplayedSpO2Average()+" hr:"+packet.getDisplayedHRAverage()+" artifacts: "+packet.hasAnyArtifact());
                            // Received a complete packet
                            // send it and start a new packet
                            if (messageHandler != null)
                                messageHandler.handle(new NoninPacket(packet));
                            packet = null;
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops the device
     */
    public synchronized void stop() {
        Log.i(this.toString(), "Stopping the device");
        busy = false;
        keepparsing = false;
        disconnect();
    }

    /**
     * Tells if the device is connected or connecting
     */
    public synchronized boolean isActive() {
        return busy;
    }

    /**
     * This create and setup the Bluetooth channel
     *
     * @return true if connection established
     */
    private void connect() throws IOException {
        Log.i(Nonin.class.toString(), "Connecting to: " + deviceMACAddress);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceMACAddress);
        ParcelUuid[] uuids = device.getUuids();

        btSocket = device.createInsecureRfcommSocketToServiceRecord(uuids[0].getUuid());

        btSocket.connect();
        Log.i(Nonin.class.toString(), "Bluetooth connection established, data transfer link open.");
        outStream = btSocket.getOutputStream();
        inStream = btSocket.getInputStream();
    }


    /**
     * If any I/O channel or the bluetooth socket are
     * opened, it closes them
     */
    private void disconnect() {

        try {
            if (inStream != null)
                inStream.close();
            if (outStream != null)
                outStream.close();
            if (btSocket != null)
                btSocket.close();
        } catch (IOException e) {
            Log.e(Nonin.class.toString(), "Couldn't disconnect easily", e);
        }
    }

    /**
     * This method is used to set the data mode of the device
     *
     * @param dataMode the datamode to set on the device
     * @return
     */
    private void setDataMode(String dataMode) throws IOException {

        // Initialise the "init" packet
        byte initPacket[] = new byte[6];
        initPacket[0] = 0x02; // START
        initPacket[1] = 0x70; // Op Code
        initPacket[2] = 0x02; // Data Size
        initPacket[3] = 0x02; // Data Type

        if (dataMode.equalsIgnoreCase("D7")) {
            initPacket[4] = 0x07;
        } else if (dataMode.equalsIgnoreCase("D13")) {
            initPacket[4] = 0x0D;
        } else if (dataMode.equalsIgnoreCase("D8")) {
            initPacket[4] = 0x08;
        } else if (dataMode.equalsIgnoreCase("D2")) {
            initPacket[4] = 0x02;
        }

        initPacket[5] = 0x03; // ETX

        outStream.write(initPacket);
        Log.i(Nonin.class.toString(), "Sent " + dataMode + " to the NONIN");
    }

}
