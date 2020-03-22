package fr.umlv.chathack.client.parser;

import fr.umlv.chathack.client.frames.Frame;

public class Parser {
	
	/**
	 * Parse the string line in parameter to get the corresponding frame to send.
	 * 
	 * @param line The line to parse
	 * @return The frame corresponding to the line
	 * @throws MalFormedFrameException If the line is syntaxically incorrect
	 */
	static public Frame parse(String line) throws MalFormedFrameException {
		if ( line.startsWith("/") || line.startsWith("@") ) {
			throw new UnsupportedOperationException(); // TODO
		} else {
			// return new PublicMessage(line);
			return null;
		}
	}
}
