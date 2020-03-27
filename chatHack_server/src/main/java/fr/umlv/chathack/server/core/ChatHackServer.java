package fr.umlv.chathack.server.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.Server;
import fr.umlv.chathack.contexts.ServerContext;
import fr.umlv.chathack.server.database.DataBase;

public class ChatHackServer implements Server {
	static private Logger logger = Logger.getLogger(ChatHackServer.class.getName());
	static final int BUFFER_SIZE = 1024;
	
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    
    private final DataBase dataBase;
    private final Set<String> authenticatedClients;
	
	public ChatHackServer(int port) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        
        this.dataBase = DataBase.connect();
        this.authenticatedClients = new HashSet<>();
	}
	
    public void launch() throws IOException {
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
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
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
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
     */
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with client", e);
        }
    }
    
    @Override
    public boolean isRegistered(String login, String password) {
    	return dataBase.isRegistered(login, password);
    }
    
    @Override
    public void authenticateClient(String login) {
    	authenticatedClients.add(login);
    }
    
    @Override
    public boolean clientAuthenticated(String login) {
    	return authenticatedClients.contains(login);
    }
}
