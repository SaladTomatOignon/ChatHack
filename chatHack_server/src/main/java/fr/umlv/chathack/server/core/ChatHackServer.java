package fr.umlv.chathack.server.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.server.frames.Frame;

public class ChatHackServer {
	static private Logger logger = Logger.getLogger(ChatHackServer.class.getName());
	static final int BUFFER_SIZE = 1024;
	
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
	
	public ChatHackServer(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
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
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
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
        clientKey.attach(new Context(this, clientKey));
    }
	
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with client", e);
        }
    }
    
    /**
     * 
     * @param login
     * @return True if the client is connected to the server
     */
    private boolean clientConnected(String login) {
        for (SelectionKey key : selector.keys()) {
            Object attachment = key.attachment();
            
            if ( Objects.isNull(attachment) )
                continue;
            
            Context ctx = (Context) attachment;
            if ( !Objects.isNull(ctx.getLogin()) && ctx.getLogin().equals(login) ) {
            	return true;
            }
        }
        
        return false;
    }
    
    /**
     * Send a frame to all connected clients
     *
     * @param frame : The frame to send
     */
    void broadcast(Frame frame) {
        for (SelectionKey key : selector.keys()) {
            Object attachment = key.attachment();
            
            if ( Objects.isNull(attachment) )
                continue;
            
            Context ctx = (Context) attachment;
            ctx.queueMessage(frame);
        }
    }
    
    /**
     * Try to login the client to server.
     * 
     * @param login
     * @param password
     * @return 0 if connection succeed, 1 if logins are wrong, 2 if login is already in used
     */
    byte tryLogin(String login, String password) {
    	if ( clientConnected(login) ) {
    		return 2;
    	}
    	
    	return 0;
    }
    
	
}
