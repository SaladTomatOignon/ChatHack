package fr.umlv.chathack.contexts;

import java.util.logging.Level;

import fr.umlv.chathack.resources.frames.Frame;

public interface Server {
	static final int BUFFER_SIZE = 1024;
	
	/**
	 * Log a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 */
	void log(Level level, String msg);
	
	/**
	 * Log a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 * @param thrw Throwable associated with log message.
	 */
	void log(Level level, String msg, Throwable thrw);
	
	/**
	 * Broadcast a frame to every connected and authenticated clients.
	 * 
	 * @param frame The frame to broadcast.
	 */
	void broadcast(Frame frame);
	
    /**
     * Send a frame to a specific client authenticated to the server.
     * 
     * @param frame The frame to send.
     * @param dest The recipient client's login.
     * 
     * @throws IllegalArgumentException If the recipient client is not authenticated to the server.
     */
	void sendFrame(Frame frame, String dest) throws IllegalArgumentException;
	
    /**
     * Check if the user is registered in the server.
     * 
     * @param login
     * @param password
     * @return True if the user's logins are valid.
     */
	boolean isRegistered(String login, String password);
	
    /**
     * Add the client to the authenticated list.
     * 
     * @param login The login's client.
     * @param ctx The client's context.
     */
	void authenticateClient(String login, ServerContext ctx);
	
    /**
     * Determine if the client with the given login is connected and authenticated to the server or not.
     * 
     * @param login
     * @return True if the client is connected and authenticated.
     */
	boolean clientAuthenticated(String login);
}
