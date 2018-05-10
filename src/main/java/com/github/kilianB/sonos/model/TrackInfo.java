package com.github.kilianB.sonos.model;

import com.github.kilianB.sonos.ParserHelper;

/**
 * 
 * @author vmichalak
 * @author Kilian
 */
public class TrackInfo {
	private final int queueIndex;
	private final int duration;
	private final int position;
	private final String uri;
	private final TrackMetadata metadata;

	public TrackInfo(int queueIndex, int duration, int position, String uri, TrackMetadata metadata) {
		this.queueIndex = queueIndex;
		this.duration = duration;
		this.position = position;
		this.uri = uri;
		this.metadata = metadata;
	}

	public int getQueueIndex() {
		return queueIndex;
	}

	public int getDuration() {
		return duration;
	}

	public String getDurationAsString() {
		return ParserHelper.secondsToFormatedTimestamp(duration);
	}

	public int getPosition() {
		return position;
	}

	/**
	 * Returns the current position of the song in the format HH:MM:SS
	 * 
	 * @return
	 */
	public String getPositionAsString() {
		return ParserHelper.secondsToFormatedTimestamp(position);
	}

	public String getUri() {
		return uri;
	}

	public TrackMetadata getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return "TrackInfo{" + "queueIndex=" + queueIndex + ", duration='" + duration + '\'' + ", position='" + position
				+ '\'' + ", uri='" + uri + '\'' + ", metadata=" + metadata + '}';
	}

	/**
	 * Compare if two track infos point to the same song
	 * 
	 * @param infoToCompareTo
	 * @return
	 */
	public boolean sameBaseTrack(TrackInfo infoToCompareTo) {
		return (this.uri.equals(infoToCompareTo.uri) && this.metadata.equals(infoToCompareTo.metadata));
	}

}
