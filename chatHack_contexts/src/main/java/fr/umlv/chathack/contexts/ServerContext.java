package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;

import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.frames.InfoFrame;
import fr.umlv.chathack.resources.frames.PublicMessageFromServFrame;
import fr.umlv.chathack.resources.frames.ServerVisitor;
import fr.umlv.chathack.resources.readers.FrameReader;

public class ServerContext implements ServerVisitor {
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
        	log(Level.SEVERE, "Error while closing connection", e);
        }
    }
    
    /**
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
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
    	synchronized ( key ) {
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
    	synchronized ( key ) {
    		queue.add(frame);
    	}
    	
    	log(Level.INFO, "Sending frame : " + frame);
        
        processOut();
        updateInterestOps();
    }
    
    @Override
    public String getLogin() {
    	return login;
    }
    
	@Override
	public void log(Level level, String msg) {
		server.log(Objects.requireNonNull(level), Objects.requireNonNull(msg));
	}
	
	public void log(Level level, String msg, Throwable thrw) {
		server.log(Objects.requireNonNull(level), Objects.requireNonNull(msg), Objects.requireNonNull(thrw));
	}
    
    @Override
    public byte tryLogin(String login, String password) {
    	if ( !server.isRegistered(login, password) ) {
    		return 1;
    	}
    	
    	server.authenticateClient(login, this);
    	this.login = login;
    	
    	return 0;
    }
    
    @Override
    public byte tryLogin(String login) {
    	if ( server.clientAuthenticated(login) ) {
    		return 2;
    	}
    	
    	server.authenticateClient(login, this);
    	this.login = login;
    	
    	return 0;
    }

	@Override
	public void broadcastMessage(String message) throws IllegalStateException {
		if ( Objects.isNull(login) ) {
			// This client is not authenticated to the server.
			throw new IllegalStateException("Client not authenticated to the server");
		}
		
		server.broadcast(new PublicMessageFromServFrame(login, message));
	}

	@Override
	public void sendFrame(Frame frame, String dest) throws IllegalStateException, IllegalArgumentException {
		if ( Objects.isNull(login) ) {
			// This client is not authenticated to the server.
			throw new IllegalStateException("Client not authenticated to the server");
		}
		
		server.sendFrame(frame, dest);
	}

	@Override
	public void sendBackFrame(Frame frame) {
		queueMessage(Objects.requireNonNull(frame));
	}

	@Override
	public InetAddress getInetAddress() throws IOException {
		return ((InetSocketAddress) sc.getRemoteAddress()).getAddress();
	}
}
