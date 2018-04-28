package com.vmichalak.protocol.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vmichalak.sonoscontroller.SonosDevice;
import com.vmichalak.sonoscontroller.exception.SonosControllerException;

/**
 * Client for discovering UPNP devices with SSDP (Simple Service Discovery Protocol).
 */
public class SSDPClient {
    /**
     * Discover any UPNP device using SSDP (Simple Service Discovery Protocol).
     * @param timeout in milliseconds
     * @param serviceType if null it use "ssdp:all"
     * @return List of devices discovered
     * @throws IOException
     * @see <a href="https://en.wikipedia.org/wiki/Simple_Service_Discovery_Protocol">SSDP Wikipedia Page</a>
     */
    public static List<Device> discover(int timeout, String serviceType) throws IOException {
        ArrayList<Device> devices = new ArrayList<Device>();
        byte[] sendData;
        byte[] receiveData = new byte[1024];

        /* Create the search request */
        StringBuilder msearch = new StringBuilder(
                "M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMan: \"ssdp:discover\"\n");
        if (serviceType == null) { msearch.append("ST: ssdp:all\n"); }
        else { msearch.append("ST: ").append(serviceType).append("\n"); }

        /* Send the request */
        sendData = msearch.toString().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(timeout);
        clientSocket.send(sendPacket);

        /* Receive all responses */
        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                devices.add(Device.parse(receivePacket));
            }
            catch (SocketTimeoutException e) { break; }
        }

        clientSocket.close();
        return Collections.unmodifiableList(devices);
    }

    public static Device discoverOne(int timeout, String serviceType) throws IOException {
        Device device = null;
        byte[] sendData;
        byte[] receiveData = new byte[1024];

        /* Create the search request */
        StringBuilder msearch = new StringBuilder(
                "M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMan: \"ssdp:discover\"\n");
        if (serviceType == null) { msearch.append("ST: ssdp:all\n"); }
        else { msearch.append("ST: ").append(serviceType).append("\n"); }

        /* Send the request */
        sendData = msearch.toString().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(timeout);
        clientSocket.send(sendPacket);

        /* Receive one response */
        try {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            device = Device.parse(receivePacket);
        }
        catch (SocketTimeoutException e) { }

        clientSocket.close();
        return device;
        
        /**
         * M-SEARCH * HTTP/1.1
			Host:239.255.255.250:1900
			Man:"ssdp:discover"
			Mx:5
			ST:ssdp:rootdevice
         */
        
    }
    
    //Subscribe to events
    public static void subscribe(String deviceIP,String servicePath) throws IOException {
    
    	/**
    	 *  SUBSCRIBE publisher path HTTP/1.1
    	 *	HOST: publisher host:publisher port
    	 *	USER-AGENT: OS/version UPnP/1.1 product/version
		 *	CALLBACK: <delivery URL>
		 *	NT: upnp:event
    	 */
    	
    	
    	ServerSocket socket = new ServerSocket(600,10,InetAddress.getByName("192.168.178.49"));
    	
    	new Thread(() -> {
    		
    		while(true) {
    			try {
					Socket s = 	socket.accept();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    			System.out.println("New connection request");
    			return;
    		}
    	
    		
    	}).start();
    	
    	
    	String callbackURL = "http:" + socket.getInetAddress()+ ":600";

    	
    	
    	//device.getSpeakerInfo().getIpAddress()
    	
    	 /* Create the search request */
        StringBuilder eventSubscription = new StringBuilder(
                "SUBSCRIBE ").append(servicePath).append(" HTTP/1.1\n")
        		.append("HOST: ").append(deviceIP).append("\n")
        		.append("USER-AGENT: Linux UPnP/1.0 Sonos/22.0-64110 (MDCR_MacBookPro6,2)\n") //TODO 1.1
        		.append("CALLBACK:<").append(callbackURL).append(">\n")
        		.append("NT: upnp:event\n")
        		.append("TIMEOUT: Second-3600\n");
        
        //Debug call
        System.out.println(eventSubscription.toString());
        
        
        
        
        //Socket s = new Socket()
        
        System.out.println("------\n");
        
        
        byte[] message = eventSubscription.toString().getBytes();
        
        //Send to device 
        DatagramPacket sendPacket = new DatagramPacket(
        		message, message.length, InetAddress.getByName("239.255.255.250"), 1900);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(30 * 1000);
        clientSocket.send(sendPacket);

        
        byte[] receiveData = new byte[512];
        /* Receive one response */
        try {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            System.out.println("Response received");
            //Dump packae
            System.out.println(new String(receivePacket.getData()));
        }
        catch (SocketTimeoutException e) {
        	System.out.println("Timeout. No response from device");
        	
        }

        clientSocket.close();
        
    }
    
}
