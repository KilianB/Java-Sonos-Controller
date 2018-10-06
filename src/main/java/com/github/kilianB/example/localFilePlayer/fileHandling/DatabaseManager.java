package com.github.kilianB.example.localFilePlayer.fileHandling;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

import com.github.kilianB.example.localFilePlayer.fileHandling.exception.MusicProviderInternalException;
import com.github.kilianB.example.localFilePlayer.fileHandling.exception.NoSuitableAlbumFound;
import com.github.kilianB.example.localFilePlayer.fileHandling.exception.NoSuitableSongFound;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Album;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.IndexedFolderData;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Interpret;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Song;
import com.github.kilianB.example.localFilePlayer.util.PlainAutoCloseable;
import com.github.kilianB.example.localFilePlayer.util.StringUtil;

import javafx.util.Pair;

/**
 * A music provider backed by an H2 SQL server serving the path of music files.
 * 
 * This implementation additionally keeps track of indexed locations which
 * highly suggests a usage on indexed directory files
 * 
 * 
 * @author Kilian
 *
 */
public class DatabaseManager implements MusicProvider, PlainAutoCloseable {

	/**
	 * Cache connection for increased performance
	 */
	private JdbcConnectionPool connPool;

	public DatabaseManager() {
		connPool = JdbcConnectionPool.create("jdbc:h2:~/sonosMusicIndex", "sa", "sa");
		setupDatabase();
	}

	/**
	 * Create all tables if they don't exist yet.
	 */
	private void setupDatabase() {

		try {

			Connection conn = connPool.getConnection();

			Statement stmt = conn.createStatement();

			// Initialize full text search. Currently not supported with old lucene instance
			// stmt.execute("CREATE ALIAS IF NOT EXISTS FTL_INIT FOR
			// \"org.h2.fulltext.FullTextLucene.init\";");
			// stmt.execute("CALL FTL_INIT();");

			// Table used to enable insert if not exists
			if (!doesTableExist("MUTEX")) {
				String sql = "CREATE TABLE MUTEX(i INTEGER, PRIMARY KEY (i))";
				stmt.execute(sql);
				stmt.execute("INSERT INTO MUTEX VALUES(1)");
			}

			/*
			 * Special implementation for local directory files.
			 */
			if (!doesTableExist("INDEXED_LOCATION")) {
				String sql = "CREATE TABLE INDEXED_LOCATION "
						+ "(id INTEGER IDENTITY(1,1), folderPath VARCHAR(120) UNIQUE not NULL, indexDate TIMESTAMP, "
						+ " PRIMARY KEY (id))";
				stmt.execute(sql);
			}

			/*
			 * Interpret table saving metadata for each interpret. Currently an interpret is
			 * uniquely identified by it's name. If we have proper metadata we could also
			 * identify him by additional attributes
			 */
			if (!doesTableExist("INTERPRET")) {
				String sql = "CREATE TABLE Interpret " + " (id INTEGER AUTO_INCREMENT, " + " name VARCHAR(255) UNIQUE, "
						+ " bio VARCHAR(MAX), " + " birthyear TIMESTAMP, "
						// + " PRIMARY KEY (name)";
						+ " PRIMARY KEY ( id ))";
				stmt.execute(sql);
				// Add default entry so we can use merge statements
				stmt.execute("INSERT INTO INTERPRET (name) VALUES('Unknown')");

			}

			/*
			 * Track table saving metadata about individual tracks.
			 */
			if (!doesTableExist("TRACK")) {
				String sql = "CREATE TABLE TRACK " + "(id INTEGER AUTO_INCREMENT, " + " title VARCHAR(255) not NULL, "
						+ " description VARCHAR(MAX), " + " trackLength INTEGER, " + " url VARCHAR(500) UNIQUE, " + "indexed TIMESTAMP,"
						+ "directoryId INTEGER," + "fileModified TIMESTAMP,"
						+ " PRIMARY KEY ( id ), FOREIGN KEY(directoryId) REFERENCES INDEXED_LOCATION(id))";
				stmt.execute(sql);

				// Enable full text search
				// Reindex if needed TODO inside get folders.

				// stmt.execute("CALL FTL_CREATE_INDEX('PUBLIC', 'TRACK', 'URL');");

			}

			// TODO currently we have an interpret id as foreign key attached to the album.
			// We migt have multiple interprets
			// in an album therefore create a n:n relation table.
			if (!doesTableExist("ALBUM")) {
				String sql = "CREATE TABLE ALBUM " + "(id INTEGER IDENTITY(1,1), " + " title VARCHAR(255) not NULL, "
						+ " description VARCHAR(MAX), " + " albumCover VARCHAR(500), " + " interpret_id INTEGER, "
						+ " PRIMARY KEY ( id ), " + " FOREIGN KEY (interpret_id) REFERENCES INTERPRET(id))";
				stmt.execute(sql);
			}

			/*
			 * Track album table N:N relationship between album and songs
			 */
			if (!doesTableExist("TRACK_ALBUM")) {
				String sql = "CREATE TABLE TRACK_ALBUM " + "(albumID INTEGER, " + " trackID INTEGER,"
						+ " PRIMARY KEY ( albumID,trackID ), " + " FOREIGN KEY (albumID) REFERENCES ALBUM(id), "
						+ " FOREIGN KEY (trackID) REFERENCES TRACK(id))";
				stmt.execute(sql);
			}

			try {
				stmt.execute("CREATE ALIAS IF NOT EXISTS LEVENSHTEIN FOR \"" + DatabaseManager.class.getName()
						+ ".computeLevenshteinDistance\"");
			} catch (Exception e) {
				e.printStackTrace();
			}

			// optimize our query
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Query if the database contains a table with the given name
	 * 
	 * @param tableName
	 * @return true if a table with the name exists, false otherwise
	 * @throws SQLException if an SQLError occurs
	 */
	private boolean doesTableExist(String tableName) throws SQLException {

		try (Connection con = connPool.getConnection()) {
			DatabaseMetaData metadata = con.getMetaData();
			ResultSet res = metadata.getTables(null, null, tableName, new String[] { "TABLE" });
			return res.next();
		}
	}

	// Music provider interface

	/**
	 * Check if an interpret already exists in the table
	 * 
	 * @param interpretName of the interpret
	 * @return the id of the interpret or -1 if the interpret does not exist in the
	 *         table
	 * @throws SQLException if an sql error occurs
	 */
	public int doesInterpretExist(String interpretName) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement checkIfInterpretExists = conn
						.prepareStatement("SELECT id FROM INTERPRET WHERE name = ?");) {

			checkIfInterpretExists.setString(1, interpretName);
			ResultSet result = checkIfInterpretExists.executeQuery();
			if (result.next()) {
				return result.getInt("id");
			}
		}
		return -1;
	}

	/**
	 * @param path
	 * @return the time the file was modified last or -1 if the track wasn't indexed
	 *         at all
	 * @throws SQLException
	 */
	public Timestamp getTrackLastModified(String path) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveTrackLastModified = conn
						.prepareStatement("SELECT fileModified FROM TRACK WHERE url = ?");) {

			retrieveTrackLastModified.setString(1, path);
			ResultSet rs = retrieveTrackLastModified.executeQuery();

			if (rs.next()) {
				return rs.getTimestamp("fileModified");
			} else {
				return null;
			}
		}
	}

	/**
	 * Check weather an entry with the supplied url already exists in the track
	 * database
	 * 
	 * @param url
	 * 
	 * @return -1 if it does not exist
	 * @throws SQLException
	 */
	public int doesTrackExist(String url) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement checkIfTrackExists = conn.prepareStatement("SELECT id FROM TRACK WHERE url = ?");) {

			checkIfTrackExists.setString(1, url);
			ResultSet result = checkIfTrackExists.executeQuery();

			if (result.next()) {
				return result.getInt("id");
			}
		}

		return -1;
	}

	public int doesAlbumExist(String albumName, int interpretID) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement checkIfAlbumExists = conn
						.prepareStatement("SELECT id FROM ALBUM WHERE title = ? AND interpret_id = ?");) {

			checkIfAlbumExists.setString(1, albumName);
			checkIfAlbumExists.setInt(2, interpretID);
			ResultSet result = checkIfAlbumExists.executeQuery();

			if (result.next()) {
				return result.getInt("id");
			}
		}

		return -1;
	}

	public int insertIndexedLocation(Path folderLocation) throws SQLException {

		int id = getIndexedLocationId(folderLocation);
	

		try (Connection conn = connPool.getConnection();
				/*
				 * PreparedStatement insertIndexedLocation = conn
				 * .prepareStatement("INSERT INTO INDEXED_LOCATION(folderPath,indexDate) VALUES(?,CURRENT_TIMESTAMP())"
				 * ,Statement.RETURN_GENERATED_KEYS);)
				 */
				// Insert into table if entry does not exist.

				PreparedStatement insertIndexedLocation = conn.prepareStatement(
						"MERGE INTO INDEXED_LOCATION (FOLDERPATH,INDEXDATE) KEY(FOLDERPATH) VALUES(?, CURRENT_TIMESTAMP())",
						Statement.RETURN_GENERATED_KEYS);

//				PreparedStatement insertIndexedLocation = conn.prepareStatement(
//						"INSERT INTO INDEXED_LOCATION SELECT ?,CURRENT_TIMESTAMP() FROM MUTEX LEFT OUTER JOIN INDEXED_LOCATION on INDEXED_LOCATION.FOLDERPATH = ? where MUTEX.I = 1 AND INDEXED_LOCATION.FOLDERPATH is null")
		) {
			insertIndexedLocation.setString(1, folderLocation.toString());
			// insertIndexedLocation.setString(2, folderLocation.toString());
			insertIndexedLocation.executeUpdate();
			
			if (id >= 0) {
				return id;
			}
			
			ResultSet key = insertIndexedLocation.getGeneratedKeys();
			if (key.next()) {
				return key.getInt(1);
			}
		}
		return -1;
	}

	public int insertInterpret(String name, String bio, Timestamp birthdate) throws SQLException {

		try (Connection conn = connPool.getConnection();
				/*
				 * PreparedStatement insertInterpret = conn.prepareStatement(
				 * "INSERT INTO INTERPRET(name,bio,birthyear) VALUES(?,?,?)",
				 * Statement.RETURN_GENERATED_KEYS);
				 */
				PreparedStatement insertInterpret = conn.prepareStatement(
						"INSERT INTO INTERPRET (NAME,BIO,BIRTHYEAR) SELECT ?,?,? FROM MUTEX LEFT OUTER JOIN INTERPRET on INTERPRET.NAME = ? where MUTEX.I = 1 AND INTERPRET.NAME is null",
						Statement.RETURN_GENERATED_KEYS);

		) {

			// Use merge to circumvent concurrency issues. doesInterpretExist and insert are
			// not atomic
			// and in multi threading environments this can lead to an issue since interpret
			// name is the only primary key!

			// TODO
			int interpretID = doesInterpretExist(name);
			if (interpretID == -1) {
				insertInterpret.setString(1, name);
				insertInterpret.setString(2, bio);
				insertInterpret.setTimestamp(3, birthdate);
				insertInterpret.setString(4, name);
				insertInterpret.executeUpdate();
				//Why does this sometimes throw an exception ? Race condition?
				ResultSet key = insertInterpret.getGeneratedKeys();
				if(key.next()) {
					interpretID = key.getInt(1);
				}else {
					interpretID = doesInterpretExist(name);
				}
			}
			return interpretID;

		}
	}

	public int insertAlbum(String title, String description, String albumCover, int interpretID) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement insertAlbum = conn.prepareStatement(
						"INSERT INTO ALBUM(title,description,albumCover,interpret_id) VALUES(?,?,?,?)",
						Statement.RETURN_GENERATED_KEYS);) {
			// Exists
			int albumID = doesAlbumExist(title, interpretID);
			if (albumID == -1) {
				insertAlbum.setString(1, title);
				insertAlbum.setString(2, description);
				insertAlbum.setString(3, albumCover);
				insertAlbum.setInt(4, interpretID);
				insertAlbum.executeUpdate();
				ResultSet key = insertAlbum.getGeneratedKeys();
				key.next();
				albumID = key.getInt(1);
			}
			return albumID;
		}

	}

	public int insertTrack(String title, String description, int trackLength, String url, Timestamp fileModified, int directoryId)
			throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement insertTrack = conn.prepareStatement(
						"INSERT INTO TRACK(title,description,trackLength,url,indexed,fileModified,directoryId) VALUES(?,?,?,?,CURRENT_TIMESTAMP(),?,?)",
						Statement.RETURN_GENERATED_KEYS);) {

			int trackID = doesTrackExist(url);
			// Exists
			if (trackID == -1) {
				insertTrack.setString(1, title);
				insertTrack.setString(2, description);
				insertTrack.setInt(3, trackLength);
				insertTrack.setString(4, url);
				insertTrack.setTimestamp(5, fileModified);
				insertTrack.setInt(6, directoryId);
				insertTrack.executeUpdate();
				ResultSet key = insertTrack.getGeneratedKeys();
				if (key.next()) {
					trackID = (int) key.getInt(1);
					return trackID;
				}
			}

			return trackID;
		}

	}

	public void insertAlbumTrack(int albumID, int trackID) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement insertTrackAlbum = conn.prepareStatement("MERGE INTO TRACK_ALBUM VALUES(?,?)");) {
			insertTrackAlbum.setInt(1, albumID);
			insertTrackAlbum.setInt(2, trackID);
			insertTrackAlbum.executeUpdate();
		}
	}

	/**
	 * CAUTION DROP ALL DATA IN THE DATABASE!
	 */
	public void resetDB() {
		try (Statement statement = connPool.getConnection().createStatement()) {

			statement.execute("DROP ALL OBJECTS");

		} catch (SQLException e) {
			e.printStackTrace();
		}
		setupDatabase();
	}

	public int getIndexedLocationId(Path path) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement getIndexedLocationId = conn
						.prepareStatement("SELECT ID FROM INDEXED_LOCATION WHERE folderPath = ?");) {

			getIndexedLocationId.setString(1, path.toString());
			ResultSet rs = getIndexedLocationId.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}
		}
		return -1;
	}

	// TODO Alpha

	static int wordThreshold = 2;

	public static boolean computeLevenshteinDistance(String s1, String s2) {
		LevenshteinDistance l = new LevenshteinDistance();
		return l.apply(s1, s2) <= wordThreshold;
	}

	public List<IndexedFolderData> getIndexedFolders() throws SQLException {

		List<IndexedFolderData> indexedFolder = new ArrayList<>();

		// Reindex CALL FT_REINDEX();.... might be expensive?

		try (Connection conn = connPool.getConnection();
				PreparedStatement insertIntoIndexedLocation = conn.prepareStatement("SELECT * FROM INDEXED_LOCATION");
				PreparedStatement getSongsPerDirectory = conn
						.prepareStatement("SELECT COUNT(*) FROM TRACK WHERE DIRECTORYID = ?");
		/*
		 * PreparedStatement reIndexFulltextSearch =
		 * conn.prepareStatement("CALL FTL_REINDEX();");
		 */) {

			// reIndexFulltextSearch.execute();

			ResultSet rs = insertIntoIndexedLocation.executeQuery();

			while (rs.next()) {
				int directoryId = rs.getInt(1);
				String basePath = rs.getString(2);
				Timestamp ts = rs.getTimestamp(3);

				getSongsPerDirectory.setInt(1, directoryId);

				ResultSet rsSongs = getSongsPerDirectory.executeQuery();

				rsSongs.next();

				int tracksIndexed = rsSongs.getInt(1);

//				// Get all tracks which
//				ResultSet rsFullTextSearch = conn.createStatement()
//						.executeQuery("SELECT COUNT(*) FROM FTL_SEARCH('" + escapeLuceneQuery(basePath) + "*',0,0);");
//
//				System.out.println(escapeLuceneQuery(basePath));
//
//				if (rsFullTextSearch.next()) {
//					tracksIndexed = rsFullTextSearch.getInt(1);
//				}
//
//				ResultSet rsFullTextSearch1 = conn.createStatement()
//						.executeQuery("SELECT * FROM FTL_SEARCH('URL:C\\:*',0,0);");
//
//				while (rsFullTextSearch1.next()) {
//					System.out.println(rsFullTextSearch1.getString(1) + " " + rsFullTextSearch1.getString(2));
//				}

				indexedFolder.add(new IndexedFolderData(basePath, ts, tracksIndexed));
			}

		}
		return indexedFolder;
	}

	public Interpret getInterpretById(int interpretId) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveInterpretByID = conn
						.prepareStatement("SELECT * FROM INTERPRET WHERE ID = ?");) {
			retrieveInterpretByID.setInt(1, interpretId);
			ResultSet rs = retrieveInterpretByID.executeQuery();

			if (rs.next()) {
				return new Interpret(rs.getString("NAME"), rs.getString("BIO"), rs.getTimestamp("BIRTHYEAR"));
			} else {
				return Interpret.createUnknownInterpret();
			}
		}

	}

	@Override
	public List<Song> getSongsByName(String trackName) throws NoSuitableSongFound, MusicProviderInternalException {

		ArrayList<Song> songs = new ArrayList<>();

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveSongsByName = conn.prepareStatement("SELECT * FROM TRACK WHERE TITLE = ?");) {
			retrieveSongsByName.setString(1, trackName);
			ResultSet songQuery = retrieveSongsByName.executeQuery();

			while (songQuery.next()) {
				String songTitle = songQuery.getString("TITLE");
				String songDescription = songQuery.getString("DESCRIPTION");
				String filePath = songQuery.getString("URL");
				int trackLength = songQuery.getInt("TRACKLENGTH");
				// TODO actually a path object
				String uri = StringUtil.decodeURLToURI(filePath);
				URI songFile = filePath == null ? null : new URI(uri);
				// Which should actually fail ...
				if (filePath == null || !new File(filePath).exists()) {
					continue;
					// TODO clean up work, delete file from database.
				}
				songs.add(new Song(songTitle,trackLength, songDescription, songFile));

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

	public List<Song> getSongsByAlbumId(int albumId) throws SQLException, URISyntaxException {

		ArrayList<Song> audioFiles = new ArrayList<>();

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveSongsOfAlbumByAlbumID = conn.prepareStatement(
						"SELECT * FROM TRACK_ALBUM  JOIN TRACK ON TRACK_ALBUM.TRACKID = TRACK.ID WHERE TRACK_ALBUM.ALBUMID = ?");) {
			retrieveSongsOfAlbumByAlbumID.setInt(1, albumId);
			ResultSet songQuery = retrieveSongsOfAlbumByAlbumID.executeQuery();

			while (songQuery.next()) {
				// We don't need the title as it's already in the tag..? Or do we want to
				// differentiate between
				// Database Title and Tag title?
				String songTitle = songQuery.getString("TITLE");
				String songDescription = songQuery.getString("DESCRIPTION");
				String filePath = songQuery.getString("URL");
				int trackLength = songQuery.getInt("TRACKLENGTH");
				
				// TODO this is a path !
				URI songFile = filePath == null ? null : new URI(StringUtil.decodeURLToURI(filePath));
				
				if (filePath == null || !new File(filePath).exists()) {
					// TODO clean up work, delete file from database.
					continue;
				}
				audioFiles.add(new Song(songTitle,trackLength, songDescription, songFile));
			}
		}
		return audioFiles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see modules.musicProvider.MusicProvider#getAlbumsByName(java.lang.String)
	 */
	@Override
	public List<Album> getAlbumsByName(String albumName) throws NoSuitableAlbumFound, MusicProviderInternalException {

		ArrayList<Album> albums = new ArrayList<>();

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveAlbumIDByName = conn
						.prepareStatement("SELECT * FROM ALBUM WHERE TITLE = ?");) {
			ResultSet rs = retrieveAlbumIDByName.executeQuery();
			retrieveAlbumIDByName.setString(1, albumName);

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
					interpret = getInterpretById(interpretID);
				} else {
					interpret = Interpret.createUnknownInterpret();
				}

				List<Song> audioFiles = getSongsByAlbumId(albumID);

				albums.add(new Album(title, description, albumCover, interpret, audioFiles));

			}

			if (albums.isEmpty()) {
				throw new NoSuitableAlbumFound("Local Network Music Provider could not find an album for " + albumName);
			} else {
				return albums;
			}
		} catch (SQLException | URISyntaxException e) {
			throw new MusicProviderInternalException(e.getMessage());
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
	// TODO throw exception?
	@Override
	public ArrayList<String> getInterpretNameAprox(String name, int editDistance)
			throws MusicProviderInternalException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveInterpretNameAprox = conn
						.prepareStatement("SELECT * FROM interpret WHERE LEVENSHTEIN(name,?)");) {
			wordThreshold = editDistance;
			retrieveInterpretNameAprox.setString(1, name);
			ArrayList<String> interpretNames = new ArrayList<>();

			ResultSet rs = retrieveInterpretNameAprox.executeQuery();

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

			return interpretNames;
		} catch (SQLException e) {
			throw new MusicProviderInternalException(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.kilianB.example.localFilePlayer.fileHandling.MusicProvider#getAllSongs()
	 */
	@Override
	public List<Album> getAllAlbums() throws MusicProviderInternalException {
		
		List<Album> albums = new ArrayList<>();
		
		try(Connection conn = connPool.getConnection();
				PreparedStatement albumIds = conn.prepareStatement("SELECT ID FROM ALBUM")){
			
			ResultSet rs = albumIds.executeQuery();
			
			while(rs.next()) {
				albums.add(this.getAlbumById(rs.getInt(1)));
			}
			
			
		} catch (SQLException e) {
			throw new MusicProviderInternalException(e.getMessage());
		}
		return albums;
		
//
//		Map<Album,List<Song>> albums = new HashMap<>();
//		Map<Integer,Album> albumLookup = new HashMap<>();
//		
//		
//		try (Connection conn = connPool.getConnection();
//				PreparedStatement getAllSongs = conn
//						.prepareStatement("SELECT t.title, t.description, t.url, album.id AS albumid FROM track AS " + 
//								"LEFT JOIN TRACK_ALBUM AS ta ON t.ID = ta.TRACKID " + 
//								"LEFT JOIN ALBUM  ON ta.ALBUMID = ALBUM.ID");
//				PreparedStatement albumIds = conn.prepareStatement("SELECT ID FROM ALBUM")
//				) {
//
//			//1st create all albums
//			ResultSet rsAlbum = albumIds.executeQuery();
//			
//			while(rsAlbum.next()) {
//				int albumId = rsAlbum.getInt(1);
//				Album a =  getAlbumById(albumId);
//				albums.put(a,new ArrayList<>());
//				albumLookup.put(albumId,a);
//			}
//			
//			
//			
//			ResultSet rs = getAllSongs.executeQuery();
//			
//			while (rs.next()) {
//				String songTitle = rs.getString(1);
//				String description = rs.getString(2);
//				URI path;
//				int albumId = rs.getInt(4);
//				try {
//					path = new URI(StringUtil.decodeURLToURI(rs.getString(3)));
//					Song song = new Song(songTitle, description, path);
//					albums.get(albumLookup.get(albumId)).add(song);
//				} catch (URISyntaxException e) {
//					e.printStackTrace();
//				}
//			}
//		} catch (SQLException e) {
//			throw new MusicProviderInternalException(e.getMessage());
//		}
//
//		return albums;
	}

	public Album getAlbumById(int albumId) throws MusicProviderInternalException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveAlbumIDByName = conn.prepareStatement("SELECT * FROM album WHERE id = ?");) {
			retrieveAlbumIDByName.setInt(1, albumId);
			ResultSet rs = retrieveAlbumIDByName.executeQuery();
			

			if (rs.next()) {

				int albumID = rs.getInt(1);
				String title = rs.getString("TITLE");
				String description = rs.getString("DESCRIPTION");
				String albumCoverPath = rs.getString("ALBUMCOVER");
				int interpretID = rs.getInt("INTERPRET_ID");

				File albumCover = albumCoverPath == null ? null : new File(albumCoverPath);

				Interpret interpret;
				// Retrieve the interpret
				if (interpretID > 0) {
					interpret = getInterpretById(interpretID);
				} else {
					interpret = Interpret.createUnknownInterpret();
				}

				List<Song> audioFiles = getSongsByAlbumId(albumID);

				return new Album(title, description, albumCover, interpret, audioFiles);

			}

			return null;
		} catch (SQLException | URISyntaxException e) {
			throw new MusicProviderInternalException(e.getMessage());
		}
	}

	/**
	 * @param unmapped
	 * @throws SQLException 
	 */
	public int getSongDuration(String unmapped) throws SQLException {
		try(Connection conn = connPool.getConnection();
				PreparedStatement getSongDuration = conn.prepareStatement("SELECT tracklength FROM track WHERE url = ?");){
			getSongDuration.setString(1,unmapped);
			ResultSet rs = getSongDuration.executeQuery();
			
			if(rs.next()) {
				return rs.getInt(1);
			}
		}
		return 0;
	}
	
	/*
	 * Update
	 */
	/**
	 * @param trackId
	 * @throws SQLException
	 */
	public void updateTrackIndexedTimestamp(int trackId) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement updateLastIndexedTimestamp = conn
						.prepareStatement("UPDATE TRACK SET INDEXED = CURRENT_TIMESTAMP() WHERE  id = ?");) {
			updateLastIndexedTimestamp.setInt(1, trackId);
			updateLastIndexedTimestamp.executeUpdate();
		}
	}

	/*
	 * Delete queries
	 */

	/**
	 * 
	 * @param cutoffDate
	 * @param directoryId
	 * @return the number of tracks deleted
	 * @throws SQLException
	 */
	public int deleteTracksIndexedOlder(Timestamp cutoffDate, int directoryId) throws SQLException {

		try (Connection conn = connPool.getConnection();
				PreparedStatement retrieveInterpretByID = conn
						.prepareStatement("DELETE FROM TRACK WHERE directoryId = ? AND INDEXED < ?");
				PreparedStatement deleteTrackAlbum = conn.prepareStatement("DELETE FROM TRACK_ALBUM WHERE TRACKID IN "
						+ "(SELECT id FROM TRACK WHERE directoryId = ? AND INDEXED < ?)")

		) {

			/// Delete track album first
			deleteTrackAlbum.setInt(1, directoryId);
			deleteTrackAlbum.setTimestamp(2, cutoffDate);
			deleteTrackAlbum.executeUpdate();

			retrieveInterpretByID.setInt(1, directoryId);
			retrieveInterpretByID.setTimestamp(2, cutoffDate);
			return retrieveInterpretByID.executeUpdate();
		}

	}

	/*
	 * Utility methods
	 */

	/**
	 * Apache lucene is used as engine to allow wildcard full text search in the h2
	 * database. Special characters have to be escaped in order for the query to
	 * work.
	 * 
	 * @param input
	 * @return
	 * @see https://lucene.apache.org/core/2_9_4/queryparsersyntax.html
	 */
	public static String escapeLuceneQuery(String input) {
		// +\\-:!(){}[\\]^\"~*?:
		input = input.replace("\\", "\\\\");
		input = input.replaceAll("([+\\-:!(){}\\[\\]^\"~*?]|(&&))", "\\\\$1");
		input = input.replace("||", "\\||");
		return input;
	}

	public static Date toSQLDate(FileTime fileTime) {
		return new Date(fileTime.toMillis());
	}

	public static Timestamp toSQLTimestamp(FileTime fileTime) {
		return new Timestamp(fileTime.toMillis());
	}

	/**
	 * Release all database conections and release lock files
	 */
	@Override
	public void close() {
		connPool.dispose();
	}

	
}