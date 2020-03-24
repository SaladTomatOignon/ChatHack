package fr.umlv.chathack.client.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.client.frames.ConnectionFrame;
import fr.umlv.chathack.client.frames.Frame;
import fr.umlv.chathack.client.readers.FrameReader;

public class ChatHackClient {
	static private Logger logger = Logger.getLogger(ChatHackClient.class.getName());
	static final int BUFFER_SIZE = 1024;
	
	private final InetSocketAddress server;
	private final SocketChannel sc;
	private final Selector selector;
	private SelectionKey key;
	
	private final String login;
	private final String password;
	
    private final ByteBuffer bbin;
    private final ByteBuffer bbout;
    private final Queue<Frame> queue;
    private final FrameReader freader;
    
    private final Thread mainThread;
    
    private boolean closed;
	
	public ChatHackClient(InetSocketAddress server, String login) throws IOException {
		this(server, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, String login, String password) throws IOException {
		this.server = Objects.requireNonNull(server);
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		
		this.login = Objects.requireNonNull(login);
		this.password = Objects.requireNonNull(password);
		
		this.bbin = ByteBuffer.allocate(BUFFER_SIZE);
		this.bbout = ByteBuffer.allocate(BUFFER_SIZE);
		this.queue = new LinkedList<>();
		this.freader = new FrameReader(bbin);
		
		this.mainThread = new Thread(this::run);
		this.closed = false;
	}
	
	public void launch() throws IOException {
		if ( mainThread.isAlive() ) {
			throw new IllegalStateException("Client is already running");
		}
		try {
			init();
			mainThread.start();
		} catch (UncheckedIOException tunneled) {
			// C'est ici qu'on pourrait éventuellement essayer de relancer la connexion au lieu de propager l'exception
			throw tunneled.getCause();
		}
	}
	
	/**
	 * Stop the client thread.
	 * It does not reset client parameters (buffers, queue and frame reader)
	 * 
	 */
	public void stop() {
		mainThread.interrupt();
	}
	
	/**
	 * Reset client parameters (buffers, queue and frame reader) and init connexion with server
	 * 
	 * @throws IOException
	 */
	private void init() throws IOException {
		bbin.clear();
		bbout.clear();
		queue.clear();
		freader.reset();
		
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(server));
		key = sc.register(selector, SelectionKey.OP_CONNECT);
	}
	
	private void run() {
		while ( !Thread.interrupted() ) {
			try {
				selector.select(this::treatKey, 100);
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
	}
	
	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection lost with server due to IOException", e);
			silentlyClose();
		}
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
     * Finishes the process of connexion with server if possible.
     * Then send the connexion request with login/password
     * 
     * @throws IOException
     */
    private void doConnect() throws IOException {
    	if ( !sc.finishConnect() ) {
    		return;
    	}
    	
    	queueMessage(new ConnectionFrame(login, password, !password.isEmpty()));
    	updateInterestOps();
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
    private void doRead() throws IOException {
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
    	synchronized (server) {
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

    private void doWrite() throws IOException {
        bbout.flip();
        sc.write(bbout);
        bbout.compact();
        
        processOut();
        updateInterestOps();
    }
    
    /**
     * Add a message to the message queue, tries to fill bbOut and updateInterestOps
     *
     * @param frame The frame to add
     */
    public void queueMessage(Frame frame) {
    	synchronized (server) {
    		queue.add(frame);
    	}
        
        processOut();
        updateInterestOps();
    }
	
    private void silentlyClose() {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with server", e);
        }
    }

}
