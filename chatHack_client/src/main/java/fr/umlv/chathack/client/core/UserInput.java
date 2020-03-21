package fr.umlv.chathack.client.core;

import java.util.Objects;
import java.util.Scanner;

import fr.umlv.chathack.client.frames.Frame;
import fr.umlv.chathack.client.parser.Parser;

public class UserInput {
	private final ChatHackClient client;
	
	public UserInput(ChatHackClient client) {
		this.client = Objects.requireNonNull(client);
	}
	
	public void interact() {
		try (Scanner scanner = new Scanner(System.in)) {
			while ( scanner.hasNextLine() ) {
				String line = scanner.nextLine();
				Frame frame = null;
				
				try {
					frame = Parser.parse(line);
				} catch (Exception e) { // Eventuellement créer une "Mal formated frame Exception"
					System.out.println("Invalid syntax");
					continue;
				}
				
				client.queueMessage(frame);
			}
		}
	}
}
