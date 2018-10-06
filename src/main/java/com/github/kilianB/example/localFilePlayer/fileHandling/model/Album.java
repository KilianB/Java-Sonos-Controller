package com.github.kilianB.example.localFilePlayer.fileHandling.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An album represents a collection of songs.
 * 
 * @author Kilian
 *
 */
public class Album {

	String title;
	String description;
	File albumCovert;
	Interpret interpret;

	// TODO maybe extend the audio file class by our own class to add addditional
	// information besides the tags.
	List<Song> tracks;

	public Album(String title, String description, File albumCovert, Interpret interpret, List<Song> tracks) {
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

	public List<Song> getTracks() {
		return tracks;
	}

	public void setTracks(ArrayList<Song> tracks) {
		this.tracks = tracks;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((albumCovert == null) ? 0 : albumCovert.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((interpret == null) ? 0 : interpret.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((tracks == null) ? 0 : tracks.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Album other = (Album) obj;
		if (albumCovert == null) {
			if (other.albumCovert != null)
				return false;
		} else if (!albumCovert.equals(other.albumCovert))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (interpret == null) {
			if (other.interpret != null)
				return false;
		} else if (!interpret.equals(other.interpret))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (tracks == null) {
			if (other.tracks != null)
				return false;
		} else if (!tracks.equals(other.tracks))
			return false;
		return true;
	}

}