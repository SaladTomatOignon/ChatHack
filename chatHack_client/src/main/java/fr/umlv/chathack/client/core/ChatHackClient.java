package fr.umlv.chathack.client.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.client.frames.Frame;
import fr.umlv.chathack.client.readers.FrameReader;

public class ChatHackClient {
	static private Logger logger = Logger.getLogger(ChatHackClient.class.getName());
	static final int BUFFER_SIZE = 1024;
	
	private final SocketChannel sc;
	private final Selector selector;
	private final String login;
	private final String password;
	
    private final ByteBuffer bbin;
    private final ByteBuffer bbout;
    private final FrameReader freader;
    
    private boolean closed;
	
	public ChatHackClient(InetSocketAddress server, String login) throws IOException {
		this(server, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, String login, String password) throws IOException {
		this.sc = SocketChannel.open();
		this.sc.connect(Objects.requireNonNull(server));
		this.selector = Selector.open();
		this.sc.register(selector, SelectionKey.OP_WRITE);
		
		this.login = Objects.requireNonNull(login);
		this.password = Objects.requireNonNull(password);
		
		this.bbin = ByteBuffer.allocate(BUFFER_SIZE);
		this.bbout = ByteBuffer.allocate(BUFFER_SIZE);
		this.freader = new FrameReader(bbin);
		
		this.closed = false;
	}
	
	public void launch() throws IOException {
		while ( !Thread.interrupted() ) {
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}
	
	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isWritable()) {
				doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection lost with server due to IOException", e);
			silentlyClose(key);
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
    private void doRead() throws IOException {
        if ( sc.read(bbin) == -1 ) {
            closed = true;
        }
        
        processIn();
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
    }
	
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with server", e);
        }
    }
}
