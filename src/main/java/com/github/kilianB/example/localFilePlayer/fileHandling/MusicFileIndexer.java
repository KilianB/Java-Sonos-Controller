package com.github.kilianB.example.player.fileHandling;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.commons.text.similarity.LevenshteinDistance;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.SimpleResultSet;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.github.kilianB.example.player.fileHandling.exception.MusicProviderInternalException;
import com.github.kilianB.example.player.fileHandling.exception.NoSuitableAlbumFound;
import com.github.kilianB.example.player.fileHandling.exception.NoSuitableSongFound;
import com.github.kilianB.example.player.fileHandling.model.Album;
import com.github.kilianB.example.player.fileHandling.model.Interpret;
import com.github.kilianB.example.player.fileHandling.model.Song;
import com.github.kilianB.example.player.util.StringUtil;

/**
 * Retrieve local/network saved music files by building a searchable index. The
 * meta information is saved in an embedded database for persistent storage and
 * can be queried using SQL by interpret title and album.
 * 
 * <h2>Indexing music files</h2> Necessary information are parsed by reading the
 * ID tags of the music file which means that:
 * </p>
 * <ol>
 * <li>Files have to be properly ID tagged to appear in the database</li>
 * <li>First time indexing is expensive as all files have to be searched for tag
 * information</li>
 * </ol>
 * 
 * <p>
 * If you want to change the indexing logic e.g. extract all necessary
 * information from the filename <code>'Interpret- Title- Album'</code> override
 * the method {@link #extractAudioFileInformation(File)}
 * </p>
 * 
 * <h3>Soft Crawl <-> Hard Crawl</h3> </br>
 * <b>Hard crawling</b> deletes all saved information and re-indexes the entire
 * folder and file structure. This operation has to be performed at least once
 * to build a full functional database. </br>
 * <b>Soft crawls</b> try to determine the delta between the database model and
 * the actual file structure resulting in an immense speedup while still being
 * able to find newly added files.
 * <p>
 * Invoke crawls by calling {@link #forceCrawl(boolean)} or
 * {@link #forceCrawlAsynch(boolean)}
 * 
 * <table>
 * <tr>
 * <th></th>
 * <th>Soft Crawl</th>
 * <th>Hard Crawl</th>
 * </tr>
 * <tr>
 * <td>Indexing*</td>
 * <td>16 seconds</td>
 * <td>5 minutes</td>
 * </tr>
 * <tr>
 * <td>Adding new files</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>Aware of Tag edits</td>
 * <td>No</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>Aware of file deletion</td>
 * <td>No</td>
 * <td>Yes</td>
 * </tr>
 * </table>
 *
 * *(8235 music files) indexed over local network from a NAS-Server
 * <h3>Implementation specific notes</h3>
 * <ul>
 * <li>The crawler can detect cyclic folder structures and avoids deadlocks</li>
 * <li>A soft crawl checks for changes in the absolute path. If only tags are
 * edited the information will not be changed in the database</li>
 * <li>Soft crawling does not delete entries in the database if files are not
 * present anymore. If retrieved by a query the missing files will be detected
 * and skipped*</li>
 * <li>Trying to increase crawl speed I implemented a hashing approach to
 * complement my own folder structure:
 * <ol>
 * <li>Create a hash out of all file names present in the directory</li>
 * <li>If the hash changed to last itteration look at the individual files else
 * skip</li>
 * </ol>
 * Even in the best case with every single album the tradeoff between computing
 * the hash and checking the database evens out with checking each individual
 * file therefore this part will most likely be deleted in the future.</li>
 * </ul>
 * 
 * <h3>Big TODO List</h3>
 * <p>
 * TODO:* we might as well delete the entry if the files are not there anymore.
 * Issues arise if network folders are not present at certain times which would
 * lead to a potential disastrous re-indexing effort. Either check if the entire
 * folder is not reachable and skip or re-index the entire file system once
 * every half year. (Stats about how many files are available will be skewed)
 * </p>
 * <p>
 * TODO: Indexing takes place at the construction of this object therefore a
 * singleton approach sounds reasonable. Maybe even make all modules singletons.
 * </p>
 * <p>
 * TODO: Changing the content of a file does not result in re-indexing meaning
 * that the file can still be accessed using the old meta tags. This is fine if
 * only the content of the file changes. e.g. parts of the file get truncated,
 * semantic changes (the file with the name "HelloWorld.mp3" has a different
 * author) are not caught by the indexing due to the way the hashes are computed
 * / file presence (file path) are checked
 * </p>
 * <p>
 * TODO create n:n relationship between interpret and album. Currently only one
 * interpret / album
 * </p>
 * <p>
 * TODO read tags for order of songs in the album..
 * </p>
 * <p>
 * TODO Hook a file analyzing api / or simply lookup based on filename and
 * interpret to retrieve more meta information about files. e.g. GENRE.
 * </p>
 * 
 * @author Kilian
 *
 */
public class MusicFileIndexer implements MusicProvider {

	private DatabaseManager database;
	private HashMap<Integer, String> directoryHashset = new HashMap<>();

	/**
	 * Be aware that calling this method the first time will result in the file
	 * indexing which can take several minutes. Subsequent calls are greatly reduced
	 * and just check for differences in file names.
	 * 
	 * @param allowedFileExtensions
	 * @param directoriesToIndex    Currently the path is case sensitive in the
	 *                              sense that files will get indexed twice if the
	 *                              directory changes.
	 * @param softCrawl             automatically crawls (re-indexes) the files if
	 *                              they are missing. This will give you access to
	 *                              newly created files but take a few seconds. If
	 *                              you have not altered (added or deleted files)
	 *                              there is no need to waste peformance for a
	 *                              crawl. The initial index might take a long time.
	 */
	public MusicFileIndexer(String[] allowedFileExtensions) {

		allowedFileTypes = new ArrayList<>(Arrays.asList(allowedFileExtensions));

		// Setup database to store music
		database = new DatabaseManager();

		// Crawl directories
//		if (softCrawl) {
//			forceCrawlAsynch(false);
//		}
	}

	// TODO maybe use proter stemmer, editdistance and wildcard matching etc ..
	/*
	 * Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14, no. 3, pp
	 * 130-137
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * modules.musicProvider.MusicProvider#getInterpretNameAprox(java.lang.String,
	 * int)
	 */
	@Override
	public ArrayList<String> getInterpretNameAprox(String name, int editDistance) {

		// TODO not thread save... and inefficient. The distance for each object is
		// calculated at least 3 times.
		try {
			database.wordThreshold = editDistance;
			database.retrieveInterpretNameAprox.setString(1, name);
			ArrayList<String> interpretNames = new ArrayList<>();

			ResultSet rs = database.retrieveInterpretNameAprox.executeQuery();

			while (rs.next()) {
				interpretNames.add(rs.getString(2));
			}

			interpretNames.sort(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					LevenshteinDistance l = new LevenshteinDistance();
					int d1 = l.apply(o1, name);
					int d2 = l.apply(o2, name);
					if (d1 > d2) {
						return 1;
					} else if (d1 < d2) {
						return -1;
					}
					return 0;
				}

			});

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public ArrayList<String> getIndexedFolders() throws MusicProviderInternalException {
		try {
			ResultSet rs = database.retrieveIndexedFolders.executeQuery();
			ArrayList<String> indexedFolders = new ArrayList<>();
			while (rs.next()) {
				indexedFolders.add(rs.getString("FOLDERPATH"));
			}
			return indexedFolders;
		} catch (SQLException e) {
			throw new MusicProviderInternalException("SQL or URI Syntax exception");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see modules.musicProvider.MusicProvider#getAlbumsByName(java.lang.String)
	 */
	@Override
	public synchronized ArrayList<Album> getAlbumsByName(String albumName)
			throws NoSuitableAlbumFound, MusicProviderInternalException {

		try {
			database.retrieveAlbumIDByName.setString(1, albumName);
			ResultSet rs = database.retrieveAlbumIDByName.executeQuery();

			ArrayList<Album> albums = new ArrayList<>();

			while (rs.next()) {
				int albumID = rs.getInt(1);
				String title = rs.getString("TITLE");
				String description = rs.getString("DESCRIPTION");
				String albumCoverPath = rs.getString("ALBUMCOVER");
				int interpretID = rs.getInt("INTERPRET_ID");

				File albumCover = albumCoverPath == null ? null : new File(albumCoverPath);

				Interpret interpret;
				// Retrieve the interpret
				if (interpretID > 0) {
					database.retrieveInterpretByID.setInt(1, interpretID);
					ResultSet interpretQuery = database.retrieveInterpretByID.executeQuery();

					if (interpretQuery.next()) {

						interpret = new Interpret(interpretQuery.getString("NAME"), interpretQuery.getString("BIO"),
								interpretQuery.getDate("BIRTHYEAR"));
					} else {
						interpret = Interpret.createUnknownInterpret();
					}
				} else {
					interpret = Interpret.createUnknownInterpret();
				}

				// Retrieve the files
				database.retrieveSongsOfAlbumByAlbumID.setInt(1, albumID);
				ResultSet songQuery = database.retrieveSongsOfAlbumByAlbumID.executeQuery();

				ArrayList<Song> audioFiles = new ArrayList<>();

				while (songQuery.next()) {
					// We don't need the title as it's already in the tag..? Or do we want to
					// differentiate between
					// Database Title and Tag title?
					String songTitle = songQuery.getString("TITLE");
					String songDescription = songQuery.getString("DESCRIPTION");
					String filePath = songQuery.getString("URL");
					URI songFile = filePath == null ? null : new URI(filePath);
					// Which should actually fail ...
					if (filePath == null || !new File(filePath).exists()) {
						continue;
						// TODO clean up work, delete file from database.
					}
					audioFiles.add(new Song(songTitle, songDescription, songFile));
				}

				albums.add(new Album(title, description, albumCover, interpret, audioFiles));
			}
			if (albums.isEmpty()) {
				throw new NoSuitableAlbumFound("Local Network Music Provider could not find an album for " + albumName);
			} else {
				return albums;
			}

		} catch (SQLException | URISyntaxException e) {
			throw new MusicProviderInternalException("SQL or URI Syntax exception");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see modules.musicProvider.MusicProvider#getSongsByName(java.lang.String)
	 */
	@Override
	public synchronized ArrayList<Song> getSongsByName(String trackName)
			throws NoSuitableSongFound, MusicProviderInternalException {

		ArrayList<Song> songs = new ArrayList<>();

		try {
			database.retrieveSongsByName.setString(1, trackName);
			ResultSet songQuery = database.retrieveSongsByName.executeQuery();

			while (songQuery.next()) {
				String songTitle = songQuery.getString("TITLE");
				String songDescription = songQuery.getString("DESCRIPTION");
				String filePath = songQuery.getString("URL");
				String uri = StringUtil.decodeURLToURI(filePath);
				URI songFile = filePath == null ? null : new URI(uri);
				// Which should actually fail ...
				if (filePath == null || !new File(filePath).exists()) {
					continue;
					// TODO clean up work, delete file from database.
				}
				songs.add(new Song(songTitle, songDescription, songFile));

			}

			if (songs.isEmpty()) {
				throw new NoSuitableSongFound(
						"Local Network Music Provider could not find a suitable song for " + trackName);
			} else {
				return songs;
			}
		} catch (SQLException | URISyntaxException e) {
			e.printStackTrace();
			throw new MusicProviderInternalException("SQL or URI Syntax Exception");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * modules.musicProvider.MusicProvider#getSongsByInterpret(java.lang.String)
	 */
	@Override
	public ArrayList<Song> getSongsByInterpret(String interpretName)
			throws NoSuitableSongFound, MusicProviderInternalException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * modules.musicProvider.MusicProvider#getAlbumsByInterpret(java.lang.String)
	 */
	@Override
	public ArrayList<Album> getAlbumsByInterpret(String interpretName)
			throws NoSuitableAlbumFound, MusicProviderInternalException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see modules.musicProvider.MusicProvider#getSongsByGenre(java.lang.String)
	 */
	@Override
	public ArrayList<Song> getSongsByGenre(String genre) throws NoSuitableSongFound, MusicProviderInternalException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see modules.musicProvider.MusicProvider#getAlbumsByGenre(java.lang.String)
	 */
	@Override
	public ArrayList<Album> getAlbumsByGenre(String genre) throws NoSuitableAlbumFound, MusicProviderInternalException {
		throw new UnsupportedOperationException();
	}

	/*
	 * -----------------------------------------------------------------------------
	 * ---------------------------------+
	 * ----------------------------------------------- Crawling
	 * -----------------------------------------------------|
	 * -----------------------------------------------------------------------------
	 * ---------------------------------+
	 */
	Stack<File> directoryToSearch = new Stack<>(); // Directories which still need to be crawled //already syncronized
	ArrayList<String> allowedFileTypes;

	public void forceCrawlAsynch(File directory, Runnable callback) {
		directoryToSearch.add(directory);
		new Thread(() -> forceCrawl(false,callback)).start();
	}

	/**
	 * Blocks until crawling is completed.
	 * 
	 * @param reIndex If true all entries in the database will be deleted and files
	 *                are reindexed from scratch;
	 * @param callback 
	 */
	public void forceCrawl(boolean reIndex, Runnable callback) {

		if (reIndex) {
			database.resetDB();
		}

		System.out.println("Start crawling");
		crawlDirectory();

		// Remove all hashes which are NOT in directory hash
		database.cleanUpDirectoryHash(directoryHashset);

		if(callback != null){
			callback.run();
		}
	}

	private void crawlDirectory() {

		// TODO a fork join pool is the right call for this!
		int threadCount = 1; // Runtime.getRuntime().availableProcessors() * 2;

		// We do have io requests
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		ArrayList<Future<File>> callback = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			Future future = executor.submit(() -> {
				File currentDirectory = null;

				while (true) {
					currentDirectory = retrieveFileSynch(directoryToSearch);
					System.out.println("Cur Dir: " + currentDirectory);
					if (currentDirectory == null) {
						break;
					}
					// Does this ever return directories or only files?
					File[] children = currentDirectory.listFiles();

					/*
					 * Compute hash of the filenames residing in this directory. Computing the hash
					 * allows us to skip cyclic folder structures and to skip over directories which
					 * were already indexed during an earlier run and no file changed within.
					 */

					StringBuilder sb = new StringBuilder();
					for (File f : children) {
						sb.append(f.getName());
					}
					int hash = sb.toString().hashCode();

					// Skip cyclic folder structures
					if (directoryHashset.containsKey(hash))
						continue;

					// Remember we have seen this folder already
					directoryHashset.put(hash, currentDirectory.getAbsolutePath());

					// Did we look at the folder on an earlier indexing run already?
					boolean hashExists = database.doesDirectoryHashExist(hash);

					for (File f : children) {
						if (f.isDirectory()) {
							// If it's a directory on it's own save it for later usage
							directoryToSearch.add(f);
						} else {

							// Directories are still pushed on the stack to ensure that we are able to check
							// every single hash.
							// But if the hash is consistent we are sure that the files did not change
							// therefore we can skip them
							if (hashExists) {
								continue;
							}
							// Is the file a music file we wan to process?

							String extension = StringUtil.getFileExtension(f);

							if (f.getName().startsWith(".")) {
								// Ignore the file;
								continue;
							}

							if (allowedFileTypes.contains(extension)) {

								try {

									// Is the file is already indexed we don't have to look into it anymore
									// Creating an AudioFileIO.read operation is expensive and by querying the
									// database
									// beforehand we save

									if (database.doesTrackExist(f.getAbsolutePath()) == -1) {

										System.out.println(f + "Track");
										AudioFile audioFile = AudioFileIO.read(f);

										String title;
										String artistName;

										Tag audioTag = audioFile.getTag();

										// We don't have much to work with. NO ID3 Tags
										if (audioTag == null) {
											title = f.getName();
											artistName = "Unknown";
										} else {
											title = audioTag.getFirst(FieldKey.TITLE);
											artistName = audioTag.getFirst(FieldKey.ARTIST);
										}

										int interpretID = database.insertInterpret(artistName, null, null);
										int trackID = database.insertTrack(title, null, f.getAbsolutePath());

										if (audioTag != null) {
											try {
												String albumName = audioTag.getFirst(FieldKey.ALBUM);
												int albumID = database.insertAlbum(albumName, null, null, interpretID);
												database.insertAlbumTrack(albumID, trackID);
											} catch (KeyNotFoundException e) {

											}
										}
									}
								} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
										| InvalidAudioFrameException | KeyNotFoundException | SQLException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			});
			callback.add(future);
		}
		;

		// Finish executor service. Maybe be non blocking and simply respond to the
		// program once it is done indexing?
		for (Future f : callback) {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		executor.shutdown();
	}

	private synchronized File retrieveFileSynch(Stack<File> stack) {
		if (!stack.isEmpty()) {
			return stack.pop();
		}
		return null;
	}

	// Return approximate results. levenshtein distance

	private static final Logger LOGGER = Logger.getLogger(MusicFileIndexer.class.getName());

}