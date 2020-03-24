package fr.umlv.chathack.server.main;

import java.io.IOException;

import fr.umlv.chathack.server.core.ChatHackServer;

public class Main {

	private static void usage() {
		System.out.println("Usage : ChatHackServer port");
	}
	
	/**
	 * Check if arg correspond to a valid port number
	 * 
	 * @param arg The string to check
	 * @return True if the arg corresponds to a valid port number
	 */
	private static boolean isPortValid(String arg) {
		int port;
		
		try {
			port = Integer.parseInt(arg);
		} catch ( NumberFormatException e ) {
			return false;
		}
		
		return 1 <= port && port <= 65535;
	}
	
	public static void main(String[] args) throws IOException {
		if ( args.length != 1 ) {
			usage();
			return;
		}
		
		if ( !isPortValid(args[0]) ) {
			System.out.println("Port number must be between 1 and 65535 inclusiv");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		ChatHackServer server = new ChatHackServer(port);
		server.launch();
	}

}
