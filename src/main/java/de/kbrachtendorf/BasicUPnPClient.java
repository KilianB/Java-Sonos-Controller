package de.kbrachtendorf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stripped down implementation of the udp protocol allowing to discover UPnP devices and subscribe to 
 * their services 
 * 
 * @see http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf
 * @author Kilian
 *
 */
public class BasicUPnPClient {

	
	public static void main(String[] args) {
		try {
			new BasicUPnPClient().discoverDevice();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private final static String UPNP_HOST = "239.255.255.250";
	private final static int UPNP_PORT = 1900;
	
	private final static String DISCOVERY_REQUEST = "M-SEARCH * HTTP/1.1\r\n"
												+ "HOST:  "+UPNP_HOST+":"+UPNP_PORT+"\r\n"	//SSDP address
												+ "MAN: \"ssdp:discover\"\r\n"				//HTTP extension framework header
												+ "MX: {mx}\r\n"							//Random delay Allowed values 1 - 5
												+ "ST: ssdp:all\r\n\r\n";					//Search type see documentation
												// + Optional user agent
	
	
	
	
	public void discoverDevice() throws IOException {
		discoverDevice(1,2000,null);
	}

	/**
	 * 
	 * @param timeout
	 * @throws IOException 
	 */
	public void discoverDevice(int loadBalancingDelay, int timeout, String searchTarget) throws IOException {
		
		if(loadBalancingDelay < 1 || loadBalancingDelay > 5) {
			//LOGGER.warn("Load balancing delay should be within [1-5] seconds. A default of 1 is assumed");
			loadBalancingDelay = 1;
		}
		
		String request = DISCOVERY_REQUEST;
		
		if(searchTarget != null && !searchTarget.isEmpty() && !searchTarget.equals("ssdp:all")) {
			request = request.replace("ssdp:all", searchTarget);
		}
		request = DISCOVERY_REQUEST.replace("{mx}", Integer.toString(loadBalancingDelay));
		
		//Create a udp package
		
		byte[] payload = request.getBytes();
		DatagramPacket discoveryRequest = new DatagramPacket(payload,payload.length,new InetSocketAddress(UPNP_HOST,UPNP_PORT));
		
		DatagramSocket udpSocket = new DatagramSocket();
		//devices 
		udpSocket.setSoTimeout(timeout);
		udpSocket.send(discoveryRequest);
		
		//Work with response
		
		
		
		
		long startTime = System.currentTimeMillis();
		
		//Reuse old package
		
		while(true) {
			byte[] response = new byte[512];
			DatagramPacket incommingPacket = new DatagramPacket(response,response.length);
			try {
				udpSocket.receive(incommingPacket);
			}catch(SocketTimeoutException timeouted) {
				break;
			}

			String data = new String(incommingPacket.getData());
			System.out.println(data);
			
			
			//time left until timeout
			int timeLeft = timeout -  (int)(System.currentTimeMillis() - startTime);
			if(timeLeft <= 0) {
				break;
			}
			udpSocket.setSoTimeout(timeLeft);
		}
		
		
		
		System.out.println();
		
		
		//UDP is unreliable maybe retry
		
		
	}
	
	

	public void disoverDevices() {
		
	}
	
	
	private String collectStream(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		String line;
		
		while( (line = br.readLine()) !=null) {
			System.out.println(line);
		}
		
		return line;
	}
	
	
	//TODO test
	private HashMap<String,String> parseUpnpNotifyAndSearchMessage(String messageToParse){
		final Matcher matcher = Pattern.compile("(.*):(.*)").matcher(messageToParse);
	
		final HashMap<String,String> parsedKeyValues = new HashMap<String,String>();
		
		while(matcher.find()) {
			parsedKeyValues.put(matcher.group(0), matcher.group(1));
		}
		return parsedKeyValues;
		
	}
	
	
}
