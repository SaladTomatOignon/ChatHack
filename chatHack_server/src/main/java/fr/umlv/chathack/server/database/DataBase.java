package fr.umlv.chathack.server.database;

import java.util.HashMap;
import java.util.Map;

public class DataBase {
	private final Map<String, String> database;
	
	private DataBase() {
		this.database = new HashMap<>();
		
		this.database.put("Samy", "123");
		this.database.put("Armand", "456");
	}
	
	public static DataBase connect() {
		return new DataBase();
	}
	
	public boolean isRegistered(String login, String password) {
		return database.containsKey(login) && database.get(login).equals(password);
	}
}
