
<p align= "center">
<img src ="http://blog.vmichalak.com/wp-content/uploads/2017/01/SONOS_controller_header.png" />
</p>
A fork of the tremendous sonos controller library originally created by <a href="https://github.com/vmichalak/sonos-controller">Valentin Michalak</a>. This repository allows to subscribe to the <a href="http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP 1.1 Event</a> endpoints enabling to receive continious updates of the devices state.

# sonos-controller
Java API for controlling [SONOS](http://www.sonos.com/) players.

## Available via Bintray and JCenter

```
<repositories>
	<repository>
		<id>jcenter</id>
		<url>https://jcenter.bintray.com/</url>
	</repository>
</repositories>

<dependency>
	<groupId>github.com.kilianB</groupId>
	<artifactId>sonos-controller</artifactId>
	<version>1.0.0</version>
	<type>pom</type>
</dependency>
```

## Basic Usage

Discovery all Sonos Devices on your network.

```java
List<SonosDevice> devices = SonosDiscovery.discover();
```

Connect to a known Sonos and pause currently playing music.

```java
SonosDevice sonos = new SonosDevice("10.0.0.102");
sonos.pause();
```

## UPnP Event Handling

Register event handlers to gain immediate access update events

```java
sonos.registerSonosEventListener(new SonosEventAdapter() {
	
	@Override
	public void volumeChanged(int newVolume) {
		System.out.println("Volume changed: " + newVolume);
	}

	@Override
	public void playStateChanged(PlayState newPlayState) {
		System.out.println("Playstate changed: " + newPlayState);
	}

	@Override
	public void trackChanged(TrackInfo currentTrack) {
		System.out.println("Track changed: " + currentTrack);
	}
}
```

Gain full access by utilizing the entire range of callback methods found in the [SonosEventListener.java](https://github.com/KilianB/Java-Sonos-Controller/blob/master/src/main/java/com/github/kilianB/sonos/listener/SonosEventListener.java).



## Why the fork?

Changes include
<ol>
	<li>Implementing UPnP event subscriptions</li>
	<li>Swapping out gradle</li>
	<li>Adding support for log4j2</li>
	<li>License change to GPLv3</li>
</ol>

I decided to fork the project instead of issuing a pull request due to the need of it being hosted on maven central within a short period of time. A huge portion of the code was being rewritten resulting in breaking changes and no backward compatibility If you look for a MIT version or high test coverage either contact me or take a look at the original repository. 

## Up next

Investigate the UPnP event endpoints.

<ul>
<li>/MediaServer/ConnectionManager/Event</li>
<li>/MediaRenderer/ConnectionManager/Event</li>
<li>/MediaServer/ContentDirectory/Event</li>
<li>/AlarmClock/Event</li>
<li>/MusicServices/Event</li>
<li>/SystemProperties/Event</li>
</ul>


How are topology changes best are tracked? (New device found  / device disconnected)
Currently the library utilizes the `/ZoneGroupTopology/Event` as does the official client.
But updates are delayed as much as two minutes.
A different approach would be to either:

<ul>
	<li>Timeout if no upnp advertisement is send within a certain timeframe?</li>
	<li>Parse topology page of a sonos controler e.g. http://192.168.178.26:1400/status/topology  - polling ....</li>
</ul>
