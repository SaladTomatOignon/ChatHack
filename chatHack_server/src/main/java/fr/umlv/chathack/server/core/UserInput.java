package fr.umlv.chathack.server.core;

import java.util.Objects;
import java.util.Scanner;

public class UserInput {
	private final ChatHackServer server;
	
	public UserInput(ChatHackServer client) {
		this.server = Objects.requireNonNull(client);
	}
	
	public void interact() {
		try (Scanner scanner = new Scanner(System.in)) {
			while ( scanner.hasNextLine() ) {
				String line = scanner.nextLine();
				
				switch (line.toUpperCase()) {
					case "INFO":
						server.showInfos();
						break;
					case "SHUTDOWN":
						server.shutdown();
						break;
					case "SHUTDOWNNOW":
						server.shutdownnow();
						break;
					default:
						System.out.println("Invalid command");
						break;
				}
			}
		}
	}
}
