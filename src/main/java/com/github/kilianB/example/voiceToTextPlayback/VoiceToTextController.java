package com.github.kilianB.example.voiceToTextPlayback;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.kilianB.apis.googleTextToSpeech.GLanguage;
import com.github.kilianB.apis.googleTextToSpeech.GoogleTextToSpeech;
import com.github.kilianB.apis.googleTextToSpeech.GoogleTextToSpeechAdapter;
import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.util.StringConverter;

/**
 * @author Kilian
 *
 */
public class VoiceToTextController {

	@FXML
	private JFXTextField textToConvertField;

	@FXML
	private JFXButton announceBtn;

	@FXML
	private JFXComboBox<SonosDevice> speakerCbox;

	private ObservableList<SonosDevice> devices = FXCollections.observableList(new ArrayList<>());
	private NetworkFileProvider fileProvider;
	
	
	/**
	 * @param fileProvider
	 */
	public VoiceToTextController(NetworkFileProvider fileProvider) {
		this.fileProvider = Objects.requireNonNull(fileProvider);
	}


	@FXML
	public void initialize() {
		//Initialize combo box
		speakerCbox.itemsProperty().set(devices);
		speakerCbox.setConverter(new SonosDeviceStringConverter());
		
		SonosDiscovery.discoverAsynch(3,device->{
			devices.add(device);
		});
		
		
		//Convert and play back
		announceBtn.setOnAction( e ->{
			
			String text = textToConvertField.getText();
			SonosDevice currentDevice = speakerCbox.getSelectionModel().getSelectedItem();
			
			if(currentDevice != null && text != null && !text.isEmpty()) {
				
				System.out.println("Start request");
				
				GoogleTextToSpeech gtts = new GoogleTextToSpeech("mp3TempFiles/");
				gtts.convertTextAsynch(text,GLanguage.English_GB,"Anounce",true,new GoogleTextToSpeechAdapter() {
					@Override
					public void mergeCompleted(File f,int id) {
					
						System.out.println("Merge completed");
						
						//We got the file we want to play back. 
						String uri = "http://"+fileProvider.toMappedPath(f.getAbsolutePath());
						try {
							currentDevice.clip(uri,null);
						} catch (IOException | SonosControllerException | InterruptedException e) {
							e.printStackTrace();
						}
						
					}
				});
			}
			
		});
		
	}
	
	
	class SonosDeviceStringConverter extends StringConverter<SonosDevice>{

		@Override
		public String toString(SonosDevice object) {			
			return	object.getRoomNameCached();
		}

		@Override
		public SonosDevice fromString(String string) {
			throw new UnsupportedOperationException("Can't construct device from string");
		}
		
	}
	
}
