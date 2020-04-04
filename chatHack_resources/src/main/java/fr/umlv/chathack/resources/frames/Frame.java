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
	 * Perform the frame action on the given client.
	 * 
     * @param client The client to perform the action.
	 */
	default void accept(ClientVisitor client) {
		// Do nothing by default.
	};
	
	/**
	 * Perform the frame action on the given server.
	 * 
     * @param server The server to perform the action.
	 */
	default void accept(ServerVisitor server) {
		// Do nothing by default.
	};
}
