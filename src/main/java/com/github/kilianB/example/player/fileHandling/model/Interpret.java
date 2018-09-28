package com.github.kilianB.example.player.fileHandling.model;

import java.util.Date;

public class Interpret {
	String name;
	String description;
	Date birthyear;
	
	public Interpret(String name, String description, Date birthyear) {
		super();
		this.name = name;
		this.description = description;
		this.birthyear = birthyear;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the birthyear
	 */
	public Date getBirthyear() {
		return birthyear;
	}

	/**
	 * @param birthyear the birthyear to set
	 */
	public void setBirthyear(Date birthyear) {
		this.birthyear = birthyear;
	}


	//TODO Maybe create a proper Factory class for this?
	public static Interpret createUnknownInterpret() {
		return new Interpret("Unknown","",null);
	}
	
	
	
	
}