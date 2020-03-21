package fr.umlv.chathack.server.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.server.frames.Frame;
import fr.umlv.chathack.server.readers.FrameReader;

public class Context {
	static private Logger logger = Logger.getLogger(Context.class.getName());
	
    final private SelectionKey key;
    final private SocketChannel sc;
    final private ChatHackServer server;
    
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    private final Queue<Frame> queue;
    final private FrameReader freader;
    
    private boolean closed;
    private boolean logged; // If the client is logged to the server
	
    public Context(ChatHackServer server, SelectionKey key) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
        
        this.bbin = ByteBuffer.allocate(ChatHackServer.BUFFER_SIZE);
        this.bbout = ByteBuffer.allocate(ChatHackServer.BUFFER_SIZE);
        this.queue = new LinkedList<>();
        this.freader = new FrameReader(bbin);
        
        this.closed = false;
        this.logged = false;
    }
    
    /**
     * Add a message to the message queue, tries to fill bbOut and updateInterestOps
     *
     * @param frame The frame to add
     */
    public void queueMessage(Frame frame) {
        queue.add(frame);
        
        processOut();
        updateInterestOps();
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
    
    private void silentlyClose() {
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with client", e);
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
    			
    			frame.accept();
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
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call
     *
     * @throws IOException
     */
    void doRead() throws IOException {
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
        while ( !queue.isEmpty() && bbout.remaining() >= queue.element().size() ) {
            bbout.put(queue.remove().getBytes());
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
    void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        
        processOut();
        updateInterestOps();
    }
}
