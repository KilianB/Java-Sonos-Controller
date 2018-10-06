package com.github.kilianB.example.localFilePlayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Kilian
 *
 */
public class LocalTrackModel extends RecursiveTreeObject<LocalTrackModel> implements Serializable {

	transient SimpleStringProperty title;
	transient SimpleStringProperty path;
	transient SimpleStringProperty album;
	transient SimpleIntegerProperty trackLength;

	/**
	 * @param title
	 * @param path
	 * @param album
	 */
	public LocalTrackModel(String title, String path, String album, int trackLength) {
		super();
		this.title = new SimpleStringProperty(title);
		this.path = new SimpleStringProperty(path);
		this.album = new SimpleStringProperty(album);
		this.trackLength = new SimpleIntegerProperty(trackLength);
	}

	/**
	 * @return the title
	 */
	public SimpleStringProperty getTitle() {
		return title;
	}

	/**
	 * @return the path
	 */
	public SimpleStringProperty getPath() {
		return path;
	}

	/**
	 * @return the album
	 */
	public SimpleStringProperty getAlbum() {
		return album;
	}
	
	
	/**
	 * @return the trackLength
	 */
	public SimpleIntegerProperty getTrackLength() {
		return trackLength;
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		System.out.println("Write object: " + title.get());
		s.defaultWriteObject();
		s.writeUTF(title.get());
		s.writeUTF(path.get());
		s.writeUTF(album.get());
		s.writeInt(trackLength.get());
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		title = new SimpleStringProperty(s.readUTF());
		path = new SimpleStringProperty(s.readUTF());
		album = new SimpleStringProperty(s.readUTF());
		trackLength = new SimpleIntegerProperty(s.readInt());
	}
}
