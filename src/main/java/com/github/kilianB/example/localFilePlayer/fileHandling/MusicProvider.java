package com.github.kilianB.example.player.fileHandling;

import java.util.ArrayList;

import com.github.kilianB.example.player.fileHandling.exception.MusicProviderInternalException;
import com.github.kilianB.example.player.fileHandling.exception.NoSuitableAlbumFound;
import com.github.kilianB.example.player.fileHandling.exception.NoSuitableSongFound;
import com.github.kilianB.example.player.fileHandling.model.Album;
import com.github.kilianB.example.player.fileHandling.model.Song;


/**
 * A music provider supplies modules and devices with links to music ressources. It does not matter if the files are
 * present on 
 * 
 * 
 * Note:
 * Per convention resources <b>should</b> be implementation independendly of the devices on the smart server and not discriminate 
 * certain servicse. IF
 * If certain resources are only available for certain 
 * @author Kilian
 *
 */
public interface MusicProvider{

	ArrayList<Song> getSongsByName(String trackName) throws NoSuitableSongFound, MusicProviderInternalException;

	ArrayList<Song> getSongsByInterpret(String interpretName)
			throws NoSuitableSongFound, MusicProviderInternalException;
	
	ArrayList<Song> getSongsByGenre(String genre) throws NoSuitableSongFound, MusicProviderInternalException;
	
	ArrayList<Album> getAlbumsByName(String albumName) throws NoSuitableAlbumFound, MusicProviderInternalException;

	ArrayList<Album> getAlbumsByInterpret(String interpretName)
			throws NoSuitableAlbumFound, MusicProviderInternalException;
	
	ArrayList<Album> getAlbumsByGenre(String genre) throws NoSuitableAlbumFound, MusicProviderInternalException;

	/**
	 * Return all names of interprets who are within edit distance of the supplied name.
	 * The LevenshteinDistance is used to determine how far individual names are apart
	 * @param name
	 * @param editDistance
	 * @return
	 */
	ArrayList<String> getInterpretNameAprox(String name, int editDistance);

}