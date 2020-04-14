package fr.umlv.chathack.resources.frames;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

public interface ClientVisitor {
	
	/**
	 * Log a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 */
	void log(Level level, String msg);

	/**
	 * Abort the private communication request with the given client.
	 * 
	 * @param login The client's login.
	 */
	void abortPrivateCommunicationRequest(String login);
	
    /**
     * Connect the client to the server corresponding to another client's private server.<br>
     * 
     * @param server The server to connect to.
     * @param login The login of the server owner.
     * @param tokenID The id assigned to the communication with the server.
     * 
     * @throws IOException 
     */
	void connectToPrivateServer(InetSocketAddress server, String login, int tokenID) throws IOException;
	
    /**
	 * Try to authenticate the given client with the given token ID.
     * 
     * @param id The token ID.
     * @param login The login.
     */
    void tryAuthenticate(int id, String login);

    /**
     * Print a private message, if the client is authenticated.
     * 
     * @param message The message
     */
	void printPrivateMessage(String message);

	/**
	 * Ask the client if he wishes to establish a private communication with the given client.
	 * 
	 * @param login The client's login.
	 */
	void askForPrivateCommunication(String login);
}
