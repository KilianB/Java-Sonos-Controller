package com.github.kilianB.example.player.fileHandling.model;

import java.net.URI;

public class Song {
	String title;
	String description;
	URI musicFile;
	
	public Song(String title, String description, URI url) {
		super();
		this.title = title;
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
	
	
}