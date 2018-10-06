package com.github.kilianB.example.player.util;

import java.io.File;

import com.google.common.net.UrlEscapers;

public class StringUtil {

	private static final char EXTENSION_SEPARATOR = '.';
	private static final char UNIX_SEPARATOR = '/';
	private static final char WINDOWS_SEPARATOR = '\\';
	
	
	/**
	 * Appache commons text filename utils. 
	 * @param f
	 * @return
	 */
	public static String getFileExtension(File f) {
	
		String fileName = f.getAbsolutePath();
	
		if (fileName == null)
			return null;
	
		int lastUnixPos = fileName.lastIndexOf(UNIX_SEPARATOR);
		int lastWindowsPos = fileName.lastIndexOf(WINDOWS_SEPARATOR);
		int lastSeperatorIndex = Math.max(lastUnixPos, lastWindowsPos);
		int extensionPos = fileName.lastIndexOf(EXTENSION_SEPARATOR);
		int indexOfExtension = (lastSeperatorIndex > extensionPos ? -1 : extensionPos);
	
		if (indexOfExtension == -1) {
			return "";
		} else {
			return fileName.substring(indexOfExtension + 1);
		}
	}
	
	/**
	 * Convert camel case to space separated individual words.
	 *
	 * @param input "ThisIsATestString"
	 * @return <b>output</b> "This Is A Test String"
	 */
	public static String addSpacesBetweenCapitalLetters(String input) {
		return input.replaceAll("(?<=\\p{L})(?=\\p{Lu})", " ");
	}

	

	/**
	 * Replaces all line breaks in a string by the given replacement. And empty string will simply return breaks.
	 * @param input
	 * @param replace
	 * @return
	 */
	public static String replaceLineBreaks(String input, String replace) {
		return input.replaceAll("\\r\\n|\\r|\\n", replace);
	}

	public static String enumToLowercase(Enum<?> e) {
		return e.name().toLowerCase();
	}

	/**
	 * Expected input default CAPITALIZED_WITH_UNDERSCORES naming convention
	 * 
	 * Valid enum names:
	 * <ul>
	 * 	<li>ENUMNAME</li>
	 *  <li>ENUM_NAME</li>
	 *  <li>ENUM_.._NAME</li>
	 * </ul>
	 * 
	 * 
	 * @param e
	 * @return name formated in lower camel case notation
	 */
	public static String enumNameToLowerCamelCase(Enum<?> e){
		//TODO fancy regex
		String[] exploded = e.name().toLowerCase().split("_");
		String returnValue = exploded[0];
		for(int i = 1; i < exploded.length;i++){
			returnValue += Character.toUpperCase(exploded[i].charAt(0)) + exploded[i].substring(1);
		}
		return returnValue;
	}

	/**
	 * Return a new string which consists out of count times the supplied string appended
	 * together.
	 * @param s
	 * @param count
	 * @return
	 */
	public static String constructRepetativeString(String s, int count) {
		//s.length()*count
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < count; i++) {
			sb.append(s);
		}
		return sb.toString();		
	}
	
	public static String decodeURLToURI(String url) {
		//backward slashes will get escaped therefore change it
		url = url.replace("\\", "/");
		return "x-file-cifs:" + UrlEscapers.urlFragmentEscaper().escape(url);
	}
	
	
	private final static long MS_IN_SEC = 1000;
	private final static long MS_IN_MIN = MS_IN_SEC * 60;
	private final static long MS_IN_HOUR = MS_IN_MIN * 60;
	private final static long MS_IN_DAY = MS_IN_HOUR * 24;
	
	/**
	 * Return a formated string representation of the time until the given moment in time. 
	 * 
	 * 
	 * 
	 * @param milis
	 * @return
	 */
	public static String milisToTimeFormated(long milis) {
		
		int days = (int) (milis / MS_IN_DAY);
		int hours = (int) (milis / MS_IN_HOUR % 24);
		int minutes = (int) (milis / MS_IN_MIN % 60);
		int seconds = (int) (milis / MS_IN_SEC % 60);
		
		StringBuilder sb = new StringBuilder();
		boolean set = false;
		if(days > 0) {
			sb.append(days);
			sb.append(" d ");
			set = true;
		}
		if(hours > 0 || set) {
			if(set) {
				sb.append(String.format(":%02d", hours));
			}else {
				sb.append(String.format("%02d", hours));
			}
			set = true;
		}
		if(minutes > 0 || set ) {
			if(set) {
				sb.append(String.format(":%02d", minutes));
			}else {
				sb.append(String.format("%02d", minutes));
			}
			set = true;
		}
		if(seconds > 0 ||set ) {
			if(set) {
				sb.append(String.format(":%02d", seconds));
			}else {
				sb.append(String.format("%02d", seconds));
			}
			set = true;
		}
		return sb.toString();
	}
	
	public static String milisToTimeFormatedVerbose(long milis) {
		
		int days = (int) (milis / MS_IN_DAY);
		int hours = (int) (milis / MS_IN_HOUR % 24);
		int minutes = (int) (milis / MS_IN_MIN % 60);
		int seconds = (int) (milis / MS_IN_SEC % 60);
		
		StringBuilder sb = new StringBuilder();
		boolean set = false;
		if(days > 0) {
			sb.append(days);
			sb.append(" days ");
			set = true;
		}
		if(hours > 0 || set) {
			sb.append(hours);
			sb.append(" hrs ");
			set = true;
		}
		if(minutes > 0 || set) {
			sb.append(minutes);
			sb.append(" min ");
			set = true;
		}
		if(seconds > 0 || set) {
			sb.append(seconds);
			sb.append(" sec");
			set = true;
		}
		return sb.toString();
	}

}
