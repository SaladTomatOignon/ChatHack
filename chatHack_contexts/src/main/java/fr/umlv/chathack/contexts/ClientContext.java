package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.FileContext.State;
import fr.umlv.chathack.resources.frames.ClientVisitor;
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.frames.InfoFrame;
import fr.umlv.chathack.resources.readers.FrameReader;

public class ClientContext implements ClientVisitor {
	static final private Logger logger = Logger.getLogger(ClientContext.class.getName());
	
    final private SelectionKey key;
    final private SocketChannel sc;
    final private Client client;
    
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private Queue<Frame> queue;
    final private FrameReader freader;
    
    final private Map<Integer, FileContext> files;
    
    private boolean closed;
    private String login; // The client login, may be null.
    private int tokenID; // The ID used to communicate the client, -1 if not assigned.
	
    public ClientContext(SelectionKey key, Client client) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.client = client;
        
        this.bbin = ByteBuffer.allocate(Client.BUFFER_SIZE);
        this.bbout = ByteBuffer.allocate(Client.BUFFER_SIZE);
        this.queue = new LinkedList<>();
        this.freader = new FrameReader(bbin);
        
        this.files = new HashMap<>();
        
        this.closed = false;
        this.login = null;
        this.tokenID = -1;
    }
    
    /**
     * Update the interestOps of the key looking
     * only at values of the boolean closed and
     * of both ByteBuffers.<br>
     * <br>
     * The convention is that both buffers are in write-mode before the call
     * to updateInterestOps and after the call.<br>
     * Also it is assumed that process has been be called just
     * before updateInterestOps.
     */
    private void updateInterestOps() {
        int newInterestOps = 0;
        
        if ( bbin.hasRemaining() && !closed ) {
        	newInterestOps |= SelectionKey.OP_READ;
        }
        
        if ( (bbout.position() > 0 || !queue.isEmpty()) && !closed ) {
        	newInterestOps |= SelectionKey.OP_WRITE;
        }
            
        if ( newInterestOps == 0 ) {
            silentlyClose();
        } else {
            key.interestOps(newInterestOps);
        }
    }
    
    /**
     * Close the connection with the socketChannel.<br>
     * It does not throw exception if an I/O error occurs.
     */
    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
        	log(Level.SEVERE, "Error while closing connection", e);
        }
    }
    
    /**
     * Process the content of bbin.<br>
     *<br>
     * The convention is that bbin is in write-mode before the call
     * to process and after the call.
     * 
     * @throws IOException 
     *
     */
    private void processIn() throws IOException {
    	switch ( freader.process() ) {
    		case DONE :
    			Frame frame = (Frame) freader.get();
    			freader.reset();
    			log(Level.INFO, "Frame received : " + frame);
    			
    			frame.accept(this);
    			
    			if ( bbin.position() > 0 ) {
    				processIn(); // We keep processing until there are bytes remaining in bbin.
    			}
    			break;
    		case REFILL :
    			break;
    		case ERROR :
    			queueMessage(new InfoFrame((byte) 1, "Invalid frame received, it has been ignored."));
    			log(Level.WARNING, "Error while reading a frame ! Ignoring the frame.");
    			freader.reset();
    			break;
    	}
    }
    
    /**
     * Performs the read action on sc.<br>
     * <br>
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call.
     *
     * @throws IOException
     */
    public void doRead() throws IOException {
        if ( sc.read(bbin) == -1 ) {
            closed = true;
        }
        
        processIn();
        updateInterestOps();
    }
    
    /**
     * Try to fill bbout from the message queue.
     *
     */
    private void processOut() {
    	synchronized ( key ) {
            while ( !queue.isEmpty() && bbout.remaining() >= queue.element().size() ) {
                bbout.put(queue.remove().getBytes());
            }
    	}
    }
    
    /**
     * Performs the write action on sc.<br>
     * <br>
     * The convention is that both buffers are in write-mode before the call
     * to doWrite and after the call.
     *
     * @throws IOException
     */
    public void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        
        processOut();
        updateInterestOps();
    }
    
    /**
     * Add a message to the message queue, tries to fill bbOut and updateInterestOps.
     *
     * @param frame The frame to add
     */
    public void queueMessage(Frame frame) {
    	synchronized ( key ) {
    		queue.add(frame);
    	}
    	
    	log(Level.INFO, "Sending frame : " + frame);
        
        processOut();
        updateInterestOps();
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
	public void log(Level level, String msg) {
		client.log(Objects.requireNonNull(level), Objects.requireNonNull(msg));
	}
	
	public void log(Level level, String msg, Throwable thrw) {
		client.log(Objects.requireNonNull(level), Objects.requireNonNull(msg), Objects.requireNonNull(thrw));
	}

	@Override
	public void abortPrivateCommunicationRequest(String login) {
		client.clearPendingMessages(Objects.requireNonNull(login));
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
		if ( isPrivateAuthenticated() ) {
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
		if ( isPrivateAuthenticated() ) {
			// This client is not authenticated, do nothing.
			return;
		}
		
		if ( files.containsKey(fileID) ) {
			// TODO Fichier déjà en cours de téléchargement
			logger.info("File already downloading");
			return ;
		} else {
			files.put(fileID, new FileContext(fileSize, client.createNewFile(fileName)));
		}
	}
	
	@Override
	public void downloadFile(int fileID, byte[] data) {
		if ( isPrivateAuthenticated() ) {
			// This client is not authenticated, do nothing.
			return;
		}
		
		var file = files.get(fileID);
		
		if ( file.state() == State.REFILL ) {
			file.fillBuffer(data);
		}
		
		if ( file.state() == State.FULL ) {
			if ( !file.flush() ) {
				// File is fully downloaded.
				files.remove(fileID);
			}
		}
	}
}
