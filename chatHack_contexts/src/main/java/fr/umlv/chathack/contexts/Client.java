package fr.umlv.chathack.contexts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

public interface Client {
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
     * Remove pending messages for 'login'.
     * 
     * @param login The client login to remove pending messages.
     */
	void clearPendingMessages(String login);
	
    /**
     * Connect the client to the server corresponding to another client's private server.<br>
     * Then register the server to selector and the server's owner to the private clients list.
     * 
     * @param server The server to connect to.
     * @param login The login of the server owner.
     * @param tokenID The id assigned to the communication with the server.
     * 
     * @throws IOException 
     */
	void connectToPrivateServer(InetSocketAddress server, String login, int tokenID) throws IOException;
	
    /**
     * Authenticates the given SelectionKey to this server if the given token ID is correct.<br>
	 * Authentication succeed if the pair (ID, login) exists in the pending client list.<br>
     * Removes the id from the "pending" list after authentication.
     * 
     * @param id The token ID.
     * @param login The login.
     * @param ctx The client Context to authenticate.
     */
    void tryAuthenticate(int id, String login, ClientContext ctx);
    
    /**
     * Add the given client to the "Asking clients list".<br>
     * Clients in this list can not receive private message until this
     * client reply with 'yes' or 'no'.
     * 
     * @param login The client's login.
     * @param ctx The client's context.
     */
    void addAskingClient(String login, ClientContext ctx);
    
    /**
     * Create a new file with the given name, and return a stream
     * in which it's possible to write content.
     * 
     * @param fileName The file name.
     * 
     * @return A new stream opened for writing.
     */
    FileOutputStream createNewFile(String fileName);
}
