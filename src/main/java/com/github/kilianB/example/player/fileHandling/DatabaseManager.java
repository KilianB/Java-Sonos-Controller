package com.github.kilianB.example.player.fileHandling;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.SimpleResultSet;

class DatabaseManager {
	/* --------------------------------------------------------------------------------------------------------------+
	 * ----------------------------------------------- Database -----------------------------------------------------|
	 * --------------------------------------------------------------------------------------------------------------+
	 */
	Connection conn;
	PreparedStatement insertInterpret;
	PreparedStatement insertAlbum;
	PreparedStatement insertTrack;
	PreparedStatement insertTrackAlbum;
	PreparedStatement insertDirectoryHash;
	
	PreparedStatement checkIfInterpretExists;
	PreparedStatement checkIfAlbumExists;
	PreparedStatement checkIfTrackExists;
	PreparedStatement checkIfDirectoryHashExists;

	PreparedStatement retrieveAlbumIDByName;
	PreparedStatement retrieveInterpretByID;
	PreparedStatement retrieveSongsOfAlbumByAlbumID;
	PreparedStatement retrieveSongsByName;
	
	PreparedStatement retrieveIndexedFolders;
	
	//Retrieve the name of interprets who are within a certain edit distance
	PreparedStatement retrieveInterpretNameAprox;
	

	
	public DatabaseManager() {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:~/sonosMusicIndex");
		ds.setUser("sa");
		ds.setPassword("sa");
		try {
			conn = ds.getConnection();
			setupDatabase();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	private void setupDatabase() {
		
		try {
			
			Statement stmt = conn.createStatement();

			// Create table interpret
			if (!doesTableExist(conn, "INTERPRET")) {
				String sql = "CREATE TABLE Interpret " + " (id INTEGER IDENTITY(1,1), " + " name VARCHAR(255) UNIQUE, "
						+ " bio VARCHAR(MAX), " + " birthyear datetime, "
						// + " PRIMARY KEY (name)";
						+ " PRIMARY KEY ( id ))";
				stmt.execute(sql);

				// TODO currently interprets are equals just by name
			}

			// Create table track
			if (!doesTableExist(conn, "TRACK")) {
				String sql = "CREATE TABLE TRACK " + "(id INTEGER IDENTITY(1,1), " + " title VARCHAR(255) not NULL, "
						+ " description VARCHAR(MAX), " + " url VARCHAR(500) UNIQUE, " + " PRIMARY KEY ( id ))";
				stmt.execute(sql);
			}

			//TODO currently we have an interpret id as foreign key attached to the album. We migt have multiple interprets
			// in an album therefore create a n:n relation table.
			if (!doesTableExist(conn, "ALBUM")) {
				String sql = "CREATE TABLE ALBUM " + "(id INTEGER IDENTITY(1,1), " + " title VARCHAR(255) not NULL, "
						+ " description VARCHAR(MAX), " + " albumCover VARCHAR(500), " + " interpret_id INTEGER, "
						+ " PRIMARY KEY ( id ), " + " FOREIGN KEY (interpret_id) REFERENCES INTERPRET(id))";
				stmt.execute(sql);
			}

			if (!doesTableExist(conn, "TRACK_ALBUM")) {
				String sql = "CREATE TABLE TRACK_ALBUM " + "(albumID INTEGER, " + " trackID INTEGER,"
						+ " PRIMARY KEY ( albumID,trackID ), " + " FOREIGN KEY (albumID) REFERENCES ALBUM(id), "
						+ " FOREIGN KEY (trackID) REFERENCES TRACK(id))";
				stmt.execute(sql);
			}
			
			/*Table which saves the hashes of the directories which already got indexed. The hash consist of concatenated filenames 
			 * in the directory leading to leading to the directory to be re-indexed if a filename gets altered changed or deleted. 
			 * TODO add a SQL call to delete all enties which start with curDirectory if this case occurs to account for deleted files.
			 * TODO We don't have a way to account for hash collision at the moment.
			 * TODO make a clean up hash run at the end of indexing deleting all hashes if the directory is not present anymore.
 			 */
			
			if(!doesTableExist(conn,"INDEX_HASH")) {
				String sql = "CREATE TABLE INDEX_HASH " 
						+ "(dirHash INTEGER, " 
						+ "folderPath VARCHAR(120), "
						+ " PRIMARY KEY (dirHash ))";
				stmt.execute(sql);
			}
			// Prepare statements

			checkIfInterpretExists = conn.prepareStatement("SELECT id FROM INTERPRET WHERE name = ?");
			// checkIfAlbumExists = conn.prepareStatement("SELECT id FROM ALBUM WHERE title
			// = ? AND interpret_id = ?");
			checkIfAlbumExists = conn.prepareStatement("SELECT id FROM ALBUM WHERE title = ?");
			checkIfTrackExists = conn.prepareStatement("SELECT id FROM TRACK WHERE url = ?");
			checkIfDirectoryHashExists = conn.prepareStatement("SELECT dirHash FROM INDEX_HASH WHERE dirHash = ?");
			
			
			insertInterpret = conn.prepareStatement("INSERT INTO INTERPRET(name,bio,birthyear) VALUES(?,?,?)");
			insertAlbum = conn
					.prepareStatement("INSERT INTO ALBUM(title,description,albumCover,interpret_id) VALUES(?,?,?,?)");
			insertTrack = conn.prepareStatement("INSERT INTO TRACK(title,description,url) VALUES(?,?,?)");
			insertTrackAlbum = conn.prepareStatement("MERGE INTO TRACK_ALBUM VALUES(?,?)");
			insertDirectoryHash = conn.prepareStatement("INSERT INTO INDEX_HASH(dirHash,folderPath) VALUES(?,?)");
			// Prepare statements
			retrieveAlbumIDByName = conn.prepareStatement("SELECT * FROM ALBUM WHERE TITLE = ?");
			retrieveInterpretByID = conn.prepareStatement("SELECT * FROM INTERPRET WHERE ID = ?");
			retrieveSongsOfAlbumByAlbumID = conn.prepareStatement("SELECT * FROM TRACK_ALBUM  JOIN TRACK ON TRACK_ALBUM.TRACKID = TRACK.ID WHERE TRACK_ALBUM.ALBUMID = ?");
			retrieveSongsByName = conn.prepareStatement("SELECT * FROM TRACK WHERE TITLE = ?");
			
			//TODO add timestamp...
			retrieveIndexedFolders = conn.prepareStatement("SELECT folderPath FROM INDEX_HASH");
			
			/*
			JdbcConnection jbdc = (JdbcConnection)conn;
			jbdc.getSession();
			jbdc.getMetaData().getDa
			
			Schema schema = new Schema()
			*/
			
			
			try {
			stmt.execute("CREATE ALIAS LEVENSHTEIN FOR \""+DatabaseManager.class.getName()+".computeLevenshteinDistance\"");
			}catch (Exception e) {}
			retrieveInterpretNameAprox = conn.prepareStatement("SELECT * FROM INTERPRET WHERE LEVENSHTEIN(name,?)");
			
					//optimize our query
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove all directory hashes from the database which are not present anymore in the file structure.
	 * @param directoryHashset
	 */
	void cleanUpDirectoryHash(HashMap<Integer,String> directoryHash) {
		//TODO do we have a potential multi threading error here by setting auto commit = falsE?
		try {
		Statement s = conn.createStatement();
		//Delete everything
		s.execute("DELETE FROM INDEX_HASH");
		conn.setAutoCommit(false);
		
		//Re add
		for(Entry<Integer, String> entry : directoryHash.entrySet()) {
			insertDirectoryHash.setInt(1, entry.getKey());
			insertDirectoryHash.setString(2,entry.getValue());
			insertDirectoryHash.addBatch();
		}
		
		insertDirectoryHash.executeBatch();
		conn.commit();
		conn.setAutoCommit(true);
		}catch(SQLException e) {
			e.printStackTrace();
		}

	}
	//TODO unify throw and try catch
	
	boolean doesTableExist(Connection connection, String tableName) {

		try {
			DatabaseMetaData metadata = connection.getMetaData();
			ResultSet res = metadata.getTables(null, null, tableName, new String[] { "TABLE" });
			return res.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
			// TODO returning false in this matter is semantically not correct.
		}
	}

	int doesInterpretExist(String interpretName) throws SQLException {
		checkIfInterpretExists.setString(1, interpretName);
		ResultSet result = checkIfInterpretExists.executeQuery();
		if (result.next()) {
			return result.getInt("id");
		}
		return -1;
	}

	/**
	 * Check weather an entry with the supplied url already exists in the track
	 * database
	 * @param url
	 * 
	 * @return
	 * @throws SQLException
	 */
	int doesTrackExist(String url) throws SQLException {
		checkIfTrackExists.setString(1, url);
		ResultSet result = checkIfTrackExists.executeQuery();

		if (result.next()) {
			return result.getInt("id");
		}
		return -1;
	}

	int doesAlbumExist(String albumName, int interpretID) throws SQLException {
		checkIfAlbumExists.setString(1, albumName);
		// checkIfAlbumExists.setInt(2, interpretID);
		ResultSet result = checkIfAlbumExists.executeQuery();

		if (result.next()) {
			return result.getInt("id");
		}
		return -1;
	}
	
	boolean doesDirectoryHashExist(int hash){
		try {
			checkIfDirectoryHashExists.setInt(1, hash);
			ResultSet result = checkIfDirectoryHashExists.executeQuery();
			if (result.next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	int insertInterpret(String name, String bio, Date birthdate) {
		try {// Exists
			int interpretID = doesInterpretExist(name);
			if (interpretID == -1) {
				insertInterpret.setString(1, name);
				insertInterpret.setString(2, bio);
				insertInterpret.setDate(3, birthdate);
				insertInterpret.executeUpdate();
				ResultSet key = insertInterpret.getGeneratedKeys();
				key.next();
				interpretID = key.getInt(1);
				
			}
			return interpretID;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	int insertAlbum(String title, String description, String albumCover, int interpretID) {
		try {
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
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	int insertTrack(String title, String description, String url) {
		try {

			int trackID = doesTrackExist(url);
			// Exists
			if (trackID == -1) {
				insertTrack.setString(1, title);
				insertTrack.setString(2, description);
				insertTrack.setString(3, url);
				insertTrack.executeUpdate();
				ResultSet key = insertTrack.getGeneratedKeys();
				key.next();
				trackID = key.getInt(1);
			}

			return trackID;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	void insertDirectoryHash(int hash, String folderPath) {
		try {
			// Exists
			if (!doesDirectoryHashExist(hash)) {
				insertDirectoryHash.setInt(1, hash);
				insertDirectoryHash.setString(2, folderPath);
				insertDirectoryHash.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void insertAlbumTrack(int albumID, int trackID) {
		try {
			// MERGE Query
			insertTrackAlbum.setInt(1, albumID);
			insertTrackAlbum.setInt(2, trackID);
			insertTrackAlbum.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * CAUTION DROP ALL DATA IN THE DATABASE!
	 */
	public void resetDB() {
		try(Statement statement = conn.createStatement()){
			
			statement.executeQuery("DROP ALL OBJECTS");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		setupDatabase();
	}
	
	//TODO Alpha
	
	static int wordThreshold = 2;
	
	public static boolean computeLevenshteinDistance(String s1, String s2) {
		SimpleResultSet rs = new SimpleResultSet();
		LevenshteinDistance l = new LevenshteinDistance();
		return l.apply(s1, s2) <= wordThreshold;
	}
	

}
