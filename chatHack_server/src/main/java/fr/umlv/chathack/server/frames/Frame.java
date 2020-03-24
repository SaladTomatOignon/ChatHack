package fr.umlv.chathack.server.frames;

import fr.umlv.chathack.server.core.Context;

public interface Frame {
	
	/**
	 * Retrieve the full size of the frame in bytes
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
	 * Perform the frame action
	 */
	void accept(Context ctx);
}
