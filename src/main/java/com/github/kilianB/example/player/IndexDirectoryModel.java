package com.github.kilianB.example.player;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;

public class IndexDirectoryModel extends RecursiveTreeObject<IndexDirectoryModel> {

	private static int id = 0;
	
	private ReadOnlyStringWrapper filePath;
	private ReadOnlyIntegerWrapper songsFound;
	private ReadOnlyIntegerWrapper  lastIndexed;
	private ReadOnlyIntegerWrapper  directoryId = new ReadOnlyIntegerWrapper (id++);
	
	public IndexDirectoryModel(String filePath, int songsFound, int lastIndexed) {
		this.filePath = new ReadOnlyStringWrapper(filePath);
		this.songsFound = new ReadOnlyIntegerWrapper(songsFound);
		this.lastIndexed = new ReadOnlyIntegerWrapper(lastIndexed);
	}


	public ReadOnlyStringProperty getFilePath() {
		return filePath.getReadOnlyProperty();
	}

	public ReadOnlyIntegerProperty getSongsFound() {
		return songsFound.getReadOnlyProperty();
	}

	public ReadOnlyIntegerProperty getDirectoryId() {
		return directoryId.getReadOnlyProperty();
	}
	
	public ReadOnlyIntegerProperty getLastIndexed() {
		return lastIndexed.getReadOnlyProperty();
	}
	
	
	
}
