package com.bairuitech.bluetooth;

import android.bluetooth.BluetoothSocket;

import com.bairuitech.callcenter.VideoActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Administrator on 2014/11/27.
 */
public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private VideoActivity myActivity = null;
    private String message;

    public ConnectedThread(BluetoothSocket socket,VideoActivity myActivity) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        this.myActivity = myActivity;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                message = new String(buffer,"UTF-8");

            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String message) {
        try {
            byte[] bytes = message.getBytes("UTF-8");
            mmOutStream.write(bytes);
            mmOutStream.flush();
            myActivity.setSendOrder(message);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}