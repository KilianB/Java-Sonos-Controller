package com.github.kilianB.example.player.fileHandling.model;

import java.io.File;
import java.util.ArrayList;

public class Album {
	
	String title;
	String description;
	File albumCovert;
	Interpret interpret;
	
	//TODO maybe extend the audio file class by our own class to add addditional information besides the tags.
	ArrayList<Song> tracks;
	
	public Album(String title, String description, File albumCovert, Interpret interpret, ArrayList<Song> tracks) {
		super();
		this.title = title;
		this.description = description;
		this.albumCovert = albumCovert;
		this.interpret = interpret;
		this.tracks = tracks;
	}
	
	public void addTrack(Song audioFile) {
		tracks.add(audioFile);
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

	public File getAlbumCovert() {
		return albumCovert;
	}

	public void setAlbumCovert(File albumCovert) {
		this.albumCovert = albumCovert;
	}

	public Interpret getInterpret() {
		return interpret;
	}

	public void setInterpret(Interpret interpret) {
		this.interpret = interpret;
	}

	public ArrayList<Song> getTracks() {
		return tracks;
	}

	public void setTracks(ArrayList<Song> tracks) {
		this.tracks = tracks;
	}
}