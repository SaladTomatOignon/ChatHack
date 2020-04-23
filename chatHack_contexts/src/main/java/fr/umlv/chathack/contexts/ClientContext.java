package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import fr.umlv.chathack.contexts.FileContext.State;
import fr.umlv.chathack.resources.frames.ClientVisitor;
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.readers.FrameReader;
import fr.umlv.chathack.resources.readers.Reader;

public class ClientContext extends Context implements ClientVisitor {
    final private Client client;
    final private Map<Integer, FileContext> files;
    
    private String login; // The client login, may be null.
    private int tokenID; // The ID used to communicate the client, -1 if not assigned.
	
    public ClientContext(SelectionKey key, Client client) {
    	this(key, client, FrameReader.class);
    }
    
    public <T extends Reader> ClientContext(SelectionKey key, Client client, Class<T> reader) {
    	super(key, reader);
    	
        this.client = client;
        this.files = new HashMap<>();
        
        this.login = null;
        this.tokenID = -1;
    }
    
    /**
     * Set the client login.
     * 
     * @param login The login to set.
     */
    public void setLogin(String login) {
    	this.login = Objects.requireNonNull(login);
    }
    
    /**
     * Retrieve the client's login.
     * 
     * @return The client's login.
     * 
     * @throws IllegalStateException If no login is assigned to this client.
     */
    public String getLogin() {
    	if ( Objects.isNull(login) ) {
    		throw new IllegalStateException("No login is assigned to this client");
    	}
    	
    	return login;
    }
    
    /**
     * Set the client's token ID. Used for private communication.
     * 
     * @param tokenID The token ID to set.
     */
    public void setTokenId(int tokenID) {
    	if ( tokenID < 0 ) {
    		throw new IllegalArgumentException("Token ID must be positive");
    	}
    	
    	this.tokenID = tokenID;
    }
    
    /**
     * Retrieve the client's token ID. Used for private communication.
     * 
     * @return The client's token ID.
     * 
     * @throws IllegalStateException If no token ID is assigned to this client.
     */
    public int getTokenId() {
    	if ( tokenID == -1 ) {
    		throw new IllegalStateException("No token ID is assigned to this client");
    	}
    	
    	return tokenID;
    }
    
    /**
     * Determines if this client is authenticated as a private client.
     * In other words if this client is part of the private clients list,
     * so we know his login and his ID.
     * 
     * @return True if this client is authenticated
     */
    private boolean isPrivateAuthenticated() {
    	return !Objects.isNull(login) && tokenID != -1;
    }
    
    @Override
    protected void acceptFrame(Frame frame) throws IOException {
    	frame.accept(this);
    }
    
	@Override
	public void log(Level level, String msg) {
		client.log(Objects.requireNonNull(level), Objects.requireNonNull(msg));
	}
	
	public void log(Level level, String msg, Throwable thrw) {
		client.log(Objects.requireNonNull(level), Objects.requireNonNull(msg), Objects.requireNonNull(thrw));
	}

	@Override
	public void abortPrivateCommunicationRequest(String login) {
		client.clearPendingMessages(Objects.requireNonNull(login));
		client.clearPendingFiles(Objects.requireNonNull(login));
	}

	@Override
	public void connectToPrivateServer(InetSocketAddress server, String login, int tokenID) throws IOException {
		client.connectToPrivateServer(Objects.requireNonNull(server), Objects.requireNonNull(login), tokenID);
	}

	@Override
	public void tryAuthenticate(int id, String login) {
		client.tryAuthenticate(id, Objects.requireNonNull(login), this);
	}
	
	@Override
	public void printPrivateMessage(String message) {
		if ( !isPrivateAuthenticated() ) {
			// This client is not authenticated, his message is ignored.
			return;
		}
		
		System.out.println("From " + login + " to me :");
		System.out.println(message);
	}

	@Override
	public void askForPrivateCommunication(String login) {
		client.addAskingClient(Objects.requireNonNull(login), this);
	}
	
	@Override
	public void initFileDownload(String fileName, int fileSize, int fileID) {
		if ( !isPrivateAuthenticated() ) {
			// This client is not authenticated, do nothing.
			return;
		}
		
		if ( files.containsKey(fileID) ) {
			log(Level.INFO, "The file (ID : " + fileID + ") is already downloading.");
		} else {
			files.put(fileID, new FileContext(fileSize, client.createNewFile(fileName)));
			
			System.out.println("Starting downloading file " + fileName + "...");
			log(Level.INFO, "Downloading file " + fileName + " of size " + fileSize + " and ID " + fileID + ".");
		}
	}
	
	@Override
	public void downloadFile(int fileID, byte[] data) {
		if ( !isPrivateAuthenticated() ) {
			// This client is not authenticated, do nothing.
			return;
		}
		
		if ( !files.containsKey(fileID) ) {
			log(Level.WARNING, "The downloading frame (ID : " + fileID + ") does not correspond to requested file.");
			return;
		}
		
		var file = files.get(fileID);
		
		if ( file.state() == State.REFILL ) {
			file.fillBuffer(data);
		}
		
		if ( file.state() == State.FULL ) {
			try {
				if ( !file.flush() ) {
					// File is fully downloaded.
					files.remove(fileID);
					System.out.println("Download complete.");
					log(Level.INFO, "Download of file with ID " + fileID + " complete.");
				}
			} catch (Exception e) {
				files.remove(fileID);
				log(Level.SEVERE, "Error while flushing the buffer into the file.", e);
			}
		}
	}
}
