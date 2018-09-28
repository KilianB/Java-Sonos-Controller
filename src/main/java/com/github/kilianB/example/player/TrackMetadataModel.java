package com.github.kilianB.example.player;

import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

class TrackMetadataModel extends RecursiveTreeObject<TrackMetadataModel> {

	SimpleStringProperty title;
	SimpleStringProperty album;
	SimpleStringProperty albumArtist;
	SimpleStringProperty albumUri;
	SimpleStringProperty creator;
	SimpleBooleanProperty currentlyPlaying = new SimpleBooleanProperty(false);

	public TrackMetadataModel(TrackMetadata metadata, SonosDevice device) {

		title = new SimpleStringProperty(metadata.getTitle());
		album = new SimpleStringProperty(metadata.getAlbum());
		albumArtist = new SimpleStringProperty(metadata.getAlbumArtist());
		albumUri = new SimpleStringProperty(metadata.getAlbumArtURI());
		creator = new SimpleStringProperty(metadata.getCreator());
	}

	public SimpleStringProperty getTitle() {
		return title;
	}

	public void setTitle(SimpleStringProperty title) {
		this.title = title;
	}

	public SimpleStringProperty getAlbum() {
		return album;
	}

	public void setAlbum(SimpleStringProperty album) {
		this.album = album;
	}

	public SimpleStringProperty getAlbumArtist() {
		return albumArtist;
	}

	public void setAlbumArtist(SimpleStringProperty albumArtist) {
		this.albumArtist = albumArtist;
	}

	public SimpleStringProperty getAlbumUri() {
		return albumUri;
	}

	public void setAlbumUri(SimpleStringProperty albumUri) {
		this.albumUri = albumUri;
	}

	public SimpleStringProperty getCreator() {
		return creator;
	}

	public void setCreator(SimpleStringProperty creator) {
		this.creator = creator;
	}

	public SimpleBooleanProperty getCurrentlyPlayingProperty() {
		return currentlyPlaying;
	}
}