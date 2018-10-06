package com.github.kilianB.example.localFilePlayer.fileHandling;

import java.io.File;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;


/**
 * Start a file server which maps a local directory to a fixed ip address allowing network devices
 * to query data from this place.
 * @author Kilian
 *
 */
public class NetworkFileProvider {

	//Settings
	private String host;
	private int port;
	private String mapPrefix;
	
	//Internal
	private final Undertow server;
	private final PathHandler pathHandler = new PathHandler();
	private HashSet<String> mappedFolders = new HashSet<>();
	
	
	public NetworkFileProvider(String host, int port, String[] allowedFileExtensions){
	
		this.host = host;
		this.port = port;
		this.mapPrefix = host+":"+port + "/";
	
		server = Undertow.builder().addHttpListener(port, host, pathHandler).build();
		
		//Undertow wraps it's exception in a runtime exception.
		try {
		server.start();
		}catch(RuntimeException e) {
			Throwable t = e.getCause();
			if(t instanceof java.net.BindException) {
				LOGGER.severe("Port already bound by another program. Please check if the smart server is running "
						+ " twice or swap to a free port.");
			}else {
				LOGGER.severe(t.toString());
			}
		}
	}
	

	/**
	 * Maps the given folder to host:port/folderPath allowing network access by quering this address
	 * @param directory
	 * @return true if folder was successfully mapped. False otherwise
	 */
	public synchronized boolean mapFolder(File directory) {
		
		if(directory.isDirectory()) {
			
			//Escape windows path
			String prefixPath = directory.getAbsolutePath().replace("\\", "/");
			
			if(mappedFolders.contains(prefixPath)) {
				LOGGER.warning("Folder " + prefixPath + " already mapped. Skip request");
				return false;
			}
			
			FileResourceManager sharedFolderRessourceManager = new FileResourceManager(directory, 0);
		
			ResourceHandler sharedRessourceManager = new ResourceHandler(sharedFolderRessourceManager);
			//TODO MIME Mapping and index directoy listing disabled for more protection	
			sharedRessourceManager.setDirectoryListingEnabled(true);
			
			pathHandler.addPrefixPath("/"+prefixPath, sharedRessourceManager);
		
			mappedFolders.add(prefixPath);
			LOGGER.log(Level.INFO,"map "+directory.getAbsolutePath()+" to " + mapPrefix + prefixPath);
		}else {
			throw new IllegalArgumentException("Please provide a folder and not a file");
		}
		
		return true;
	}

	
	public String toMappedPath(String originalLocation) {
		return mapPrefix + originalLocation;
	}
	
	public String toUnmappedPath(String mappedLocation) {
		if(mappedLocation.startsWith(mapPrefix)) {
			return mappedLocation.substring(mapPrefix.length());
		}
		return mappedLocation;
	}
	
	public void deinit(){
		server.stop();
	}
	

	private	static final Logger	LOGGER = Logger.getLogger(NetworkFileProvider.class.getName());


	/**
	 * @return
	 */
	public String getMapPrefix() {
		return mapPrefix;
	}	
}
