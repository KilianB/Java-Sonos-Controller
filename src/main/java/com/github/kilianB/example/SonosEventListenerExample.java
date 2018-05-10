package com.github.kilianB.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;

/**
 * This example demonstrates how to use upnp event callbacks to react to changes 
 * of a sonos speaker
 * @author Kilian
 *
 */
public class SonosEventListenerExample {

	public static void main(String[] args) {
		
		try {
			SonosDevice sonos = SonosDiscovery.discoverOne();
			
			System.out.println("Sonos device found: " + sonos.getDeviceName());
			
			sonos.registerSonosEventListener(new SonosEventListener() {

				@Override
				public void volumeChanged(int newVolume) {
					System.out.println("Volume changed: " + newVolume);
				}

				@Override
				public void playStateChanged(PlayState newPlayState) {
					System.out.println("Playstate changed: " + newPlayState);
				}

				@Override
				public void playModeChanged(PlayMode newPlayMode) {
					System.out.println("Playmode changed: " + newPlayMode);
				}

				@Override
				public void trackChanged(TrackInfo currentTrack) {
					System.out.println("Track changed: " + currentTrack);
				}

				@Override
				public void trebleChanged(int treble) {
					System.out.println("Treble changed: " + treble);
				}

				@Override
				public void bassChanged(int bass) {
					System.out.println("Bass changed: " + bass);
				}

				@Override
				public void loudenessChanged(boolean loudness) {
					System.out.println("Loudness changed: " + loudness);
				}

				@Override
				public void avtTransportEvent(AVTransportEvent avtTransportEvent) {
					System.out.println("AVTTransportEvent: " + avtTransportEvent);
				}

				@Override
				public void queueChanged(List<QueueEvent> queuesAffected) {
					System.out.println(Arrays.toString(queuesAffected.toArray(new QueueEvent[0])));
				}

				@Override
				public void sonosDeviceConnected(String deviceName) {
					System.out.println("New sonos device connected: " + deviceName);
				}

				@Override
				public void sonosDeviceDisconnected(String deviceName) {
					System.out.println("New sonos device disconnected: " + deviceName);
				}

				@Override
				public void groupChanged(ArrayList<String> allDevicesInZone) {
					System.out.println("Group changed. " + (allDevicesInZone.size() > 1 ? "grouped" : "solo"));
				}
			});
			
			
		} catch (IOException | SonosControllerException e) {
			e.printStackTrace();
		}
	}

}
