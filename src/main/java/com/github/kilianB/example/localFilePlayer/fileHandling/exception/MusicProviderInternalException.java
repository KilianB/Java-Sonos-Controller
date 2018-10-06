package com.github.kilianB.example.localFilePlayer.fileHandling.exception;

/**
 * Exception to be thrown if the music provider engine failed to make a request which is unlikely to 
 * be repaired. The Smart house should actively try to avoid this engine from now on and use a fallback if possible
 * @author Kilian
 *
 */
public class MusicProviderInternalException extends MusicProviderException{

	private static final long serialVersionUID = 1L;

	public MusicProviderInternalException(String string) {
		super(string);
	}

}