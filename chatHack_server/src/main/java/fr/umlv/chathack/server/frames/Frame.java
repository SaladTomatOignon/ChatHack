package fr.umlv.chathack.server.frames;

public interface Frame {
	
	
	/**
	 * Perform the frame action
	 */
	void accept();
	
	/**
	 * Retrieve the bytes constituting the frame
	 * 
	 * @return The content byte of the frame
	 */
	byte[] getBytes();
	
	
	/**
	 * Retrieve the full size of the frame in byte
	 * 
	 * @return The frame size
	 */
	int size();
}
