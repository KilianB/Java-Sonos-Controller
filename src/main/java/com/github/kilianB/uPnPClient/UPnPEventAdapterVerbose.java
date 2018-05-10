package com.github.kilianB.uPnPClient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * A default {@link UPnPEventListener} implementation logging the content of events 
 * allowing the first inspection of the received content.
 * 
 * @author Kilian
 *
 */
public class UPnPEventAdapterVerbose implements UPnPEventListener{

	private final static Logger LOGGER = LogManager.getLogger();
	
	private final String servicePath;
	
	public UPnPEventAdapterVerbose(String servicePath) {
		this.servicePath = servicePath;
	}
	
	@Override
	public void initialEventReceived(UPnPEvent event) {
		LOGGER.debug("inital event");
		LOGGER.info(event);
		LOGGER.info(event.getBodyAsString());
	}

	@Override
	public void eventReceived(UPnPEvent event) {
		LOGGER.debug("value changed event");
		LOGGER.info(event);
		LOGGER.info(event.getBodyAsString());
		for(Element e : event.getProperties()) {
			LOGGER.info(e);
		}
	}

	@Override
	public void eventSubscriptionExpired() {
		LOGGER.error("Event subscription for: {} expired",servicePath);
	}

	@Override
	public void renewalFailed(Exception e) {
		LOGGER.error("Renewal subscroption for: {} failed",servicePath);
	}

	@Override
	public void unsubscribed() {
		LOGGER.info("Unsubscribed from {}",servicePath);
	}
	
}