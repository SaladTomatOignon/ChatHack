package fr.umlv.chathack.client.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.ClientContext;
import fr.umlv.chathack.resources.frames.ConnectionFrame;
import fr.umlv.chathack.resources.frames.Frame;

public class ChatHackClient {
	static private Logger logger = Logger.getLogger(ChatHackClient.class.getName());
	static final int BUFFER_SIZE = 1024;
	
	private final InetSocketAddress server;
	private final Selector selector;
	private SelectionKey serverChannelKey;
	
	private final String login;
	private final String password;
    
    private final Thread mainThread;
	
	public ChatHackClient(InetSocketAddress server, String login) throws IOException {
		this(server, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, String login, String password) throws IOException {
		this.server = Objects.requireNonNull(server);
		this.selector = Selector.open();
		
		this.login = Objects.requireNonNull(login);
		this.password = Objects.requireNonNull(password);
		
		this.mainThread = new Thread(this::run);
	}
	
	public void launch() throws IOException {
		if ( mainThread.isAlive() ) {
			throw new IllegalStateException("Client is already running");
		}
		try {
			init();
			mainThread.start();
		} catch (UncheckedIOException tunneled) {
			// C'est ici qu'on pourrait �ventuellement essayer de relancer la connexion au lieu de propager l'exception
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
	 * Reset client parameters (buffers, queue and frame reader) and init connection with server
	 * 
	 * @throws IOException
	 */
	private void init() throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(server));
		
		serverChannelKey = sc.register(selector, SelectionKey.OP_CONNECT);
		serverChannelKey.attach(new ClientContext(serverChannelKey));
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
				doConnect(key);
			}
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
		
		try {
			if (key.isValid() && key.isWritable()) {
				((ClientContext) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ClientContext) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection lost with server due to IOException", e);
			silentlyClose(key);
		}
	}
    
    /**
     * Finishes the process of connection with server if possible.
     * Then send the connection request with login/password
     * @param key The channel server key
     * 
     * @throws IOException
     */
    private void doConnect(SelectionKey key) throws IOException {
    	SocketChannel sc = (SocketChannel) key.channel();
    	ClientContext ctx = (ClientContext) key.attachment();
    	
    	if ( !sc.finishConnect() ) {
    		return;
    	}
    	
    	ctx.queueMessage(new ConnectionFrame(login, password, !password.isEmpty()));
	}
	
    /**
     * Close the connection with the socketChannel.
     * It does not throw exception if an I/O error occurs.
     */
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with server", e);
        }
    }

    /**
     * Add the frame to the public server queue
     * 
     * @param frame The frame to send
     */
    public void queueMessageToPublicServer(Frame frame) {
    	ClientContext ctx = (ClientContext) serverChannelKey.attachment();
    	ctx.queueMessage(frame);
    }
}
