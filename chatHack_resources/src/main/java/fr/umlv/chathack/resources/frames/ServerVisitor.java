package fr.umlv.chathack.resources.frames;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;

public interface ServerVisitor {
	
	/**
	 * Log a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 */
	void log(Level level, String msg);
	
    /**
     * Try to login the client to the server.
     * It succeeds if the logins are registered in the database.
     * 
     * @param login
     * @param password
     */
    void tryLogin(String login, String password);
    
    /**
     * Try to login the client to the server.
     * It succeeds if the given login is not already authenticated by the server
     * and does not exists in the database.
     * 
     * @param login
     */
    void tryLogin(String login);
    
    /**
     * Authenticates or deny the authentication of a client
     * according to the database answer.
     * 
     * @param id The identifier of the request to the database.
     * @param positiveAnswer The response, True if positive, False if negative.
     */
    void answerFromDatabase(long id, boolean positiveAnswer);
    
    /**
     * Broadcast a public message to the server, so every connected client
     * can receive it.
     * 
     * @param message The message to broadcast.
     * 
     * @throws IllegalStateException If the client is not able to send a broadcast a frame.
     */
    void broadcastMessage(String message) throws IllegalStateException;
    
    /**
     * Send a frame to a specific client connected to the server.
     * 
     * @param frame The frame to send.
     * @param dest The recipient client's login.
     * 
     * @throws IllegalStateException If the client is not able to send a frame to another client.
     * @throws IllegalArgumentException If the recipient client does not exist or is not able to receive a frame.
     */
    void sendFrame(Frame frame, String dest) throws IllegalStateException, IllegalArgumentException;
    
    /**
     * Send a frame to this client.
     * 
     * @param frame The frame to send.
     */
    void sendBackFrame(Frame frame);
    
    /**
     * Retrieve the client's login.
     * 
     * @return The client login.
     */
    String getLogin();

    /**
     * Retrieve the client's IP address.
     * 
     * @return The client's IP address.
     * @throws IOException 
     */
	InetAddress getInetAddress() throws IOException;
}
