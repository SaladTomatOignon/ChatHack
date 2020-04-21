package fr.umlv.chathack.server.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.Server;
import fr.umlv.chathack.contexts.ServerContext;
import fr.umlv.chathack.resources.frames.AuthBddFrame;
import fr.umlv.chathack.resources.frames.ConnectionAnswerFrame;
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.frames.RequestLoginExistFrame;
import fr.umlv.chathack.resources.readers.BddReader;

public class ChatHackServer implements Server {
	static private final Logger logger = Logger.getLogger(ChatHackServer.class.getName());
	static private final int DB_PORT = 7777;
	
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private SelectionKey dbServerKey;
    
    private final Map<Long, ServerContext> pendingClients; // Clients asking for authentication but not yet authenticated. Key:requestID ; Value:ClientContext
    private final Map<String, ServerContext> authenticatedClients;
	
	public ChatHackServer(int port) throws IOException {
		logger.addHandler(new FileHandler("server_log.log"));
		logger.setUseParentHandlers(false);
		
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        
        this.pendingClients = new HashMap<>();
        this.authenticatedClients = new HashMap<>();
	}
	
    public void launch() throws IOException {
    	logger.log(Level.INFO, "Server started on port " + serverSocketChannel.socket().getLocalPort());
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		connectToDatabase();
		
		while ( !Thread.interrupted() ) {
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
    }
    
    /**
     * Initiates the connection to the database server in non blocking mode,
     * opened in local on port 'DB_PORT'.
     * 
     * @throws IOException If connection failed.
     */
    private void connectToDatabase() throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(new InetSocketAddress("localhost", DB_PORT));
		
		dbServerKey = sc.register(selector, SelectionKey.OP_CONNECT);
		dbServerKey.attach(new ServerContext(dbServerKey, this, BddReader.class));
    }
    
	/**
	 * Perform an action according to the available state of the given SelectionKey.
	 * 
	 * @param key The key ready for an I/O action.
	 * 
     * @throws UncheckedIOException if acceptance caused an IOException.
	 */
	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				doConnect(key);
			}
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
		
		try {
			if (key.isValid() && key.isWritable()) {
				((ServerContext) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ServerContext) key.attachment()).doRead();
			}
		} catch (IOException e) {
			silentlyClose(key);
		}
	}
	
	/**
     * Finishes the process of connection with database server if possible.<br>
     * 
	 * @param key The selection key corresponding to the server database.
	 * 
	 * @throws IOException 
	 */
    private void doConnect(SelectionKey key) throws IOException {
    	SocketChannel sc = (SocketChannel) key.channel();
    	
    	if ( !sc.finishConnect() ) {
    		return;
    	}
    	
    	log(Level.INFO, "Connection with database server established");
	}

	/**
     * Accept the client to the public server, and assign him its Context.
     * 
     * @param key The key associated to the public server
     * 
     * @throws IOException
     */
    private void doAccept(SelectionKey key) throws IOException {
        SocketChannel sc = serverSocketChannel.accept();
        
        if ( Objects.isNull(sc) ) {
        	return;
        }
        
        sc.configureBlocking(false);
        SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ServerContext(clientKey, this));
    }
	
    /**
     * Close the connection with the socketChannel.
     * It does not throw exception if an I/O error occurs.
     * 
     * Remove the client associated to the key from the authenticated list.
     * 
     */
    private void silentlyClose(SelectionKey key) {
    	SocketChannel sc = (SocketChannel) key.channel();
        ServerContext ctx = (ServerContext) key.attachment();
        
        authenticatedClients.remove(ctx.getLogin());
        
        try {
        	logger.log(Level.INFO, "Connection closed with " + sc.getRemoteAddress());
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with client", e);
        }
    }
    
    /**
     * Add the client to the authenticated list.
     * 
     * @param login The login's client.
     * @param ctx The client's context.
     */
    private void authenticateClient(String login, ServerContext ctx) {
    	logger.log(Level.INFO, "Authenticating " + login + " to the server.");
    	
    	authenticatedClients.put(login, ctx);
    }
    
	@Override
	public void broadcast(Frame frame) {
		logger.log(Level.INFO, "Broadcasting a frame : " + frame);
		
		for (var client : authenticatedClients.keySet()) {
			var ctx = authenticatedClients.get(client);
			
			ctx.queueMessage(frame);
		}
	}

	@Override
	public void sendFrame(Frame frame, String dest) throws IllegalArgumentException {
		if ( !authenticatedClients.containsKey(dest) ) {
			throw new IllegalArgumentException(dest + " is not authenticated to the server");
		}
		
		var ctx = authenticatedClients.get(dest);
		
		ctx.queueMessage(frame);
	}
	
	@Override
	public void sendAuthRequest(String login, String password, ServerContext ctx) {
		ServerContext dbCtx = (ServerContext) dbServerKey.attachment();
		
		long id = System.currentTimeMillis(); // Generating a unique random identifier.
		
		pendingClients.put(id, ctx);
		dbCtx.queueMessage(new AuthBddFrame(id, login, password));
	}

	@Override
	public void sendAuthRequest(String login, ServerContext ctx) {
		ServerContext dbCtx = (ServerContext) dbServerKey.attachment();
		
		long id = System.currentTimeMillis(); // Generating a unique random identifier.
		
		pendingClients.put(id, ctx);
		dbCtx.queueMessage(new RequestLoginExistFrame(id, login));
	}
	
	@Override
	public void tryAuthenticate(long id, boolean positiveAnswer) {
		if ( !pendingClients.containsKey(id) ) {
			return;
		}
		
		ServerContext ctx = pendingClients.get(id);
		pendingClients.remove(id);
		
		if ( positiveAnswer ) {
			if ( ctx.isGuest() ) {
				// It means that the login with which the client wanted to identify already exists in the database, so we refuse the authentication.
				ctx.queueMessage(new ConnectionAnswerFrame((byte) 2));
				return;
			}
		} else {
			if ( !ctx.isGuest() ) {
				// It means that the pair login/password was invalid.
				ctx.queueMessage(new ConnectionAnswerFrame((byte) 1));
				return;
			}
		}
		
		ctx.confirmAuthentication();
		authenticateClient(ctx.getLogin(), ctx);
		ctx.queueMessage(new ConnectionAnswerFrame((byte) 0));
	}

	@Override
	public void log(Level level, String msg) {
		logger.log(level, msg);
	}

	@Override
	public void log(Level level, String msg, Throwable thrw) {
		logger.log(level, msg, thrw);
	}

}
