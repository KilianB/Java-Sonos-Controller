[ ![Download](https://api.bintray.com/packages/kilianb/maven/Java-Sonos-Controller/images/download.svg) ](https://bintray.com/kilianb/maven/Java-Sonos-Controller/_latestVersion)

# Sonos-controller
Java API for controlling [SONOS](http://www.sonos.com/) players. 

<p align= "center">
<img src ="http://blog.vmichalak.com/wp-content/uploads/2017/01/SONOS_controller_header.png" />
</p>


## Available via Bintray and JCenter

Starting with version 2.0.0 at least Java 10 is required.

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
	<version>2.0.0</version>
	<type>pom</type>
</dependency>
```

## Basic Usage

Discovery all Sonos Devices on your network.

```java
List<SonosDevice> devices = SonosDiscovery.discover();

//Asynchronous
SonosDiscovery.discoverAsynch(1, device ->{
});

```

Connect to a known Sonos and pause currently playing music.

```java
SonosDevice sonos = new SonosDevice("10.0.0.102");
sonos.pause();
```

## UPnP Event Handling

Register event handlers to gain immediate access to update events

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


## More examples

### 1. Text to speech playback on any sonos speakers

A small example utilizing my <a href="https://github.com/KilianB/GoogleTranslatorTTS">prototyping text to speech library</a>. 
The generated mp3 file is hosted on the current machine to make it accessible to the sonos speakers on the network.

<a href="src/main/java/com/github/kilianB/example/voiceToTextPlayback">Source</a>

![texttospeech](https://user-images.githubusercontent.com/9025925/46544392-becbb800-c8c3-11e8-90d8-945bf1e3880d.jpg)

### 2. Simple Sonos Desktop Player With Local File Playback

A basic player allowing to playback local music files. Index any folder on your computer, create a track index in a SQL 
table and make the folders accessible to the network. While you are able to start stop, playback change volume etc,
this is just intended to lay out the steps needed to implement local music file playback and not function as a standalone application.

<ol>
	<li><a href="src/main/java/com/github/kilianB/example/localFilePlayer/fileHandling/MusicFileIndexer.java">Directory Crawler & File Indexer</a></li>
	<li><a href="src/main/java/com/github/kilianB/example/localFilePlayer/fileHandling/DatabaseManager.java">SQL Database</a></li>
	<li><a href="src/main/java/com/github/kilianB/example/localFilePlayer/fileHandling/NetworkFileProvider.java">File Hosting</a></li>
</ol>

![sonosspeaker](https://user-images.githubusercontent.com/9025925/46569592-b8871b80-c957-11e8-9095-d4310b4c977b.jpg)



## Original contribution

A fork of the tremendous sonos controller library originally created by <a href="https://github.com/vmichalak/sonos-controller">Valentin Michalak</a>. 

Based upon this changes include:
<ol>
	<li>Implementing UPnP event subscriptions</li>
	<li>Swapping out gradle</li>
	<li>License change to GPLv3</li>
</ol>

This repository allows to subscribe to the <a href="http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP 1.1 Event</a> endpoints enabling to receive continious updates of the devices state. I decided to fork the project instead of issuing a pull request due to the need of it being hosted via maven central within a (short) period of time. A huge portion of the code was being rewritten resulting in breaking changes and no backward compatibility If you look for a MIT version or high test coverage either contact me or take a look at the original repository. 

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
