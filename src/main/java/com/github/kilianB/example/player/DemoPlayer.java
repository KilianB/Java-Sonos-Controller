package com.github.kilianB.example.player;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DemoPlayer extends Application {

	@Override
	public void start(Stage primaryStage) throws IOException {
		
		DemoPlayerController controller = new DemoPlayerController();
		
		FXMLLoader loader = new FXMLLoader();
		loader.setController(controller);
		
		loader.setLocation(getClass().getResource("DemoPlayer.fxml"));
		
		Parent root = loader.load();
		
		Scene scene = new Scene(root,1000,800);
		scene.getStylesheets().add(getClass().getResource("DemoPlayer.css").toExternalForm());
		primaryStage.setScene(scene);
		
		primaryStage.show();
		
		
	}

	public static void main(String[] args) {
		launch(args);
	}
}
