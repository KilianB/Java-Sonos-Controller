package com.github.kilianB.example.voiceToTextPlayback;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import com.github.kilianB.NetworkUtil;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * @author Kilian
 *
 */
public class VoiceToTextLauncher extends Application {

	private final static Logger LOGGER = Logger.getLogger(VoiceToTextLauncher.class.getName());
	
	/*
	 * We create our own mp3 file. In order for sonos to play it back 
	 */
	private InetAddress hostAddress;
	private NetworkFileProvider fileProvider;
	//Ephemeral ports 49152 to 65535 
	private int port = 63258;
	
	
	public VoiceToTextLauncher() {
		
		//Local file hosting. 
		try {
			hostAddress = NetworkUtil.resolveSiteLocalAddress();
		} catch (IOException e) {
			
			try {
			hostAddress = InetAddress.getLocalHost();
			}catch(UnknownHostException e1) {
				e1.printStackTrace();
			}
			LOGGER.severe("Could not resolve a valid ip adress. fallback to localhost " + e);
		}
		
		fileProvider = new NetworkFileProvider(hostAddress.getHostAddress(),port,new String[] {"mp3"});
		
		File hostedFolder = new File("mp3TempFiles");
		hostedFolder.mkdirs();
		fileProvider.mapFolder(hostedFolder);
		
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		FXMLLoader loader = new FXMLLoader();
		
		loader.setController(new VoiceToTextController(fileProvider));
		loader.setLocation(getClass().getResource("VoiceToText.fxml"));
		
		Parent root = loader.load();
		
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("SonosIcon.png")));
		primaryStage.setTitle("Sonos Text To Speech");
		primaryStage.show();
		
	}
	
	@Override
	public void stop() {
		fileProvider.deinit();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}


}
