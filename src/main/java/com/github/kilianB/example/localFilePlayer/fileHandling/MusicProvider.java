package com.github.kilianB.example.localFilePlayer.fileHandling;

import java.util.ArrayList;
import java.util.List;

import com.github.kilianB.example.localFilePlayer.fileHandling.exception.MusicProviderInternalException;
import com.github.kilianB.example.localFilePlayer.fileHandling.exception.NoSuitableAlbumFound;
import com.github.kilianB.example.localFilePlayer.fileHandling.exception.NoSuitableSongFound;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Album;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Song;


/**
 * A music provider supplies modules and devices with links to music resources. It does not matter if the files are
 * located on the network, a local device or the internet. <p>
 * 
 * <b>Note:</b>
 * Per convention resources <b>should</b> be implementation independently of the devices on the smart server and not discriminate 
 * certain services. If certain resources are only available for certain devices (e.g. Sonos) the provider shall ensure a lower 
 * priority when registering to the music manager module.
 * @author Kilian
 *
 */
public interface MusicProvider{

	
	/**
	 * 
	 * @return
	 * @throws MusicProviderInternalException
	 */
	
	List<Album> getAllAlbums() throws MusicProviderInternalException;
	/**
	 * 
	 * @param trackName
	 * @return
	 * @throws NoSuitableSongFound
	 * @throws MusicProviderInternalException
	 */
	List<Song> getSongsByName(String trackName) throws NoSuitableSongFound, MusicProviderInternalException;

	List<Song> getSongsByInterpret(String interpretName)
			throws NoSuitableSongFound, MusicProviderInternalException;
	
	List<Song> getSongsByGenre(String genre) throws NoSuitableSongFound, MusicProviderInternalException;
	
	List<Album> getAlbumsByName(String albumName) throws NoSuitableAlbumFound, MusicProviderInternalException;

	List<Album> getAlbumsByInterpret(String interpretName)
			throws NoSuitableAlbumFound, MusicProviderInternalException;
	
	List<Album> getAlbumsByGenre(String genre) throws NoSuitableAlbumFound, MusicProviderInternalException;

	/**
	 * Return all names of interprets who are within edit distance of the supplied name.
	 * The LevenshteinDistance is used to determine how far individual names are apart
	 * @param name
	 * @param editDistance
	 * @return
	 */
	ArrayList<String> getInterpretNameAprox(String name, int editDistance) throws MusicProviderInternalException;

}