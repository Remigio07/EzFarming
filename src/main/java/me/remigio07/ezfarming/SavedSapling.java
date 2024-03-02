package me.remigio07.ezfarming;

import org.bukkit.Location;
import org.bukkit.Material;

public class SavedSapling {
	
	private String id;
	private Location location;
	private Material type;
	
	SavedSapling(String id, Location location, Material type) {
		this.id = id;
		this.location = location;
		this.type = type;
	}
	
	public String getID() {
		return id;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public Material getType() {
		return type;
	}
	
}
