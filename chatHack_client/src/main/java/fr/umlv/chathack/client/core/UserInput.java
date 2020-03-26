package fr.umlv.chathack.client.core;

import java.util.Objects;
import java.util.Scanner;

import fr.umlv.chathack.client.parser.MalFormedFrameException;
import fr.umlv.chathack.client.parser.Parser;
import fr.umlv.chathack.resources.frames.Frame;

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
				} catch (MalFormedFrameException e) {
					System.out.println("Invalid syntax");
					continue;
				}
				
				client.queueMessageToPublicServer(frame);
			}
		}
	}
}
