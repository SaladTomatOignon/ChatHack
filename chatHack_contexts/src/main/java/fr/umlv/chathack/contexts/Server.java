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
	 * Send a request to the database server in order to determinate if
	 * the pair login/password is registered in the database.
	 * 
	 * @param login
	 * @param password
	 * @param ctx The client context
	 */
	void sendAuthRequest(String login, String password, ServerContext ctx);
	
	/**
	 * Sends a request to the database server in order to determinate if
	 * the the login exists in the database.
	 * 
	 * @param login
	 * @param ctx The client context
	 */
	void sendAuthRequest(String login, ServerContext ctx);
	
	/**
	 * Try to authenticate the client corresponding to the given ID according
	 * to the database response.
	 * 
	 * @param id The client's identifier
	 * @param positiveAnswer The database response, True if positive, False if negative.
	 */
	void tryAuthenticate(long id, boolean positiveAnswer);
}
