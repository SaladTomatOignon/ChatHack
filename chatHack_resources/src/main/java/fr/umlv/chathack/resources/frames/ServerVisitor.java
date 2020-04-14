package fr.umlv.chathack.resources.frames;

import java.io.IOException;
import java.net.InetAddress;

public interface ServerVisitor {
	
    /**
     * Try to login the client to the server.
     * It succeed if the logins are registered by the server.
     * 
     * @param login
     * @param password
     * 
     * @return 0 if login succeed or 1 if it failed.
     */
    byte tryLogin(String login, String password);
    
    /**
     * Try to login the client to the server.
     * It succeed if the given login is not already authenticated by the server.
     * 
     * @param login
     * 
     * @return 0 if login succeed or 2 if it failed.
     */
    byte tryLogin(String login);
    
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
