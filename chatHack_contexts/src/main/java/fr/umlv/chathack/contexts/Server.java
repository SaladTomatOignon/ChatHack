package fr.umlv.chathack.contexts;

public interface Server {
	static final int BUFFER_SIZE = 1024;
	
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
     */
	void authenticateClient(String login);
	
    /**
     * Determine if the client with the given login is connected and authenticated to the server or not.
     * 
     * @param login
     * @return True if the client is connected and authenticated.
     */
	boolean clientAuthenticated(String login);
}
