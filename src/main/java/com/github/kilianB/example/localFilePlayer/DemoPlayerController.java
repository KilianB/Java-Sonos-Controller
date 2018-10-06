package com.github.kilianB.example.player;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.github.kilianB.example.player.fileHandling.NetworkFileProvider;
import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.ParserHelper;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.listener.SonosEventAdapter;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.base.JFXTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import javafx.util.Duration;

public class DemoPlayerController {

	@FXML
	private VBox roomRoot;

	@FXML
	private ImageView forwardBtn;

	@FXML
	private ImageView backBtn;

	@FXML
	private Label durationProgressLabel;

	@FXML
	private ImageView playPauseBtn;

	@FXML
	private Slider songPositionSlider;

	@FXML
	private Slider volumeSlider;

	@FXML
	private ImageView volumeImg;

	@FXML
	private Button clearListBtn;

	@FXML
	private Button addDirectoryBtn;

	@FXML
	private JFXTreeTableView<IndexDirectoryModel> indexedDirectories;

	private Image forwardImg;
	private Image forwardImgHoover;
	private Image backImg;
	private Image backImgHoover;
	private Image playImg;
	private Image playImgHoover;
	private Image stopImg;
	private Image stopImgHoover;
	private Image pauseImg;
	private Image pauseImgHoover;

	private Image curPlayPauseImage;
	private Image curPlayPauseImageHoover;

	private Image folderImg;

	private SonosDevice currentlyActiveDevice;

	@FXML
	private JFXTreeTableView<TrackMetadataModel> playlistTable;
	
	@FXML
	private JFXTreeTableView<LocalTrackModel> musicLibraryView;

	// We don't clear the image cache currently. If this ever leads to a memory
	// issue maybe implement
	// a linked hashmap and clear the oldest entries... Or a priority hashmap
	// sorting by last access date.
	private HashMap<String, Image> cachedAlbumImages = new HashMap<>();
	private List<SonosDevice> devices = new ArrayList<>();
	private HashMap<String, SonosDevice> sonosDeviceMap = new HashMap<>();

	private String currentZoneName = "";
	private VBox currentGroupWrapper;
	private SonosEventListener curentListener;
	
	
	private ObservableList<TrackMetadataModel> currentPlaylistData = FXCollections.observableArrayList();
	private ObservableList<IndexDirectoryModel> indexedDirectoryData = FXCollections.observableArrayList();
	private ObservableList<LocalTrackModel> localMusicData = FXCollections.observableArrayList();

	
	
	private NetworkFileProvider fileProvider;

	// Position slier animation.
	private Timeline positionAnimation = new Timeline();

	// Settings
	int directoryRowHeight = 35;

	public DemoPlayerController(NetworkFileProvider fileProvider) {

		this.fileProvider = fileProvider;
		// TODO do this with a clipping oval and css coloring on transparent image. much
		// much easier!
		backImg = new Image(getClass().getResourceAsStream("icons/back.png"));
		backImgHoover = new Image(getClass().getResourceAsStream("icons/backHoover.png"));

		playImg = new Image(getClass().getResourceAsStream("icons/play.png"));
		playImgHoover = new Image(getClass().getResourceAsStream("icons/playHoover.png"));

		pauseImg = new Image(getClass().getResourceAsStream("icons/pause.png"));
		pauseImgHoover = new Image(getClass().getResourceAsStream("icons/pauseHoover.png"));

		stopImg = new Image(getClass().getResourceAsStream("icons/stop.png"));
		stopImgHoover = new Image(getClass().getResourceAsStream("icons/stopHoover.png"));

		forwardImg = new Image(getClass().getResourceAsStream("icons/next.png"));
		forwardImgHoover = new Image(getClass().getResourceAsStream("icons/nextHoover.png"));

		curPlayPauseImage = pauseImg;
		curPlayPauseImageHoover = pauseImgHoover;

		folderImg = new Image(getClass().getResourceAsStream("icons/folder.png"), directoryRowHeight / 2,
				directoryRowHeight / 2, false, true);
	}

	@FXML
	public void initialize() {

		// Handle the volume slider
		volumeSlider.setMin(0);
		volumeSlider.setMax(100);

		volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
			if (volumeSlider.isValueChanging()) {
				if (currentlyActiveDevice != null) {
					try {
						currentlyActiveDevice.setVolume(newValue.intValue());
					} catch (IOException | SonosControllerException e1) {
						e1.printStackTrace();
					}
				}

			}
		});

		clearListBtn.setOnAction(e -> {
			if (currentlyActiveDevice != null) {
				try {
					currentlyActiveDevice.clearQueue();
					// The upnp listener will also catch it. but it's delayed. So we do double the
					// work
					// for a bit performance
					rebuildQueueList();
				} catch (IOException | SonosControllerException e1) {
					e1.printStackTrace();
				}
			}
		});

		addDirectoryBtn.setOnAction(e -> {

			DirectoryChooser dirChooser = new DirectoryChooser();
			dirChooser.setTitle("Directory to index");

			File selectedDirectory = dirChooser.showDialog(addDirectoryBtn.getScene().getWindow());

			if (selectedDirectory != null) {
				
				if(fileProvider.mapFolder(selectedDirectory,()->{
					
					//Callback add data 
					localMusicData.clear();
					
					//fileProvider.getAllSongs();
					
					
				})) {
					indexedDirectoryData.add(new IndexDirectoryModel(selectedDirectory.getAbsolutePath(), 0, -1));
				}
				
			
			}

		});

		volumeImg.setImage(new Image(getClass().getResourceAsStream("icons/volume.png")));

		backBtn.setImage(backImg);

		backBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			backBtn.setImage(backImg);
		});

		backBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			backBtn.setImage(backImgHoover);
		});

		backBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

			if (currentlyActiveDevice != null) {
				try {
					currentlyActiveDevice.previous();
				} catch (IOException | SonosControllerException e1) {
					e1.printStackTrace();
//					// This happens if we have repeat and try to playback the next song.
//					// This works but throws an error. TODO properly reset index to 0
//					if (!e1.getMessage().contains("UPnP Error 711")) {
//						e1.printStackTrace();
//					}
				}
			}
		});

		playPauseBtn.setImage(curPlayPauseImage);

		playPauseBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			playPauseBtn.setImage(curPlayPauseImageHoover);
		});

		playPauseBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			playPauseBtn.setImage(curPlayPauseImage);
		});

		playPauseBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

			if (currentlyActiveDevice != null) {
				try {
					switch (currentlyActiveDevice.getPlayState()) {
					case ERROR:
						break;
					case PAUSED_PLAYBACK:
						currentlyActiveDevice.play();
						curPlayPauseImage = pauseImg;
						curPlayPauseImageHoover = pauseImgHoover;
						playPauseBtn.setImage(curPlayPauseImage);
						positionAnimation.play();
						break;
					case PLAYING:
						currentlyActiveDevice.pause();
						curPlayPauseImage = playImg;
						curPlayPauseImageHoover = playImgHoover;
						playPauseBtn.setImage(curPlayPauseImage);
						positionAnimation.pause();
						break;
					case STOPPED:
						// TODO check
						currentlyActiveDevice.stop();
						curPlayPauseImage = playImg;
						curPlayPauseImageHoover = playImgHoover;
						playPauseBtn.setImage(curPlayPauseImage);
						break;
					case TRANSITIONING:
						break;
					default:
						break;

					}
				} catch (IOException | SonosControllerException e1) {
					e1.printStackTrace();
				}
			}

		});

		forwardBtn.setImage(forwardImg);

		forwardBtn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
			forwardBtn.setImage(forwardImg);
		});

		forwardBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
			forwardBtn.setImage(forwardImgHoover);
		});

		forwardBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

			if (currentlyActiveDevice != null) {
				try {
					currentlyActiveDevice.next();
				} catch (IOException | SonosControllerException e1) {
					e1.printStackTrace();

//					// This happens if we have repeat and try to playback the next song.
//					// This works but throws an error. TODO properly reset index to 0
//					if (!e1.getMessage().contains("UPnP Error 711")) {
//						e1.printStackTrace();
//					}
				}
			}
		});

		setupTreeTableView();

		try {
			devices = SonosDiscovery.discover(2);

			for (int i = 0; i < devices.size(); i++) {
				VBox groupWrapper = new VBox();
				groupWrapper.getStyleClass().add("zoneToken");
				groupWrapper.setAlignment(Pos.TOP_CENTER);

				SonosDevice device = devices.get(i);

				String zoneName = device.getRoomNameCached();
				ImageView thumbnailCurrentlyPlayed = new ImageView();
				thumbnailCurrentlyPlayed.setFitHeight(45);
				thumbnailCurrentlyPlayed.setFitWidth(45);
				thumbnailCurrentlyPlayed.getStyleClass().add("zoneTokenImageView");

				Rectangle clip = new Rectangle(thumbnailCurrentlyPlayed.getFitWidth(),
						thumbnailCurrentlyPlayed.getFitHeight());
				clip.setArcWidth(5);
				clip.setArcHeight(5);

				thumbnailCurrentlyPlayed.setClip(clip);

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
						loadAndSetImage(imgUri, device, thumbnailCurrentlyPlayed);

						if (device == currentlyActiveDevice) {
							boolean playing = true;
							try {
								playing = device.getPlayState().equals(PlayState.PLAYING);
							} catch (IOException | SonosControllerException e) {
								e.printStackTrace();
							}
							setupSongPositionSpliderAnimation(currentTrack, playing);
						}
					}

					public void volumeChanged(int newVolume) {
						// Check if the user isn't currently manipulating it himself!.
						if (device == currentlyActiveDevice && !volumeSlider.isValueChanging()) {
							volumeSlider.setValue(newVolume);
						}
					}

					@Override
					public void sonosDeviceConnected(String deviceName) {
						// TODO
						groupWrapper.setDisable(false);
					}

					@Override
					public void sonosDeviceDisconnected(String deviceName) {
						// TODO
						groupWrapper.setDisable(true);
					}

					@Override
					public void groupChanged(ArrayList<String> allDevicesInZone) {

						// TODO
					}
				});

				try {
					TrackInfo track = device.getCurrentTrackInfo();

					TrackMetadata trackMeta = track.getMetadata();

					loadAndSetImage(trackMeta.getAlbumArtURI(), device, thumbnailCurrentlyPlayed);

					String trackTitle = trackMeta.getTitle();
					if (trackTitle.isEmpty()) {
						trackTitle = "No Song selected";
					}

					groupWrapper.getChildren().add(new Label(trackTitle));

				} catch (SonosControllerException e1) {
					e1.printStackTrace();
				}

				groupWrapper.getChildren().add(thumbnailCurrentlyPlayed);

				groupWrapper.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					switchZone(zoneName, groupWrapper);
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

		TreeTableColumn<TrackMetadataModel, Boolean> currentlyPlaying = new JFXTreeTableColumn<>("");
		currentlyPlaying.setCellValueFactory(item -> {
			return item.getValue().getValue().getCurrentlyPlayingProperty();
		});
		currentlyPlaying.setCellFactory((
				TreeTableColumn<TrackMetadataModel, Boolean> param) -> new JFXTreeTableCell<TrackMetadataModel, Boolean>() {

					@Override
					protected void updateItem(Boolean item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							if (item.booleanValue()) {
								this.getTreeTableRow().getStyleClass().add("playing");
							} else {
								this.getTreeTableRow().getStyleClass().remove("playing");
							}

						}
						setText(null);
						setGraphic(null);
					}

				});

		TreeTableColumn<TrackMetadataModel, String> icon = new JFXTreeTableColumn<>("Icon");

		icon.setCellValueFactory(item -> {
			return item.getValue().getValue().getAlbumUri();
		});

		icon.setCellFactory((
				TreeTableColumn<TrackMetadataModel, String> param) -> new JFXTreeTableCell<TrackMetadataModel, String>() {

					ImageView imageView = new ImageView();

					{
						imageView.setFitHeight(64);
						imageView.setFitWidth(64);
					}

					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							// Update the table row if this is the item currently
							// playing
							// TODO add check if this device is still connected
							loadAndSetImage(item, devices.get(0), imageView);
							setGraphic(imageView);
						} else {
							setText(null);
							setGraphic(null);
						}
					}
				});

		TreeTableColumn<TrackMetadataModel, String> title = new JFXTreeTableColumn<>("Title");

		title.setCellValueFactory(item -> {
			return item.getValue().getValue().getTitle();
		});

		TreeTableColumn<TrackMetadataModel, String> album = new JFXTreeTableColumn<>("Album");

		album.setCellValueFactory(item -> {
			return item.getValue().getValue().getAlbum();
		});

		TreeTableColumn<TrackMetadataModel, String> artist = new JFXTreeTableColumn<>("Artist");

		artist.setCellValueFactory(item -> {
			return item.getValue().getValue().getCreator();
		});

		icon.setMinWidth(64);
		icon.setMaxWidth(64);
		// hide this pseudo table cell
		currentlyPlaying.setMaxWidth(0);
		title.setMaxWidth(Integer.MAX_VALUE);
		album.setMaxWidth(Integer.MAX_VALUE);
		artist.setMaxWidth(Integer.MAX_VALUE);

		playlistTable.setRowFactory(new StyleChangingRowFactory("playing"));

		playlistTable.getColumns().addAll(icon, title, album, artist, currentlyPlaying);

		playlistTable.setSelectionModel(null);

		// playlistTable.getColumns().add(path);
		playlistTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		// Directory table

		TreeTableColumn<IndexDirectoryModel, String> path = new JFXTreeTableColumn("Path");

		path.setCellValueFactory(value -> {
			return value.getValue().getValue().getFilePath();
		});

		path.setCellFactory((
				TreeTableColumn<IndexDirectoryModel, String> param) -> new JFXTreeTableCell<IndexDirectoryModel, String>() {

					ImageView imgView = new ImageView();

					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							imgView.setImage(folderImg);
							setGraphic(imgView);
							setText(item);
						} else {
							setText(null);
							setGraphic(null);
						}
					}
				});

		TreeTableColumn<IndexDirectoryModel, Integer> itemsIndexed = new JFXTreeTableColumn<>("Indexed");

		itemsIndexed.setCellValueFactory(value -> {
			return value.getValue().getValue().getSongsFound().asObject();
		});

		TreeTableColumn<IndexDirectoryModel, Integer> lastIndexed = new JFXTreeTableColumn("Last Indexed");

		lastIndexed.setCellValueFactory(value -> {
			return value.getValue().getValue().getLastIndexed().asObject();
		});

		TreeTableColumn<IndexDirectoryModel, Integer> removeBox = new JFXTreeTableColumn("");

		removeBox.setCellValueFactory(value -> {
			
			return value.getValue().getValue().getDirectoryId().asObject();
		});

		// removeBox.setCellFactory(Callback<Tr>);

		removeBox.setCellFactory((
				TreeTableColumn<IndexDirectoryModel, Integer> param) -> new JFXTreeTableCell<IndexDirectoryModel, Integer>() {

					Button removeFromMapping = new Button("X");

					@Override
					protected void updateItem(Integer item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							setGraphic(removeFromMapping);
							setText(null);							
						} else {
							setText(null);
							setGraphic(null);
						}
						this.setPrefWidth(45);
					}
				});

		// indexedDirectories

		indexedDirectories
				.setRowFactory(new Callback<TreeTableView<IndexDirectoryModel>, TreeTableRow<IndexDirectoryModel>>() {
					@Override
					public TreeTableRow<IndexDirectoryModel> call(TreeTableView<IndexDirectoryModel> param) {
						TreeTableRow<IndexDirectoryModel> row = new TreeTableRow<>();
						row.setPrefHeight(directoryRowHeight);
						row.setAlignment(Pos.CENTER_LEFT);
						return row;
					}
				});
		
		path.setMaxWidth(Integer.MAX_VALUE * 4f);
		itemsIndexed.setMaxWidth(Integer.MAX_VALUE);
		lastIndexed.setMaxWidth(Integer.MAX_VALUE);
		removeBox.setMaxWidth(Integer.MAX_VALUE);
		
		indexedDirectories.getColumns().addAll(path, itemsIndexed, lastIndexed, removeBox);

		// indexedDirectories.setSelectionModel(null);

		indexedDirectories.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		TreeItem<IndexDirectoryModel> root = new RecursiveTreeItem<IndexDirectoryModel>(indexedDirectoryData,
				RecursiveTreeObject::getChildren);
		indexedDirectories.setRoot(root);
		indexedDirectories.setShowRoot(false);
		
		TreeTableColumn<LocalTrackModel, String> titleLocal = new JFXTreeTableColumn<>("Title");
		
		titleLocal.setCellValueFactory( value ->{
			return value.getValue().getValue().getTitle();
		});
		
		TreeTableColumn<LocalTrackModel, String> albumLocal = new JFXTreeTableColumn<>("Album");
		
		albumLocal.setCellValueFactory( value ->{
			return value.getValue().getValue().getAlbum();
		});
		
		
		TreeTableColumn<LocalTrackModel, String> pathLocal = new JFXTreeTableColumn<>("Path");
		
		pathLocal.setCellValueFactory( value ->{
			return value.getValue().getValue().getPath();
		});
		
		musicLibraryView.getColumns().addAll(titleLocal,albumLocal,pathLocal);
		
		

	}

	private void switchZone(String zoneName, VBox groupWrapper) {

		if (!zoneName.equals(currentZoneName)) {

			try {
				SonosDevice device = sonosDeviceMap.get(zoneName);

				if (currentlyActiveDevice != null) {
					currentlyActiveDevice.unregisterSonosEventListener(curentListener);
				}

				currentlyActiveDevice = device;

				if (currentGroupWrapper != null) {
					currentGroupWrapper.getStyleClass().remove("selected");
				}

				currentGroupWrapper = groupWrapper;

				curentListener = new SonosEventAdapter() {
					@Override
					public void trackChanged(TrackInfo trackInfo) {

						TrackMetadata currentlyPlaying = trackInfo.getMetadata();

						// TODO solve this with a more performant approach. e.g. a hashmap
						for (TrackMetadataModel property : currentPlaylistData) {

							if (!currentlyPlaying.getTitle().equals(property.getTitle().get())) {

								SimpleBooleanProperty isCurrentlyPlaying = property.getCurrentlyPlayingProperty();

								// A good heuristic could be to simply take the next and start searching there
								if (isCurrentlyPlaying.get()) {
									isCurrentlyPlaying.set(false);
								}
							} else {
								// This will also fail for duplicate entires. deep inspect
								if (currentlyPlaying.getAlbum().equals(property.getAlbum().get())) {
									property.getCurrentlyPlayingProperty().set(true);
								}
							}
						}
					}

					public void queueChanged(List<QueueEvent> queuesAffected) {
						rebuildQueueList();
					}

					@Override
					public void playModeChanged(PlayMode playMode) {

					}
				};

				device.registerSonosEventListener(curentListener);

				groupWrapper.getStyleClass().add("selected");

				TrackInfo curTrack = device.getCurrentTrackInfo();

				boolean playing = device.getPlayState().equals(PlayState.PLAYING);

				if (curTrack != null)
					setupSongPositionSpliderAnimation(curTrack, playing);

				volumeSlider.setValue(device.getVolume());

				// register listeners...

				rebuildQueueList();

			} catch (IOException | SonosControllerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void rebuildQueueList() {

		Platform.runLater(() -> {
			currentPlaylistData.clear();

			List<TrackMetadata> queueInfo;
			try {
				queueInfo = currentlyActiveDevice.getQueue(0, Integer.MAX_VALUE);
				for (TrackMetadata t : queueInfo) {
					currentPlaylistData.add(new TrackMetadataModel(t, currentlyActiveDevice));
				}
				TreeItem<TrackMetadataModel> root = new RecursiveTreeItem<TrackMetadataModel>(currentPlaylistData,
						RecursiveTreeObject::getChildren);
				playlistTable.setRoot(root);
				playlistTable.setShowRoot(false);
			} catch (IOException | SonosControllerException e) {
				e.printStackTrace();
			}

		});
	}

	private void loadAndSetImage(String imgBaseUri, SonosDevice device, ImageView targetImageView) {

		String imgResolvedUri;
		if (imgBaseUri.isEmpty()) {
			// TODO change to own img
			imgBaseUri = "placeholder";
			imgResolvedUri = getClass().getResource("icons/placeholder.png").toString();
		} else {
			imgResolvedUri = device.resolveAlbumURL(imgBaseUri);
		}
		if (!cachedAlbumImages.containsKey(imgBaseUri)) {

			cachedAlbumImages.put(imgBaseUri, new Image(imgResolvedUri, 64, 64, true, false, true));
		}
		targetImageView.setImage(cachedAlbumImages.get(imgBaseUri));
	};

	public class StyleChangingRowFactory<T> implements Callback<JFXTreeTableView, TreeTableRow<T>> {

		private final String styleClass;
		private final ObservableList<Integer> styledRowIndices;
		private final Callback<JFXTreeTableView, TreeTableRow<T>> baseFactory;

		/**
		 * Construct a <code>StyleChangingRowFactory</code>, specifying the name of the
		 * style class that will be applied to rows determined by
		 * <code>getStyledRowIndices</code> and a base factory to create the
		 * <code>TableRow</code>. If <code>baseFactory</code> is <code>null</code>,
		 * default table rows will be created.
		 * 
		 * @param styleClass  The name of the style class that will be applied to
		 *                    specified rows.
		 * @param baseFactory A factory for creating the rows. If null, default
		 *                    <code>TableRow&lt;T&gt;</code>s will be created using the
		 *                    default <code>TableRow</code> constructor.
		 */
		public StyleChangingRowFactory(String styleClass, Callback<JFXTreeTableView, TreeTableRow<T>> baseFactory) {
			this.styleClass = styleClass;
			this.baseFactory = baseFactory;
			this.styledRowIndices = FXCollections.observableArrayList();
		}

		/**
		 * Construct a <code>StyleChangingRowFactory</code>, which applies
		 * <code>styleClass</code> to the rows determined by
		 * <code>getStyledRowIndices</code>, and using default <code>TableRow</code>s.
		 * 
		 * @param styleClass
		 */
		public StyleChangingRowFactory(String styleClass) {
			this(styleClass, null);
		}

		@Override
		public TreeTableRow<T> call(JFXTreeTableView param) {
			final TreeTableRow<T> row;
			if (baseFactory == null) {
				row = new TreeTableRow<>();
			} else {
				row = baseFactory.call(param);
			}

			row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if (e.getClickCount() == 2) {
					try {
						currentlyActiveDevice.playFromQueue(row.getIndex() + 1);
					} catch (IOException | SonosControllerException e1) {
						e1.printStackTrace();
					}
				}
			});

			row.indexProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> obs, Number oldValue, Number newValue) {
					updateStyleClass(row);
				}
			});

			styledRowIndices.addListener(new ListChangeListener<Integer>() {
				@Override
				public void onChanged(Change<? extends Integer> change) {
					updateStyleClass(row);
				}
			});

			return row;
		}

		/**
		 * 
		 * @return The list of indices of the rows to which <code>styleClass</code> will
		 *         be applied. Changes to the content of this list will result in the
		 *         style class being immediately updated on rows whose indices are
		 *         either added to or removed from this list.
		 */
		public ObservableList<Integer> getStyledRowIndices() {
			return styledRowIndices;
		}

		private void updateStyleClass(TreeTableRow<T> row) {
			final ObservableList<String> rowStyleClasses = row.getStyleClass();
			if (styledRowIndices.contains(row.getIndex())) {
				if (!rowStyleClasses.contains(styleClass)) {
					rowStyleClasses.add(styleClass);
				}
			} else {
				// remove all occurrences of styleClass:
				rowStyleClasses.removeAll(Collections.singletonList(styleClass));
			}
		}
	}

	private void setupSongPositionSpliderAnimation(TrackInfo currentTrack, boolean playing) {
		positionAnimation.pause();

		int duration = currentTrack.getDuration();

		songPositionSlider.setMin(0);
		songPositionSlider.setMax(duration);
		songPositionSlider.setValue(currentTrack.getPosition());

		positionAnimation.stop();
		positionAnimation.getKeyFrames().clear();

		int initialOffset = currentTrack.getPosition();

		// Weak listener should be gced automatically? TODO care about memory leak?
		SimpleStringProperty durationLabelTest = new SimpleStringProperty();
		SimpleIntegerProperty currentPositionProperty = new SimpleIntegerProperty(initialOffset);

		currentPositionProperty.addListener((obs, oldVal, newVal) -> {

			// new value will be updates

			durationLabelTest.set(ParserHelper.secondsToFormatedTimestamp(newVal.intValue()) + " / "
					+ ParserHelper.secondsToFormatedTimestamp(duration));
		});

		Platform.runLater(() -> {
			durationProgressLabel.textProperty().bind(durationLabelTest);
		});

		KeyValue kv = new KeyValue(songPositionSlider.valueProperty(), duration);
		KeyValue kv1 = new KeyValue(currentPositionProperty, duration);

		double timeLeft = duration - currentTrack.getPosition();

		if (timeLeft > 0) {
			positionAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(timeLeft), kv, kv1));
			positionAnimation.setCycleCount(0);

			if (playing) {
				positionAnimation.play();
			}
		}

	}

	/**
	 * Release upnp threads and allow the jvm to terminate
	 */
	public void deinit() {
		for (SonosDevice d : devices) {
			d.deinit();
		}
	}

}
