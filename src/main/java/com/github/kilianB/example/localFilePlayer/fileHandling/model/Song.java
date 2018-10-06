package com.github.kilianB.example.localFilePlayer.fileHandling.model;

import java.net.URI;

public class Song {
	String title;
	String description;
	int trackLength;
	URI musicFile;
	Album album;
	
	public Song(String title, int trackLength, String description, URI url) {
		super();
		this.title = title;
		this.trackLength = trackLength;
		this.description = description;
		this.musicFile = url;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public URI getMusicFile() {
		return musicFile;
	}
	public void setMusicFile(URI url) {
		this.musicFile = url;
	}

	/**
	 * @return the trackLength
	 */
	public int getTrackLength() {
		return trackLength;
	}

	/**
	 * @param trackLength the trackLength to set
	 */
	public void setTrackLength(int trackLength) {
		this.trackLength = trackLength;
	}

	/**
	 * @return the album
	 */
	public Album getAlbum() {
		return album;
	}
	
	
	
}