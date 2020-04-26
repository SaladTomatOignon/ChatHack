package fr.umlv.chathack.client.parser;

import fr.umlv.chathack.client.core.ChatHackClient;

public class Parser {
	
	/**
	 * Parses the string line in parameter and performs the action
	 * according to the syntax.
	 * 
	 * @param client The client to take the action.
	 * @param line The line to parse.
	 * 
	 * @throws MalFormedFrameException If the line is syntactically incorrect.
	 */
	static public void parse(ChatHackClient client, String line) throws MalFormedFrameException {
		try {
			if ( line.startsWith("@") ) {
				String[] sequences = line.split(" ", 2);
				String recipientClient = sequences[0].substring(1);
				String msg = sequences[1];
				
				if ( msg.isBlank() ) {
					throw new MalFormedFrameException("Messages can not be blank.");
				}
				
				client.sendPrivateMessage(msg, recipientClient);
			} else if ( line.startsWith("/") ) {
				String[] sequences = line.split(" ", 2);
				String recipientClient = sequences[0].substring(1);
				String fileName = sequences[1];
				
				if ( fileName.isBlank() ) {
					throw new MalFormedFrameException("You must enter a file name.");
				}
				
				client.sendFile(fileName, recipientClient);
			} else {
				if ( line.isBlank() ) {
					throw new MalFormedFrameException("Messages can not be blank.");
				}
				
				client.sendPublicMessage(line);
			}
		} catch (IndexOutOfBoundsException e) {
			throw new MalFormedFrameException();
		}
	}
}
