package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

import fr.umlv.chathack.resources.frames.DlFileFrame;
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.frames.InfoFrame;
import fr.umlv.chathack.resources.readers.FrameReader;
import fr.umlv.chathack.resources.readers.Reader;

public abstract class Context {
    final private SelectionKey key;
    final private SocketChannel sc;
	
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private Queue<Frame> queue;
    final private Reader freader;
    
    private boolean closed;
    
    public Context(SelectionKey key) {
    	this(key, FrameReader.class);
    }
    
    public <T extends Reader> Context(SelectionKey key, Class<T> reader) {
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        
        this.bbin = ByteBuffer.allocate(Server.BUFFER_SIZE);
        this.bbout = ByteBuffer.allocate(Server.BUFFER_SIZE);
        this.queue = new PriorityBlockingQueue<Frame>(512, frameComparator());
        
        Reader tmpReader = null;
        try {
        	tmpReader = reader.getConstructor(ByteBuffer.class).newInstance(bbin);
		} catch (Exception e) {
			log(Level.SEVERE, "Error while instanciating the frame reader", e);
			throw new IllegalArgumentException("The provided Reader class could not be loaded");
		}
        this.freader = tmpReader;
        
        this.closed = false;
    }
    
    /**
     * Makes and returns a frame comparator which compare 2 types of frame :</br>
     * Download Frame and not Download Frame.</br>
     * This comparator make the Download Frames less important than others frames,
     * in order to send faster messages.</br>
     * To conserve order in 2 Download Frames, the one which has been created first
     * will be sent first.
     * 
     * @return A frame comparator, making Download Frames less important.
     */
    private Comparator<Frame> frameComparator() {
    	return new Comparator<Frame>() {

			@Override
			public int compare(Frame f1, Frame f2) {
				if ( (f1 instanceof DlFileFrame) && !(f2 instanceof DlFileFrame) ) {
					return -1;
				} else if ( !(f1 instanceof DlFileFrame) && (f2 instanceof DlFileFrame) ) {
					return 1;
				}
				
				return (int) (f1.getCreationTime() - f2.getCreationTime());
			}
    		
    	};
    }
    
    /**
     * Updates the interestOps of the key looking
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
     * Closes the connection with the socketChannel.<br>
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
     * Processes the content of bbin.<br>
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
    			
    			acceptFrame(frame);
    			
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
    protected void processOut() {
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
    	synchronized ( key ) {
            bbout.flip();
            sc.write(bbout);
            bbout.compact();
    	}
        
        processOut();
        updateInterestOps();
    }
    
    /**
     * Adds a message to the message queue, tries to fill bbOut and updateInterestOps.
     *
     * @param frame The frame to add
     */
    public void queueMessage(Frame frame) {
    	queue.add(frame);
    	
    	log(Level.INFO, "Sending frame : " + frame);
        
        processOut();
        updateInterestOps();
    }
    
    protected InetSocketAddress getRemoteAddress() throws IOException {
    	return (InetSocketAddress) sc.getRemoteAddress();
    }
    
    /**
     * Executes the code linked to the given frame.
     * 
     * @param frame The frame to which execute the code.
     * 
     * @throws IOException
     */
    protected abstract void acceptFrame(Frame frame) throws IOException;
    
	/**
	 * Logs a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 */
    public abstract void log(Level level, String msg);
    
	/**
	 * Logs a message.
	 * 
	 * @param level One of the message level identifiers, e.g., SEVERE.
	 * @param msg The string message (or a key in the message catalog).
	 * @param thrw Throwable associated with log message.
	 */
    public abstract void log(Level level, String msg, Throwable thrw);
    
    /**
     * Retrieves the size of the sending frame's queue.
     * 
     * @return The number of elements in the queue.
     */
	public int getQueueSize() {
		synchronized (key) {
			return queue.size();
		}
	}
}
