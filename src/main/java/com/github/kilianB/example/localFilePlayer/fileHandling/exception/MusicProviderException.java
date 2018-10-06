package com.github.kilianB.example.localFilePlayer.fileHandling.exception;

public abstract class MusicProviderException extends Exception{

	private static final long serialVersionUID = 1L;

	public MusicProviderException(String string) {
		super(string);
	}
	
	public MusicProviderException(){
		super();
	};
	
}