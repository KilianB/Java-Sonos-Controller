package com.github.kilianB.example.localFilePlayer.fileHandling.model;

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
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((birthyear == null) ? 0 : birthyear.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interpret other = (Interpret) obj;
		if (birthyear == null) {
			if (other.birthyear != null)
				return false;
		} else if (!birthyear.equals(other.birthyear))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	//TODO Maybe create a proper Factory class for this?
	public static Interpret createUnknownInterpret() {
		return new Interpret("Unknown","",null);
	}
	
	
	
	
}