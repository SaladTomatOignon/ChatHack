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
import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.server.database.DataBase;

public class ChatHackServer implements Server {
	static private Logger logger = Logger.getLogger(ChatHackServer.class.getName());
	
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    
    private final DataBase dataBase;
    private final Map<String, ServerContext> authenticatedClients;
	
	public ChatHackServer(int port) throws IOException {
		logger.addHandler(new FileHandler("server_log.log"));
		logger.setUseParentHandlers(false);
		
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        
        this.dataBase = DataBase.connect();
        this.authenticatedClients = new HashMap<>();
	}
	
    public void launch() throws IOException {
    	logger.log(Level.INFO, "Server started on port " + serverSocketChannel.socket().getLocalPort());
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		while ( !Thread.interrupted() ) {
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
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
    public boolean isRegistered(String login, String password) {
    	return dataBase.isRegistered(login, password);
    }
    
    @Override
    public void authenticateClient(String login, ServerContext ctx) {
    	logger.log(Level.INFO, "Authenticating " + login + " to the server.");
    	
    	authenticatedClients.put(login, ctx);
    }
    
    @Override
    public boolean clientAuthenticated(String login) {
    	return authenticatedClients.containsKey(login);
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
