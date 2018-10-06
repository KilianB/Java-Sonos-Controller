package com.github.kilianB.example.player.fileHandling.exception;

public class NoSuitableSongFound extends MusicProviderException{

	private static final long serialVersionUID = 1L;

	public NoSuitableSongFound(String string) {
		super(string);
	}
	public NoSuitableSongFound(){
		super();
	};
}