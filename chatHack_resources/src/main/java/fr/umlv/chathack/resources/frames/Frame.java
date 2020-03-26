package fr.umlv.chathack.resources.frames;

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
	 * Perform the frame action
	 */
	void accept();
}
