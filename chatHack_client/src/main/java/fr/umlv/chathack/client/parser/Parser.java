package fr.umlv.chathack.client.parser;

import fr.umlv.chathack.client.core.ChatHackClient;

public class Parser {
	
	/**
	 * Parse the string line in parameter and performs the action
	 * according to the syntax.
	 * 
	 * @param client The client to take the action.
	 * @param line The line to parse.
	 * 
	 * @throws MalFormedFrameException If the line is syntactically incorrect.
	 */
	static public void parse(ChatHackClient client, String line) throws MalFormedFrameException {
		try {
			if ( line.startsWith("/") ) {
				String[] sequences = line.split(" ", 2);
				String recipientClient = sequences[0].substring(1);
				String msg = sequences[1];
				
				client.sendPrivateMessage(msg, recipientClient);
			} else if ( line.startsWith("@") ) {
				throw new UnsupportedOperationException(); // TODO
			} else {
				client.sendPublicMessage(line);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new MalFormedFrameException();
		}
	}
}
