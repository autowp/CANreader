package com.autowp.can.adapter.udp;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.adapter.canhacker.CanHacker;
import com.autowp.can.adapter.canhacker.CanHackerException;
import com.autowp.can.adapter.canhacker.command.Command;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;

public class CanHackerUdp extends CanHacker {

    public static int PORT = 11111;

    private String mHostname = "127.0.0.1";
    private int mPort = PORT;
    private DatagramSocket mSocket = null;
    private InetAddress mIPAddress = null;

    public CanHackerUdp(CanBusSpecs specs) {
        super(specs);
    }

    public CanHackerUdp setPort(int port) {
        mPort = port;
        
        return this;
    }
    
    public CanHackerUdp setHostname(String hostname) {
        mHostname = hostname;
        
        return this;
    }

    protected synchronized void doConnect() throws CanHackerException {
        System.out.println("UDP.doConnect()");
        try {
            DatagramChannel channel = DatagramChannel.open();
            mSocket = channel.socket();
            //mSocket = new DatagramSocket(mPort);
            mIPAddress = InetAddress.getByName(mHostname);
            SocketAddress socketAddress = new InetSocketAddress(mPort);
            mSocket.bind(socketAddress);

            System.out.print("mSocket = ");
            System.out.println(mSocket);

            System.out.println("super");

            try {
                super.doConnect();
                System.out.println("super 2");
            } catch (CanHackerException e) {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                mIPAddress = null;

                e.printStackTrace();

                throw e;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CanHackerUdpException("I/O error: " + e.getMessage());
        }
    }

    protected synchronized void doDisconnect() throws CanAdapterException {
        super.doDisconnect();

        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }

        mIPAddress = null;
    }

    public synchronized CanHackerUdp send(Command c) throws CanHackerUdpException
    {
        if (getConnectionState() == CanAdapter.ConnectionState.DISCONNECTED) {
            throw new CanHackerUdpException("CanHackerUdp is not connected");
        }

        if (mSocket == null || mSocket.isClosed()) {
            throw new CanHackerUdpException("Socket not initialized");
        }
        
        String command = c.toString() + COMMAND_DELIMITER;
        
        try {

            byte[] sendData = command.getBytes("ISO-8859-1");
            mSocket.send(new DatagramPacket(sendData, sendData.length, mIPAddress, mPort));
            
        } catch (IOException e) {
            throw new CanHackerUdpException("I/O error: " + e.getMessage());
        }
                
        return this;
    }

    @Override
    protected synchronized byte[] readBytes(int timeout) {
        try {
            if (connectionState == CanAdapter.ConnectionState.DISCONNECTED) {
                System.err.println("CanHackerUdp is not connected");
                return null;
            }
            if (mSocket == null) {
                System.err.println("readBytes: socket is null");
                return null;
            }
            mSocket.setSoTimeout(10000);
            byte[] readBuffer = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(readBuffer, readBuffer.length);
            mSocket.receive(receivePacket);
            mSocket.setSoTimeout(0);

            return receivePacket.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
