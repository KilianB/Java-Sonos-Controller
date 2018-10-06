package com.github.kilianB.example.localFilePlayer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import com.github.kilianB.NetworkUtil;
import com.github.kilianB.example.localFilePlayer.fileHandling.DatabaseManager;
import com.github.kilianB.example.localFilePlayer.fileHandling.MusicFileIndexer;
import com.github.kilianB.example.localFilePlayer.fileHandling.NetworkFileProvider;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class DemoPlayer extends Application {

	private static Logger LOGGER = Logger.getLogger(DemoPlayer.class.getName());

	private InetAddress hostAddress;

	private NetworkFileProvider fileProvider;
	private MusicFileIndexer fileIndexer;
	private DatabaseManager db = new DatabaseManager();
	
	private String[] allowedFileExtensions = { "flac", "mp3", "wav" };

	DemoPlayerController controller;

	public DemoPlayer() {

		// String defaultIPv4Stack = System.getProperty("java.net.preferIPv4Stack");

		try {
			hostAddress = NetworkUtil.resolveSiteLocalAddress();
		} catch (IOException e) {
			LOGGER.severe("Could not resolve a valid ip adress. fallback to localhost " + e);
		}

		
		//db.resetDB();
		
		fileIndexer = new MusicFileIndexer(allowedFileExtensions, db);
		fileProvider = new NetworkFileProvider(hostAddress.getHostAddress(), 7001, allowedFileExtensions);

		controller = new DemoPlayerController(fileProvider, fileIndexer,db);


	}

	@Override
	public void start(Stage primaryStage) throws IOException {

		// Shutdown on window close!
		Platform.setImplicitExit(true);

		FXMLLoader loader = new FXMLLoader();
		loader.setController(controller);

		loader.setLocation(getClass().getResource("DemoPlayer.fxml"));

		Parent root = loader.load();

		Scene scene = new Scene(root, 1000, 800);
		scene.getStylesheets().add(getClass().getResource("DemoPlayer.css").toExternalForm());

		primaryStage.setScene(scene);

		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("icons/SonosIcon.png")));
		
		primaryStage.show();

	}

	@Override
	public void stop() {
		fileProvider.deinit();
		controller.deinit();
		db.close();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
