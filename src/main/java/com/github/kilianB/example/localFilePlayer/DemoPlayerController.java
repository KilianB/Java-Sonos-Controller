package com.github.kilianB.example.localFilePlayer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.github.kilianB.example.localFilePlayer.fileHandling.DatabaseManager;
import com.github.kilianB.example.localFilePlayer.fileHandling.MusicFileIndexer;
import com.github.kilianB.example.localFilePlayer.fileHandling.NetworkFileProvider;
import com.github.kilianB.example.localFilePlayer.fileHandling.exception.MusicProviderInternalException;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Album;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.IndexedFolderData;
import com.github.kilianB.example.localFilePlayer.fileHandling.model.Song;
import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.ParserHelper;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.github.kilianB.sonos.listener.SonosEventAdapter;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.jfoenix.controls.JFXTextField;
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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
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

	@FXML
	private JFXTextField searchLocalMusic;

	private Image forwardImg;
	private Image forwardImgHoover;
	private Image backImg;
	private Image backImgHoover;
	private Image playImg;
	private Image playImgHoover;
//	private Image stopImg;
//	private Image stopImgHoover;
	private Image pauseImg;
	private Image pauseImgHoover;
	private Image reload;

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
	private MusicFileIndexer fileIndexer;
	private DatabaseManager db;

	// Position slier animation.
	private Timeline positionAnimation = new Timeline();

	// Custom drag and drop action
	private DataFormat dragTrackFormat = new DataFormat("com.github.kilianB.copyTrackModel");

	// Settings
	int directoryRowHeight = 35;

	public DemoPlayerController(NetworkFileProvider fileProvider, MusicFileIndexer fileIndexer, DatabaseManager db) {

		this.fileProvider = fileProvider;
		this.fileIndexer = fileIndexer;
		this.db = db;

		// TODO do this with a clipping oval and css coloring on transparent image. much
		// much easier!
		backImg = new Image(getClass().getResourceAsStream("icons/back.png"));
		backImgHoover = new Image(getClass().getResourceAsStream("icons/backHoover.png"));

		playImg = new Image(getClass().getResourceAsStream("icons/play.png"));
		playImgHoover = new Image(getClass().getResourceAsStream("icons/playHoover.png"));

		pauseImg = new Image(getClass().getResourceAsStream("icons/pause.png"));
		pauseImgHoover = new Image(getClass().getResourceAsStream("icons/pauseHoover.png"));

//		stopImg = new Image(getClass().getResourceAsStream("icons/stop.png"));
//		stopImgHoover = new Image(getClass().getResourceAsStream("icons/stopHoover.png"));

		forwardImg = new Image(getClass().getResourceAsStream("icons/next.png"));
		forwardImgHoover = new Image(getClass().getResourceAsStream("icons/nextHoover.png"));

		reload = new Image(getClass().getResourceAsStream("icons/reload.png"));

		curPlayPauseImage = pauseImg;
		curPlayPauseImageHoover = pauseImgHoover;

		folderImg = new Image(getClass().getResourceAsStream("icons/folder.png"), directoryRowHeight / 2,
				directoryRowHeight / 2, false, true);

		// Map all folders

		try {
			List<IndexedFolderData> indexedFolder = db.getIndexedFolders();

			for (IndexedFolderData indexed : indexedFolder)
				fileProvider.mapFolder(new File(indexed.getFolderPath()));

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@FXML
	public void initialize() {

		// Setup filtering
		searchLocalMusic.textProperty().addListener((obs, old, newV) -> {
			musicLibraryView.setPredicate((value) -> {
				return (value.getValue().getTitle().get().contains(newV)
						|| value.getValue().getAlbum().get().contains(newV));

			});
		});

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

			crawlDirectory(selectedDirectory);

		});

		updateIndexedLocation();

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
					// Add after the current song
					int curQueueIndex = currentlyActiveDevice.getCurrentTrackInfo().getQueueIndex();
					
					if(!currentlyActiveDevice.getQueue(curQueueIndex,1).isEmpty()) {
						currentlyActiveDevice.next();
					}else {
						currentlyActiveDevice.playFromQueue(1);
					}
						
				} catch (IOException | SonosControllerException e1) {
					e1.printStackTrace();
				}
			}
		});

		setupTreeTableView();

		populateIndexedMusicFiles();

		discoverSonosDevices();
	}

	/**
	 * 
	 */
	private void updateIndexedLocation() {
		try {
			indexedDirectoryData.clear();
			List<IndexedFolderData> indexFolders = db.getIndexedFolders();
			for (IndexedFolderData indexedFolder : indexFolders) {
				indexedDirectoryData.add(new IndexDirectoryModel(indexedFolder.getFolderPath(),
						indexedFolder.getTracksIndexed(), indexedFolder.getLastIndexed().toLocalDateTime()));

			}
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * @param selectedDirectory
	 */
	private void crawlDirectory(File selectedDirectory) {
		if (selectedDirectory != null) {
			fileIndexer.crawlAsynch(selectedDirectory.toPath(), (filesIndexed) -> {
				if (fileProvider.mapFolder(selectedDirectory)) {
					// Callback add data
					indexedDirectoryData.add(new IndexDirectoryModel(selectedDirectory.getAbsolutePath(), filesIndexed,
							LocalDateTime.now()));

				} else {
					// Reindexed maybe
					// Update last indexed timestamp
					updateIndexedLocation();
				}
				populateIndexedMusicFiles();
			});

		}
	}

	/**
	 * 
	 */
	private void discoverSonosDevices() {
		devices = new ArrayList<>();
		SonosDiscovery.discoverAsynch(2, (sonosDevice) -> {
			devices.add(sonosDevice);
			Platform.runLater(() -> {
				VBox groupWrapper = new VBox();
				groupWrapper.getStyleClass().add("zoneToken");
				groupWrapper.setAlignment(Pos.TOP_CENTER);

				String zoneName = sonosDevice.getRoomNameCached();
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

				Label trackLabel = new Label();

				groupWrapper.getChildren().add(zoneNameLabel);

				sonosDevice.registerSonosEventListener(new SonosEventAdapter() {

					@Override
					public void playStateChanged(PlayState playState) {

						if (sonosDevice == currentlyActiveDevice) {
							if (playState.equals(PlayState.PLAYING)) {
								positionAnimation.play();
							}
						}

					}

					@Override
					public void trackChanged(TrackInfo currentTrack) {
						String imgUri = currentTrack.getMetadata().getAlbumArtURI();
						loadAndSetImage(imgUri, sonosDevice, thumbnailCurrentlyPlayed);

						Platform.runLater(() -> {
							String trackTitle = currentTrack.getMetadata().getTitle();
							if (trackTitle.isEmpty()) {
								trackTitle = "No Song selected";
							}

							trackLabel.setText(trackTitle);
						});

						if (sonosDevice == currentlyActiveDevice) {
							boolean playing = true;
							try {
								playing = sonosDevice.getPlayState().equals(PlayState.PLAYING);
							} catch (IOException | SonosControllerException e) {
								e.printStackTrace();
							}

							// Traclinfo is not properly initialized when using the playUri command
							// Therefore, we do not have access to the duration.

							setupSongPositionSpliderAnimation(currentTrack, playing);
						}
					}

					public void volumeChanged(int newVolume) {
						// Check if the user isn't currently manipulating it himself!.
						if (sonosDevice == currentlyActiveDevice && !volumeSlider.isValueChanging()) {
							volumeSlider.setValue(newVolume);
						}
					}

					// TODO we will get these callbacks multiple times per event.
					@Override
					public void sonosDeviceConnected(String deviceName) {
						try {
							if (deviceName.equals(currentlyActiveDevice.getDeviceName())) {
								groupWrapper.setDisable(false);
							}
						} catch (IOException | SonosControllerException e) {
							e.printStackTrace();
						}

					}

					@Override
					public void sonosDeviceDisconnected(String deviceName) {
						try {
							if (deviceName.equals(currentlyActiveDevice.getDeviceName())) {
								groupWrapper.setDisable(true);
							}
						} catch (IOException | SonosControllerException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void groupChanged(ArrayList<String> allDevicesInZone) {
						// TODO
					}
				});

				try {
					TrackInfo track = sonosDevice.getCurrentTrackInfo();

					TrackMetadata trackMeta = track.getMetadata();

					loadAndSetImage(trackMeta.getAlbumArtURI(), sonosDevice, thumbnailCurrentlyPlayed);

					String trackTitle = trackMeta.getTitle();
					if (trackTitle.isEmpty()) {
						trackTitle = "No Song selected";
					}

					trackLabel.setText(trackTitle);
					groupWrapper.getChildren().add(trackLabel);

				} catch (SonosControllerException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				groupWrapper.getChildren().add(thumbnailCurrentlyPlayed);

				groupWrapper.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					switchZone(zoneName, groupWrapper);
				});

				roomRoot.getChildren().add(groupWrapper);

				// TODO change to arraylist and map by zone name?
				sonosDeviceMap.put(zoneName, sonosDevice);
			});
		});
	}

	/**
	 * 
	 */
	private void populateIndexedMusicFiles() {
		System.out.println("populated indexed music files");
		localMusicData.clear();
		List<Album> albums;
		try {
			albums = db.getAllAlbums();

			for (Album album : albums) {
				for (Song song : album.getTracks()) {
					LocalTrackModel track = new LocalTrackModel(song.getTitle(), song.getMusicFile().toString(),
							album.getTitle(), song.getTrackLength());
					localMusicData.add(track);
				}
			}

		} catch (MusicProviderInternalException e2) {
			e2.printStackTrace();
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

		playlistTable.setOnDragOver(dragEvent -> {
			// asContent(new DataFormat("copyTrackModel"))
			if (dragEvent.getGestureSource() != playlistTable && dragEvent.getDragboard().hasContent(dragTrackFormat)) {
				/* allow for both copying and moving, whatever user chooses */
				dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			dragEvent.consume();
		});

		playlistTable.setOnDragDropped(dragEvent -> {
			Dragboard db = dragEvent.getDragboard();
			boolean success = false;

			System.out.println("Drag done");
			if (db.hasContent(dragTrackFormat)) {

				System.out.println("Drag accepted: " + db.hasContent(dragTrackFormat));

				try {
					LocalTrackModel trackModel = (LocalTrackModel) db.getContent(dragTrackFormat);

					System.out.println("Model: " + trackModel);
					TrackMetadata meta = new TrackMetadata(trackModel.getTitle().get(), "", "",
							trackModel.getAlbum().get(), "");
					System.out.println("Meta");
					try {
						String playbackPath = "http://" + fileProvider.getMapPrefix() + trackModel.getPath().get();

						// Alternativly
						// currentlyActiveDevice.playUri(playbackPath, meta);
						// Add after the current song
						int curQueueIndex = currentlyActiveDevice.getCurrentTrackInfo().getQueueIndex();

						currentlyActiveDevice.addToQueue(curQueueIndex + 1, playbackPath, meta);
						if (!currentlyActiveDevice.getPlayState().equals(PlayState.PLAYING)) {
							currentlyActiveDevice.playFromQueue(curQueueIndex + 1);
						}
						// We will get a notification about queue update but it might take a few
						// seconds. Speed it up
						rebuildQueueList();
					} catch (IOException | SonosControllerException e1) {
						e1.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				success = true;
			}
			/*
			 * let the source know whether the string was successfully transferred and used
			 */
			dragEvent.setDropCompleted(success);

			dragEvent.consume();
		});

		// Directory table

		TreeTableColumn<IndexDirectoryModel, String> path = new JFXTreeTableColumn<>("Path");

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

		TreeTableColumn<IndexDirectoryModel, LocalDateTime> lastIndexed = new JFXTreeTableColumn<>("Last Indexed");

		lastIndexed.setCellValueFactory(value -> {
			return value.getValue().getValue().getLastIndexed();
		});

		lastIndexed.setCellFactory((
				TreeTableColumn<IndexDirectoryModel, LocalDateTime> param) -> new JFXTreeTableCell<IndexDirectoryModel, LocalDateTime>() {

					DateTimeFormatter df = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

					@Override
					protected void updateItem(LocalDateTime item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							setGraphic(null);
							setText(df.format(item));
						} else {
							setText(null);
							setGraphic(null);
						}
					}
				});

		TreeTableColumn<IndexDirectoryModel, Integer> removeBox = new JFXTreeTableColumn<>("");

		removeBox.setCellValueFactory(value -> {

			return value.getValue().getValue().getDirectoryId().asObject();
		});

		// removeBox.setCellFactory(Callback<Tr>);

		removeBox.setCellFactory((
				TreeTableColumn<IndexDirectoryModel, Integer> param) -> new JFXTreeTableCell<IndexDirectoryModel, Integer>() {

					Button removeFromMapping = new Button("X");
					Button reIndex = new Button("");

					@Override
					protected void updateItem(Integer item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							removeFromMapping.prefWidthProperty().bind(this.widthProperty().divide(2));
							reIndex.prefWidthProperty().bind(this.widthProperty().divide(2));

							reIndex.setOnAction(e -> {
								IndexDirectoryModel rowData = indexedDirectoryData
										.get(this.getTreeTableRow().getIndex());
								File directory = new File(rowData.getFilePath().get());
								crawlDirectory(directory);
							});

							removeFromMapping.setDisable(true);

							ImageView imgView = new ImageView(reload);
							imgView.setFitHeight(16);
							imgView.setFitWidth(16);
							reIndex.setGraphic(imgView);
							HBox fp = new HBox(5);

							fp.getChildren().addAll(reIndex, removeFromMapping);
							setGraphic(fp);
							setText(null);
						} else {
							setText(null);
							setGraphic(null);
						}
						this.setPrefWidth(70);
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
		itemsIndexed.setMaxWidth(Integer.MAX_VALUE * 0.5f);
		lastIndexed.setMaxWidth(Integer.MAX_VALUE);
		removeBox.setMinWidth(70);
		removeBox.setMaxWidth(Integer.MAX_VALUE * 0.1f);

		indexedDirectories.getColumns().addAll(path, itemsIndexed, lastIndexed, removeBox);

		// indexedDirectories.setSelectionModel(null);

		indexedDirectories.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		TreeItem<IndexDirectoryModel> root = new RecursiveTreeItem<IndexDirectoryModel>(indexedDirectoryData,
				RecursiveTreeObject::getChildren);
		indexedDirectories.setRoot(root);
		indexedDirectories.setShowRoot(false);

		TreeTableColumn<LocalTrackModel, LocalTrackModel> titleLocal = new JFXTreeTableColumn<>("Title");

		titleLocal.setCellValueFactory(value -> {
			return new SimpleObjectProperty<LocalTrackModel>(value.getValue().getValue());
		});

		titleLocal.setCellFactory((
				TreeTableColumn<LocalTrackModel, LocalTrackModel> param) -> new JFXTreeTableCell<LocalTrackModel, LocalTrackModel>() {

					@Override
					protected void updateItem(LocalTrackModel item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							setText(null);

							// new Label(Integer.toString(item.getTrackLength().get())

							VBox wrapper = new VBox();
							wrapper.getChildren().addAll(new Label(item.getTitle().get()),
									new Label("Album: " + item.getAlbum().get()));
							setGraphic(wrapper);
						} else {
							setText(null);
							setGraphic(null);
						}
					}

				});
//
//		TreeTableColumn<LocalTrackModel, String> albumLocal = new JFXTreeTableColumn<>("Album");
//
//		albumLocal.setCellValueFactory(value -> {
//			return value.getValue().getValue().getAlbum();
//		});

//		TreeTableColumn<LocalTrackModel, String> pathLocal = new JFXTreeTableColumn<>("Path");
//
//		pathLocal.setCellValueFactory(value -> {
//			return value.getValue().getValue().getPath();
//		});

		musicLibraryView.getColumns().addAll(titleLocal/* , albumLocal, pathLocal */);

		musicLibraryView.setRowFactory(new Callback<TreeTableView<LocalTrackModel>, TreeTableRow<LocalTrackModel>>() {
			@Override
			public TreeTableRow<LocalTrackModel> call(TreeTableView<LocalTrackModel> param) {
				TreeTableRow<LocalTrackModel> row = new TreeTableRow<>();

				row.setOnDragDetected(event -> {
					// TODO link!
					Dragboard db = row.startDragAndDrop(TransferMode.ANY);

					/* Put a string on a dragboard */
					ClipboardContent content = new ClipboardContent();
					LocalTrackModel trackModel = row.getItem();
					content.put(dragTrackFormat, trackModel);

					// content.putString(trackModel.getPath().get());
					db.setContent(content);

					event.consume();

				});

				row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

					if (e.getClickCount() == 2) {

						if (currentlyActiveDevice != null) {

							LocalTrackModel trackModel = row.getItem();

							// String title, String creator, String albumArtist, String album, String
							// albumArtURI
							TrackMetadata meta = new TrackMetadata(trackModel.getTitle().get(), "", "",
									trackModel.getAlbum().get(), "");
							try {

								// x-rincon-mp3radio://
								String playbackPath = "http://" + fileProvider.toMappedPath(trackModel.getPath().get());

								// Add after the current song
								int curQueueIndex = currentlyActiveDevice.getCurrentTrackInfo().getQueueIndex();

								currentlyActiveDevice.addToQueue(curQueueIndex + 1, playbackPath, meta);
								if (!currentlyActiveDevice.getPlayState().equals(PlayState.PLAYING)) {
									currentlyActiveDevice.playFromQueue(curQueueIndex + 1);
								}
								// We will get a notification about queue update but it might take a few
								// seconds. Speed it up
								rebuildQueueList();
							} catch (IOException | SonosControllerException e1) {
								e1.printStackTrace();
							}

						}

					}

				});

				return row;
			}
		});

		musicLibraryView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		musicLibraryView
				.setRoot(new RecursiveTreeItem<LocalTrackModel>(localMusicData, RecursiveTreeObject::getChildren));
		musicLibraryView.setShowRoot(false);
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
					public void playStateChanged(PlayState playState) {
						switch (playState) {
						case ERROR:
							curPlayPauseImage = playImg;
							curPlayPauseImageHoover = playImgHoover;
							playPauseBtn.setImage(curPlayPauseImage);
							break;
						case PAUSED_PLAYBACK:
							curPlayPauseImage = playImg;
							curPlayPauseImageHoover = playImgHoover;
							playPauseBtn.setImage(curPlayPauseImage);
							break;
						case PLAYING:
							curPlayPauseImage = pauseImg;
							curPlayPauseImageHoover = pauseImgHoover;
							playPauseBtn.setImage(curPlayPauseImage);
							break;
						case STOPPED:
							curPlayPauseImage = playImg;
							curPlayPauseImageHoover = playImgHoover;
							playPauseBtn.setImage(curPlayPauseImage);
							break;
						case TRANSITIONING:
							break;
						default:
							break;

						}

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

			TrackMetadata currentlyPlaying = null;
			try {
				currentlyPlaying = currentlyActiveDevice.getCurrentTrackInfo().getMetadata();
			} catch (IOException | SonosControllerException e1) {
				e1.printStackTrace();
			}

			List<TrackMetadata> queueInfo;
			try {
				queueInfo = currentlyActiveDevice.getQueue(0, Integer.MAX_VALUE);
				for (TrackMetadata t : queueInfo) {

					// We can't simply go by index as we want to be able to sort the table in the
					// future?
					TrackMetadataModel tm = new TrackMetadataModel(t, currentlyActiveDevice);
					if (currentlyPlaying != null) {
						if (currentlyPlaying.getAlbum().equals(t.getAlbum())
								&& currentlyPlaying.getTitle().equals(t.getTitle())) {
							tm.getCurrentlyPlayingProperty().set(true);
						}
					}

					currentPlaylistData.add(tm);
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
						// TODO we shouldn't rebuild the entire queue. anyways go for it now
						rebuildQueueList();
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

		System.out.println("Setup position slider animation");

		positionAnimation.pause();

		int duration = currentTrack.getDuration();

		// Either a radio stream or a custom file set by playUri.
		if (duration == 0) {
			// This is escaped isn't it? extract the data as we would have found it in the
			// database

			String uriEscaped = currentTrack.getUri();

			// strip http://
			if (uriEscaped.startsWith("http://")) {
				uriEscaped = uriEscaped.substring(7);
			}

			// Strip file provider prefix
			String unmapped = fileProvider.toUnmappedPath(uriEscaped);

			// TODO guava does not offer a proper decode method. we decode and encode with
			// different classes!!!
			// maybe time to cut guava from the project?
			try {
				unmapped = URLDecoder.decode(unmapped, "UTF-8");
				unmapped = unmapped.replace("/", "\\");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			System.out.println(unmapped);
			try {
				duration = db.getSongDuration(unmapped);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		songPositionSlider.setMin(0);
		songPositionSlider.setMax(duration);
		songPositionSlider.setValue(currentTrack.getPosition());

		positionAnimation.stop();
		positionAnimation.getKeyFrames().clear();

		int initialOffset = currentTrack.getPosition();

		// Weak listener should be gced automatically? TODO care about memory leak?
		SimpleStringProperty durationLabelTest = new SimpleStringProperty();
		SimpleIntegerProperty currentPositionProperty = new SimpleIntegerProperty(initialOffset);

		int songDurc = duration;

		currentPositionProperty.addListener((obs, oldVal, newVal) -> {
			// new value will be updates
			durationLabelTest.set(ParserHelper.secondsToFormatedTimestamp(newVal.intValue()) + " / "
					+ ParserHelper.secondsToFormatedTimestamp(songDurc));
		});

		Platform.runLater(() -> {
			durationProgressLabel.textProperty().bind(durationLabelTest);
		});

		KeyValue kv = new KeyValue(songPositionSlider.valueProperty(), duration);
		KeyValue kv1 = new KeyValue(currentPositionProperty, duration);

		double timeLeft = duration - currentTrack.getPosition();

		System.out.println(timeLeft + " " + duration + " " + currentTrack.getPosition());

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
