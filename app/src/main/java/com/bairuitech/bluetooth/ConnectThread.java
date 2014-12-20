package com.bairuitech.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.bairuitech.callcenter.VideoActivity;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Administrator on 2014/11/27.
 */
public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final String NAME = "客户端";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;
    private VideoActivity myActivity;

    public ConnectThread(BluetoothDevice device,BluetoothAdapter mBluetoothAdapter,VideoActivity myActivity) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.myActivity = myActivity;

        this.mBluetoothAdapter = mBluetoothAdapter;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            System.out.println("客户端绑定设备错误");
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
            System.out.println("客户端连接成功");
            myActivity.setBluetoothState("客户端连接成功");

        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            System.out.println("客户端连接错误");
            myActivity.setBluetoothState("客户端连接错误");
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        myActivity.manageBluetoothConnectedSocket(mmSocket);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
