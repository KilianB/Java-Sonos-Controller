package com.github.kilianB.example;

import java.io.IOException;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.model.TrackMetadata;

/**
 * This example demonstrates basic functions like discovering a sonos speaker, playing back a radio station 
 * grabbing information from the device and pausing the playback after 10 seconds
 * @author Kilian
 *
 */
public class SonosControlExample {
	
	private static final String bbcURI  = "x-rincon-mp3radio://http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lrldn_mf_p"; 
	
	public static void main(String[] args){

		try {		
		//Discover a random sonos device
		SonosDevice sonos = SonosDiscovery.discoverOne();
		
		//Alternatively discover a device with specific name
		//SonosDevice sonos = SonosDiscovery.discoverByName("ZoneName");

		//Radio channels do not feature album or artists. We don't supply an image therefore null
		TrackMetadata trackMeta = new TrackMetadata("BBC Set By Java",null,null,null,null);

		//Start playback
		sonos.playUri(bbcURI,trackMeta);
		
		//Retrieve some information about the current track
		System.out.println(sonos.getCurrentTrackInfo());
		
		//Sleep for 10 seconds
		Thread.sleep(10000);

		sonos.pause();
		}catch(IOException | SonosControllerException | InterruptedException io) {
			io.printStackTrace();
		}	
	}
}
