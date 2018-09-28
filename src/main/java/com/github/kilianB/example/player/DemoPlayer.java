package com.github.kilianB.example.player;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import com.github.kilianB.NetworkUtil;
import com.github.kilianB.example.player.fileHandling.MusicFileIndexer;
import com.github.kilianB.example.player.fileHandling.NetworkFileProvider;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DemoPlayer extends Application {

	private static Logger LOGGER = Logger.getLogger(DemoPlayer.class.getName());

	private InetAddress hostAddress;

	
	private NetworkFileProvider fileProvider;

	private String[] allowedFileExtensions = { "flac", "mp3", "wav" };

	
	DemoPlayerController controller;
	
	public DemoPlayer() {

		// String defaultIPv4Stack = System.getProperty("java.net.preferIPv4Stack");

		try {
			hostAddress = NetworkUtil.resolveSiteLocalAddress();
		} catch (IOException e) {
			LOGGER.severe("Could not resolve a valid ip adress. fallback to localhost " + e);
		}

		fileProvider = new NetworkFileProvider(hostAddress.getHostAddress(),7001,allowedFileExtensions);
		
		controller  = new DemoPlayerController(fileProvider);
		
		// Add shutdown hooks. SQL connection
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			// musicIndexer.deinit();
		}));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			fileProvider.deinit();
		}));

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

		primaryStage.show();
		
	}

	@Override
	public void stop() {
		controller.deinit();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
