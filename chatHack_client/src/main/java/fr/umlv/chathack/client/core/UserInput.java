package fr.umlv.chathack.client.core;

import java.util.Objects;
import java.util.Scanner;

import fr.umlv.chathack.client.parser.MalFormedFrameException;
import fr.umlv.chathack.client.parser.Parser;

public class UserInput {
	private final ChatHackClient client;
	
	public UserInput(ChatHackClient client) {
		this.client = Objects.requireNonNull(client);
	}
	
	public void interact() {
		try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
			while ( scanner.hasNextLine() ) {
				String line = scanner.nextLine();
				
				try {
					Parser.parse(client, line);
				} catch (MalFormedFrameException e) {
					System.err.println("Invalid syntax" + (e.getMessage() == null ? "" : " : " + e.getMessage()));
					continue;
				}
			}
		}
	}
}
