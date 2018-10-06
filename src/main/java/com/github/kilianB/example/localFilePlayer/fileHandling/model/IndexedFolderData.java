package com.github.kilianB.example.localFilePlayer.fileHandling.model;

import java.sql.Timestamp;

/**
 * @author Kilian
 *
 */
public class IndexedFolderData {
	
	String folderPath;
	Timestamp lastIndexed;
	int tracksIndexed;
	/**
	 * @param folderPath
	 * @param lastIndexed
	 * @param tracksIndexed
	 */
	public IndexedFolderData(String folderPath, Timestamp lastIndexed, int tracksIndexed) {
		super();
		this.folderPath = folderPath;
		this.lastIndexed = lastIndexed;
		this.tracksIndexed = tracksIndexed;
	}
	/**
	 * @return the folderPath
	 */
	public String getFolderPath() {
		return folderPath;
	}
	/**
	 * @return the lastIndexed
	 */
	public Timestamp getLastIndexed() {
		return lastIndexed;
	}
	/**
	 * @return the tracksIndexed
	 */
	public int getTracksIndexed() {
		return tracksIndexed;
	}
	
	
}
