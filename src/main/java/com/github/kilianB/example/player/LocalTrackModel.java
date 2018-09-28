package com.github.kilianB.example.player;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.SimpleStringProperty;

/**
 * @author Kilian
 *
 */
public class LocalTrackModel extends RecursiveTreeObject<LocalTrackModel> {
	
	SimpleStringProperty title; 
	SimpleStringProperty path;
	SimpleStringProperty album;
	/**
	 * @param title
	 * @param path
	 * @param album
	 */
	public LocalTrackModel(String title, String path, String album) {
		super();
		this.title = new SimpleStringProperty(title);
		this.path = new SimpleStringProperty(path);
		this.album = new SimpleStringProperty(album);
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
	
	
	
	
}
