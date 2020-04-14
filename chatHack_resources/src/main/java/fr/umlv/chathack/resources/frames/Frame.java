package fr.umlv.chathack.resources.frames;

import java.io.IOException;

public interface Frame {
	
	/**
	 * Retrieve the frame content size in bytes
	 * 
	 * @return The frame size
	 */
	int size();
	
	/**
	 * Retrieve the bytes constituting the frame
	 * 
	 * @return The content bytes of the frame
	 */
	byte[] getBytes();
	
	/**
	 * Perform the frame action on the given client.
	 * 
     * @param client The client to perform the action.
     * 
	 * @throws IOException 
	 */
	default void accept(ClientVisitor client) throws IOException {
		// Do nothing by default.
	};
	
	/**
	 * Perform the frame action on the given server.
	 * 
     * @param server The server to perform the action.
     * 
	 * @throws IOException 
	 */
	default void accept(ServerVisitor server) throws IOException {
		// Do nothing by default.
	};
}
