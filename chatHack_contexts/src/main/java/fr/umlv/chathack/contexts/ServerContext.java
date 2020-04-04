package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.readers.FrameReader;

public class ServerContext {
	static private final Logger logger = Logger.getLogger(ServerContext.class.getName());
	
    final private SelectionKey key;
    final private SocketChannel sc;
    final private Server server;
    
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private Queue<Frame> queue;
    final private FrameReader freader;
    
    private String login;
    private boolean closed;
	
    public ServerContext(SelectionKey key, Server server) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
        
        this.bbin = ByteBuffer.allocate(Server.BUFFER_SIZE);
        this.bbout = ByteBuffer.allocate(Server.BUFFER_SIZE);
        this.queue = new LinkedList<>();
        this.freader = new FrameReader(bbin);
        
        this.login = null;
        this.closed = false;
    }
    
    /**
     * Update the interestOps of the key looking
     * only at values of the boolean closed and
     * of both ByteBuffers.
     *
     * The convention is that both buffers are in write-mode before the call
     * to updateInterestOps and after the call.
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
     * Close the connection with the socketChannel.
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
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
     *
     */
    private void processIn() {
    	switch ( freader.process() ) {
    		case DONE :
    			Frame frame = (Frame) freader.get();
    			freader.reset();
    			
    			System.out.println("Frame recu : " + frame);
    			frame.accept();
    			break;
    		case REFILL :
    			break;
    		case ERROR :
    			// TODO : Gestion des erreurs lorsqu'un paquet re�u n'est pas conforme.
    			freader.reset();
    			break;
    	}
    }
    
    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call
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
     * Try to fill bbout from the message queue
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
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doWrite and after the call
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
     * @param frame The frame to add.
     */
    public void queueMessage(Frame frame) {
    	System.out.println("Dans queue message, on ajoute " + frame);
    	synchronized ( logger ) {
    		queue.add(frame);
    	}
        
        processOut();
        updateInterestOps();
    }
    
    /**
     * Try to login the client to the server.
     * It succeed if the logins are registered by the server.
     * 
     * @param login
     * @param password
     * @return 0 if login succeed or 1 if it failed.
     */
    public byte tryLogin(String login, String password) {
    	if ( !server.isRegistered(login, password) ) {
    		return 1;
    	}
    	
    	server.authenticateClient(login);
    	return 0;
    }
    
    /**
     * Try to login the client to the server.
     * It succeed if the given login is not already authenticated by the server.
     * 
     * @param login
     * @return 0 if login succeed or 2 if it failed.
     */
    public byte tryLogin(String login) {
    	if ( server.clientAuthenticated(login) ) {
    		return 2;
    	}
    	
    	server.authenticateClient(login);
    	return 0;
    }
    
    /**
     * Retrieve the client login.
     * 
     * @return The client login.
     */
    public String getLogin() {
    	return login;
    }
}
