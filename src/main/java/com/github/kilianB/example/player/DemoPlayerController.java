package com.github.kilianB.example.player;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.github.kilianB.uPnPClient.UPnPDevice;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
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

	HashMap<String, SonosDevice> sonosDeviceMap = new HashMap<>();

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
			List<SonosDevice> devices = SonosDiscovery.discover(1);

			for (int i = 0; i < devices.size(); i++) {
				VBox groupWrapper = new VBox();
				groupWrapper.getStyleClass().add("zoneToken");
				groupWrapper.setAlignment(Pos.TOP_CENTER);
				String zoneName = devices.get(i).getRoomNameCached();
				groupWrapper.getChildren().add(new Label(zoneName));

				try {
					TrackMetadata track = devices.get(i).getCurrentTrackInfo().getMetadata();

					groupWrapper.getChildren().add(new Label(track.getTitle()));

				} catch (SonosControllerException e1) {
					e1.printStackTrace();
				}

				groupWrapper.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					switchZone(zoneName);
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
					
					Image image;
					ImageView imageView = new ImageView();
					
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (item != null) {
							
							if(image == null) {
								image = new Image(item,64,64,true,true,true);
							}
							//setText(item);
							imageView.setImage(image);
							System.out.println("updateIcon " + item);
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
			return item.getValue().getValue().getAlbumArtist();
		});

		// artist.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param)
		// -> new JFXTreeTableCell<>());

		TreeTableColumn<TrackMetadataProperty, String> path = new JFXTreeTableColumn<>("Path");

		path.setCellValueFactory(item -> {
			return item.getValue().getValue().getAlbum();
		});

		// path.setCellFactory((TreeTableColumn<TrackMetadataProperty, String> param) ->
		// new JFXTreeTableCell<>());

		playlistTable.getColumns().add(icon);
		playlistTable.getColumns().add(title);
		playlistTable.getColumns().add(album);
		playlistTable.getColumns().add(artist);
		playlistTable.getColumns().add(path);
	}

	String currentZoneName = "";

	private void switchZone(String zoneName) {

		if (!zoneName.equals(currentZoneName)) {

			try {
				SonosDevice device = sonosDeviceMap.get(zoneName);
				List<TrackMetadata> queueInfo = device.getQueue(0, Integer.MAX_VALUE);
				
				currentlyActiveDevice = device;
				
				//register listeners...
				
				
				String ipAddress = device.getIpAddress();
				
				ObservableList<TrackMetadataProperty> data = FXCollections.observableArrayList();

				System.out.println(ipAddress);
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

	class TrackMetadataProperty extends RecursiveTreeObject<TrackMetadataProperty> {

		SimpleStringProperty title;
		SimpleStringProperty album;
		SimpleStringProperty albumArtist;
		SimpleStringProperty albumUri;
		SimpleStringProperty creator;

		public TrackMetadataProperty(TrackMetadata metadata,SonosDevice device) {

			title = new SimpleStringProperty(metadata.getTitle());
			album = new SimpleStringProperty(metadata.getAlbum());
			albumArtist = new SimpleStringProperty(metadata.getAlbumArtist());
			albumUri = new SimpleStringProperty(device.resolveAlbumURL(metadata.getAlbumArtURI()));
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

	}

}
