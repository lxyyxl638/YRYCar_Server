package com.bairuitech.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.bairuitech.callcenter.VideoActivity;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Administrator on 2014/11/27.
 */
public class AcceptThread extends Thread{
    private final BluetoothServerSocket mmServerSocket;
    private final String NAME = "服务器";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private VideoActivity myActivity = null;

    public AcceptThread(BluetoothAdapter mBluetoothAdapter,VideoActivity myActivity) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            System.out.println("服务器绑定错误");
        }
        mmServerSocket = tmp;

        this.myActivity = myActivity;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        System.out.println("开启服务器");
        while (true) {
            try {
                socket = mmServerSocket.accept();
                System.out.println("服务器已连接上");
                System.out.println("服务器已连接上");
            } catch (IOException e) {
                System.out.println("服务器监听错误");
                System.out.println("服务器监听错误");
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                myActivity.manageBluetoothConnectedSocket(socket);
                System.out.println("socket不为空");
                try {

                    mmServerSocket.close();
                } catch (IOException e) {
                    System.out.println("服务器关闭错误");
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
}
