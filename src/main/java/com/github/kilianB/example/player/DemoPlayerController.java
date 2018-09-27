package com.github.kilianB.example.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.listener.SonosEventAdapter;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.github.kilianB.uPnPClient.UPnPDevice;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class DemoPlayerController {

	@FXML
	private VBox roomRoot;

	@FXML
	private ImageView forwardBtn;

	private Image forwardImg;
	private Image forwardImgHoover;

	@FXML
	private ImageView backBtn;

	private Image backImg;
	private Image backImgHoover;

	@FXML
	private ImageView playPauseBtn;

	private Image playImg;
	private Image playImgHoover;
	private Image stopImg;
	private Image stopImgHoover;
	private Image pauseImg;
	private Image pauseImgHoover;
	
	
	private SonosDevice currentlyActiveDevice;

	@FXML
	private JFXTreeTableView playlistTable;

	
	//We don't clear the image cache currently. If this ever leads to a memory issue maybe implement
	//a linked hashmap and clear the oldest entries... Or a priority hashmap sorting by last access date.
	private HashMap<String,Image> cachedAlbumImages = new HashMap<>();
	private List<SonosDevice> devices = new ArrayList<>();
	private HashMap<String, SonosDevice> sonosDeviceMap = new HashMap<>();

	
	public DemoPlayerController() {

		backImg = new Image(getClass().getResourceAsStream("back.png"));
		backImgHoover = new Image(getClass().getResourceAsStream("backHoover.png"));

		playImg = new Image(getClass().getResourceAsStream("play.png"));
		playImgHoover = new Image(getClass().getResourceAsStream("playHoover.png"));

		pauseImg = new Image(getClass().getResourceAsStream("pause.png"));
		pauseImgHoover = new Image(getClass().getResourceAsStream("pauseHoover.png"));

		stopImg = new Image(getClass().getResourceAsStream("stop.png"));
		stopImgHoover = new Image(getClass().getResourceAsStream("stopHoover.png"));

		forwardImg = new Image(getClass().getResourceAsStream("next.png"));
		forwardImgHoover = new Image(getClass().getResourceAsStream("nextHoover.png"));
	}

	
	@FXML
	public void initialize() {
		backBtn.setImage(backImg);

		backBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			backBtn.setImage(backImg);
		});

		backBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			backBtn.setImage(backImgHoover);
		});

		playPauseBtn.setImage(playImg);

		playPauseBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			playPauseBtn.setImage(playImg);
		});

		playPauseBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			playPauseBtn.setImage(playImgHoover);
		});

		forwardBtn.setImage(forwardImg);

		forwardBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			forwardBtn.setImage(forwardImg);
		});

		forwardBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			forwardBtn.setImage(forwardImgHoover);
		});

		setupTreeTableView();

		try {
			devices = SonosDiscovery.discover(3);

			for (int i = 0; i < devices.size(); i++) {
				VBox groupWrapper = new VBox();
				groupWrapper.getStyleClass().add("zoneToken");
				groupWrapper.setAlignment(Pos.TOP_CENTER);
				
				SonosDevice device = devices.get(i);
				
				String zoneName = device.getRoomNameCached();
				ImageView thumbnailCurrentlyPlayed = new ImageView();
				thumbnailCurrentlyPlayed.setFitHeight(45);
				thumbnailCurrentlyPlayed.setFitWidth(45);
				
				Label zoneNameLabel = new Label(zoneName);
				zoneNameLabel.getStyleClass().add("zoneNameTitle");
				
				groupWrapper.getChildren().add(zoneNameLabel);
				
				
				device.registerSonosEventListener(new SonosEventAdapter() {

					@Override
					public void playStateChanged(PlayState playState) {
						
					}

					@Override
					public void trackChanged(TrackInfo currentTrack) {
						String imgUri = currentTrack.getMetadata().getAlbumArtURI();
						loadAndSetImage(imgUri,device,thumbnailCurrentlyPlayed);
					}
					
					@Override
					public void sonosDeviceConnected(String deviceName) {
						groupWrapper.setDisable(false);
					}

					@Override
					public void sonosDeviceDisconnected(String deviceName) {
						groupWrapper.setDisable(true);
					}
					@Override
					public void groupChanged(ArrayList<String> allDevicesInZone) {
						
						//TODO
					}					
				});
				
				
				try {
					TrackMetadata track = device.getCurrentTrackInfo().getMetadata();
					loadAndSetImage(track.getAlbumArtURI(),device,thumbnailCurrentlyPlayed);
					
					String trackTitle = track.getTitle();
					if(trackTitle.isEmpty()) {
						trackTitle = "No Song selected";
					}
					
					groupWrapper.getChildren().add(new Label(trackTitle));
					
				} catch (SonosControllerException e1) {
					e1.printStackTrace();
				}
				
				groupWrapper.getChildren().add(thumbnailCurrentlyPlayed);

				groupWrapper.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					switchZone(zoneName,groupWrapper);
				});

				roomRoot.getChildren().add(groupWrapper);

				// TODO change to arraylist and map by zone name?
				sonosDeviceMap.put(zoneName, devices.get(i));

			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	
	private void setupTreeTableView() {

		TreeTableColumn<TrackMetadataProperty, String> icon = new JFXTreeTableColumn<>("Icon");

		icon.setCellValueFactory(item -> {
			return item.getValue().getValue().getAlbumUri();
		});
		

		icon.setCellFactory((
				TreeTableColumn<TrackMetadataProperty, String> param) -> new JFXTreeTableCell<TrackMetadataProperty, String>() {
					
					ImageView imageView = new ImageView();
					
					{
						imageView.setFitHeight(64);
						imageView.setFitWidth(64);
					}
					
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						
						if (item != null) {
							//Update the table row if this is the item currently
							//playing
							//TODO add check if this device is still connected
							loadAndSetImage(item,devices.get(0),imageView);
							setGraphic(imageView);
						} else {
							setText(null);
							setGraphic(null);
						}
					}
				});

		TreeTableColumn<TrackMetadataProperty, String> title = new JFXTreeTableColumn<>("Title");

		title.setCellValueFactory(item -> {
			return item.getValue().getValue().getTitle();
		});
		// title.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param)
		// -> new JFXTreeTableCell<>());

		TreeTableColumn<TrackMetadataProperty, String> album = new JFXTreeTableColumn<>("Album");

		album.setCellValueFactory(item -> {
			return item.getValue().getValue().getAlbum();
		});

		// album.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param)
		// -> new JFXTreeTableCell<>());

		TreeTableColumn<TrackMetadataProperty, String> artist = new JFXTreeTableColumn<>("Artist");

		artist.setCellValueFactory(item -> {
			return item.getValue().getValue().getCreator();
		});

		// artist.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param)
		// -> new JFXTreeTableCell<>());

//		TreeTableColumn<TrackMetadataProperty, String> path = new JFXTreeTableColumn<>("Path");
//
//		path.setCellValueFactory(item -> {
//			return item.getValue().getValue().getPath();
//		});

		// path.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param) ->
		// new JFXTreeTableCell<>());

		//icon.setMaxWidth(Integer.MAX_VALUE * 50);
		title.setMaxWidth(Integer.MAX_VALUE);
		album.setMaxWidth(Integer.MAX_VALUE);
		artist.setMaxWidth(Integer.MAX_VALUE);
		//path.setMaxWidth(Integer.MAX_VALUE);
		
		playlistTable.getColumns().add(icon);
		playlistTable.getColumns().add(title);
		playlistTable.getColumns().add(album);
		playlistTable.getColumns().add(artist);
		//playlistTable.getColumns().add(path);
		playlistTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
	}

	String currentZoneName = "";
	VBox currentGroupWrapper;
	
	private void switchZone(String zoneName, VBox groupWrapper) {

		if (!zoneName.equals(currentZoneName)) {
			
			try {
				SonosDevice device = sonosDeviceMap.get(zoneName);
		
				List<TrackMetadata> queueInfo = device.getQueue(0, Integer.MAX_VALUE);
				
				currentlyActiveDevice = device;
				
				if(currentGroupWrapper != null) {
					currentGroupWrapper.getStyleClass().remove("selected");
				}
				
				
				currentGroupWrapper = groupWrapper;
				groupWrapper.getStyleClass().add("selected");
				
				//register listeners...
				
				
				String ipAddress = device.getIpAddress();
				
				ObservableList<TrackMetadataProperty> data = FXCollections.observableArrayList();

				for (TrackMetadata t : queueInfo) {
					data.add(new TrackMetadataProperty(t,device));
				}
				TreeItem<TrackMetadataProperty> root = new RecursiveTreeItem<TrackMetadataProperty>(data,
						RecursiveTreeObject::getChildren);
				playlistTable.setRoot(root);
				playlistTable.setShowRoot(false);

			} catch (IOException | SonosControllerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void loadAndSetImage(String imgBaseUri,SonosDevice device, ImageView targetImageView) {
		if(!cachedAlbumImages.containsKey(imgBaseUri)) {
			String imgResolvedUri = device.resolveAlbumURL(imgBaseUri);
			cachedAlbumImages.put(imgBaseUri, new Image(imgResolvedUri,64,64,true,false,true));
		}
		targetImageView.setImage(cachedAlbumImages.get(imgBaseUri));
	};
	
	class TrackMetadataProperty extends RecursiveTreeObject<TrackMetadataProperty> {

		SimpleStringProperty title;
		SimpleStringProperty album;
		SimpleStringProperty albumArtist;
		SimpleStringProperty albumUri;
		SimpleStringProperty creator;
		SimpleBooleanProperty currentlyPlaying;
		
		public TrackMetadataProperty(TrackMetadata metadata,SonosDevice device) {

			title = new SimpleStringProperty(metadata.getTitle());
			album = new SimpleStringProperty(metadata.getAlbum());
			albumArtist = new SimpleStringProperty(metadata.getAlbumArtist());
			albumUri = new SimpleStringProperty(metadata.getAlbumArtURI());
			creator = new SimpleStringProperty(metadata.getCreator());
		}

		public SimpleStringProperty getTitle() {
			return title;
		}

		public void setTitle(SimpleStringProperty title) {
			this.title = title;
		}

		public SimpleStringProperty getAlbum() {
			return album;
		}

		public void setAlbum(SimpleStringProperty album) {
			this.album = album;
		}

		public SimpleStringProperty getAlbumArtist() {
			return albumArtist;
		}

		public void setAlbumArtist(SimpleStringProperty albumArtist) {
			this.albumArtist = albumArtist;
		}

		public SimpleStringProperty getAlbumUri() {
			return albumUri;
		}

		public void setAlbumUri(SimpleStringProperty albumUri) {
			this.albumUri = albumUri;
		}

		public SimpleStringProperty getCreator() {
			return creator;
		}

		public void setCreator(SimpleStringProperty creator) {
			this.creator = creator;
		}

		public SimpleBooleanProperty getCurrentlyPlayingProperty() {
			return currentlyPlaying;
		}
	}

}
