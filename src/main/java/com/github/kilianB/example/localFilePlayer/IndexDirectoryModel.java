package com.github.kilianB.example.localFilePlayer;

import java.time.LocalDateTime;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

public class IndexDirectoryModel extends RecursiveTreeObject<IndexDirectoryModel> {

	private static int id = 0;
	
	private ReadOnlyStringWrapper filePath;
	private ReadOnlyIntegerWrapper songsFound;
	private ReadOnlyObjectWrapper<LocalDateTime>  lastIndexed;
	private ReadOnlyIntegerWrapper  directoryId = new ReadOnlyIntegerWrapper (id++);
	
	public IndexDirectoryModel(String filePath, int songsFound, LocalDateTime lastIndexed) {
		this.filePath = new ReadOnlyStringWrapper(filePath);
		this.songsFound = new ReadOnlyIntegerWrapper(songsFound);
		this.lastIndexed = new ReadOnlyObjectWrapper<>(lastIndexed);
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
	
	public ReadOnlyObjectProperty<LocalDateTime> getLastIndexed() {
		return lastIndexed.getReadOnlyProperty();
	}
	
	
	
}
