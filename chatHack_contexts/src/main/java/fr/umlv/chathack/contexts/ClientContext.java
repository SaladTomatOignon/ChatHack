package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.resources.frames.ClientVisitor;
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.readers.FrameReader;

public class ClientContext implements ClientVisitor {
	static private final Logger logger = Logger.getLogger(ClientContext.class.getName());
	
    final private SelectionKey key;
    final private SocketChannel sc;
    final private Client client;
    
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private Queue<Frame> queue;
    final private FrameReader freader;
    
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
        	logger.log(Level.SEVERE, "Error while closing connection", e);
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
    			
    			frame.accept(this);
    			break;
    		case REFILL :
    			break;
    		case ERROR :
    			// TODO : Gestion des erreurs lorsqu'un paquet reçu n'est pas conforme.
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
    	synchronized ( logger ) {
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
    	System.out.println("On envoie " + frame);
    	synchronized ( logger ) {
    		queue.add(frame);
    	}
        
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
    
	@Override
	public void log(Level level, String msg) {
		logger.log(Objects.requireNonNull(level), Objects.requireNonNull(msg));
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
		if ( Objects.isNull(login) || tokenID == -1 ) {
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
}
