package com.bairuitech.callcenter;

import android.app.Fragment;
import android.content.Intent;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2014/11/4.
 */
public class ServerServer implements Runnable {

    final private static int port = 9750;
    private boolean stop = false;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader read = null;
    private PrintStream print = null;
    private Fragment ControlFragment;
    private VideoActivity myActivity;

    public void setMyActivity(VideoActivity myActivity)
    {
        this.myActivity = myActivity;
    }


    @Override
    public void run() {

        try {
                while (myActivity == null)
                {
                    /*wait for the VideoActivity*/
                }
                myActivity.setIsRunning(true);
                serverSocket = new ServerSocket(port);
                myActivity.setState("服务器准备就绪");
                System.out.println("服务器准备就绪");

                clientSocket = serverSocket.accept();
                myActivity.setState("已经连接上客户端");
                System.out.println("已经连接上客户端");

            read = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            print = new PrintStream(clientSocket.getOutputStream());
            String message = null;
            while (!stop)
            {
                while ((message = read.readLine())!=null)
                {
                    System.out.println(message);
                    if (myActivity!=null)
                    {
                        myActivity.setOrder(message);
                    }
                }
            }

            serverSocket.close();
            clientSocket.close();
            myActivity.setIsRunning(false);
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}

