package com.github.kilianB.example.localFilePlayer.fileHandling;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.IntConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

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
 * The crawler can detect cyclic folder structures and avoids deadlocks.
 * Currently artists are only differentiated by name which might raise ambiguity. 
 * 
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
public class MusicFileIndexer {

	private DatabaseManager database;

	private PathMatcher extensionMatcher;

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
	public MusicFileIndexer(String[] allowedFileExtensions, DatabaseManager database) {

		StringBuilder allowedFilesExtensionBuilder = new StringBuilder();

		for (String extension : allowedFileExtensions) {
			allowedFilesExtensionBuilder.append(extension);
		}

		String fileExtensionPattern = "glob:**/*.{" + Stream.of(allowedFileExtensions).collect(Collectors.joining(","))
				+ "}";

		extensionMatcher = FileSystems.getDefault().getPathMatcher(fileExtensionPattern);

		// Setup database to store music
		this.database = database;

		// Crawl directories
//		if (softCrawl) {
//			forceCrawlAsynch(false);
//		}
	}

	/*
	 * ----------------------------------------------------------------------------+
	 * ----------------------------------------------------------------------------|
	 * --------------------------------- Crawling----------------------------------|
	 * ----------------------------------------------------------------------------|
	 * ----------------------------------------------------------------------------+
	 */

	/**
	 * Crawl the directory and add all found music files to the database. Invalid
	 * entries will be removed
	 * 
	 * @param baseDirectory
	 * @param callback
	 */
	public void crawlAsynch(Path baseDirectory, IntConsumer callback) {
		new Thread(() -> {
			int tracksIndex = crawl(false, baseDirectory);
			if (callback != null) {
				callback.accept(tracksIndex);
			}
		}).start();
	}

	/**
	 * Blocks until crawling is completed.
	 * 
	 * @param reIndex  If true all entries in the database will be deleted and files
	 *                 are reindexed from scratch;
	 * @param callback
	 */
	public int crawl(boolean reIndex, Path baseDirectory) {

		// Delete all entries in the database
		if (reIndex) {
			// NOP. We just reindex present files...
			database.resetDB();
		}

		/**
		 * Common fork join pool. Reuse to save resources
		 */
		ForkJoinPool commonPool = ForkJoinPool.commonPool();

		/**
		 * avoid cyclic folder structure especially with symlinks
		 */
		KeySetView<Path, Boolean> alreadySeen = ConcurrentHashMap.newKeySet();

		try {
			baseDirectory = baseDirectory.toRealPath();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Start crawling: " + baseDirectory);

		try {

			Timestamp startCrawlingTimestamp = new Timestamp(System.currentTimeMillis());

			int directoryId = database.insertIndexedLocation(baseDirectory);
			alreadySeen.add(baseDirectory);
			int titlesIndexed = commonPool.invoke(new CrawlDirectory(baseDirectory, alreadySeen, directoryId));

			/*
			 * 
			 */
			int titlesRemoved = database.deleteTracksIndexedOlder(startCrawlingTimestamp, directoryId);

			System.out.println("Titles indexed: " + titlesIndexed + " Titles removed: " + titlesRemoved);

			return titlesIndexed;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Purge invalid data
		// SELECT * FROM TRACKS WHERE DIRECTORY.ID = directoryId and lastIndexed <
		// startTimestamp

		return 0;
	}

	/**
	 * Insert real path!
	 * 
	 * @author Kilian
	 *
	 */
	class CrawlDirectory extends RecursiveTask<Integer> {

		private static final long serialVersionUID = 5401593123821239010L;

		private int directoryId;
		private Path directory;
		private KeySetView<Path, Boolean> alreadySeen;

		public CrawlDirectory(Path directory, KeySetView<Path, Boolean> alreadySeen, int directoryId) {
			this.directory = directory;
			this.alreadySeen = alreadySeen;
			this.directoryId = directoryId;
		}

		@Override
		protected Integer compute() {

			if (Files.isDirectory(directory)) {

				List<Path> subPaths;
				try {
					subPaths = Files.walk(directory).collect(Collectors.toList());
					List<CrawlDirectory> subtasks = new ArrayList<>();

					for (Path path : subPaths) {
						// Create subtasks
						// resolve symlinks ...
						Path pReal = path.toRealPath();
						if (!alreadySeen.contains(pReal)) {
							subtasks.add(new CrawlDirectory(pReal, alreadySeen, directoryId));
							alreadySeen.add(pReal);
						}
					}
					return ForkJoinTask.invokeAll(subtasks).stream().mapToInt(ForkJoinTask::join).sum();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// Do we want to do the hashing? the hash does not reflect deep changes!
			} else {
				// File
				if (extensionMatcher.matches(directory)) {

					FileTime lastModified = null;
					try {
						lastModified = Files.getLastModifiedTime(directory);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					try {

						int trackId = database.doesTrackExist(directory.toString());

						if (trackId == -1 || database.getTrackLastModified(directory.toString())
								.getTime() != lastModified.toMillis()) {

							File f = directory.toFile();

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

								if (title.isEmpty()) {
									title = "Unknown";
								}

								if (artistName.isEmpty()) {
									artistName = "Unknown";
								}
							}

							int trackLength = audioFile.getAudioHeader().getTrackLength();

							int interpretID = database.insertInterpret(artistName, null, null);

							int trackID = database.insertTrack(title, null, trackLength, directory.toString(),
									DatabaseManager.toSQLTimestamp(lastModified), directoryId);

							String albumName = "Unknown";
							if (audioTag != null) {

								try {
									albumName = audioTag.getFirst(FieldKey.ALBUM);
									if (albumName.isEmpty())
										albumName = "Unknown";
								} catch (KeyNotFoundException e) {
								}
							}

							int albumID = database.insertAlbum(albumName, null, null, interpretID);
							database.insertAlbumTrack(albumID, trackID);
						} else {
							database.updateTrackIndexedTimestamp(trackId);
						}
						return 1;
					} catch (KeyNotFoundException | SQLException | CannotReadException | IOException | TagException
							| ReadOnlyFileException | InvalidAudioFrameException e) {
						e.printStackTrace();
					}
				}
				return 0;
			}
			return 0;
		}

	}

	private static final Logger LOGGER = Logger.getLogger(MusicFileIndexer.class.getName());

}