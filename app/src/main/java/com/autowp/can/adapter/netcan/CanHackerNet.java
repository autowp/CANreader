package com.autowp.can.adapter.netcan;

import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrameException;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.command.Command;
import com.autowp.can.adapter.canhacker.response.Response;
import com.autowp.can.adapter.canhacker.response.ResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CanHackerNet extends CanHacker {
    private String mHostname = "127.0.0.1";
    private int mPort = 20100;
    private Socket mSocket = null;

    public CanHackerNet setPort(int port) {
        mPort = port;
        
        return this;
    }
    
    public CanHackerNet setHostname(String hostname) {
        mHostname = hostname;
        
        return this;
    }

    @Override
    public void connect() throws CanAdapterException {
        try {
            mSocket = new Socket(mHostname, mPort);
            
            readerThread.start();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            readerThread.interrupt();
            mSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mSocket = null;
    }

    @Override
    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public synchronized CanHackerNet send(Command c) throws CanHackerNetException
    {
        if (!this.isConnected()) {
            throw new CanHackerNetException("CanHackerNet is not connected");
        }
        
        String command = c.toString() + COMMAND_DELIMITER;
        
        try {
            
            OutputStream out = mSocket.getOutputStream();

            out.write(command.getBytes("ISO-8859-1"));
            out.flush();
            
        } catch (IOException e) {
            throw new CanHackerNetException("I/O error: " + e.getMessage());
        }
                
        return this;
    }
    
    private Thread readerThread = new Thread() {
        public void run() {
            InputStream is;
            try {
                is = mSocket.getInputStream();
                byte[] readBuffer = new byte[2000];
    
                while(true) {
                    int bytesRead = is.read(readBuffer);
                    byte[] readBytes = new byte[bytesRead];
                    System.arraycopy(readBuffer, 0, readBytes, 0, bytesRead);
                    processBytes(readBytes);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
      
}
