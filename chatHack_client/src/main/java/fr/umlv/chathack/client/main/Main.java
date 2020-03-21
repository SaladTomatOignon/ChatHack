package fr.umlv.chathack.client.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import fr.umlv.chathack.client.core.ChatHackClient;
import fr.umlv.chathack.client.core.UserInput;

public class Main {
	
	private static void usage() {
		System.out.println("Usage : ChatHackClient ipAdress port directory login [password]");
	}

	public static void main(String[] args) throws IOException {
		if ( args.length != 4 && args.length != 5 ) {
			usage();
			return;
		}
		
		if ( !Files.isDirectory(Paths.get(args[2])) ) {
			System.out.println("The given directory path is not valid");
			return;
		}
		
		try {
			Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("Port number not valid");
			return;
		}
		
		ChatHackClient client = args.length == 4 ? new ChatHackClient(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[3])
												 : new ChatHackClient(new InetSocketAddress(args[0], Integer.parseInt(args[1])), args[3], args[4]);
		client.launch();
		
		UserInput input = new UserInput(client);
		input.interact();
	}

}
