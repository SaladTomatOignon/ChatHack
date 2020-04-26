package fr.umlv.chathack.contexts;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.util.logging.Level;

import fr.umlv.chathack.resources.frames.Frame;
import fr.umlv.chathack.resources.frames.PublicMessageFromServFrame;
import fr.umlv.chathack.resources.frames.ServerVisitor;
import fr.umlv.chathack.resources.readers.FrameReader;
import fr.umlv.chathack.resources.readers.Reader;

public class ServerContext extends Context implements ServerVisitor {
    final private Server server;
    
    private String login;
    private String pendingLogin; // The login whose authentication has not be made.
    private boolean guest; // The client is a guest if he is not register by the database (he does not have password).
	
    public ServerContext(SelectionKey key, Server server) {
    	this(key, server, FrameReader.class);
    }
    
    public <T extends Reader> ServerContext(SelectionKey key, Server server, Class<T> reader) {
    	super(key, reader);
        this.server = server;
        
        this.login = null;
        this.pendingLogin = null;
    }
    
    /**
     * Confirms the login of the client.
     * 
     */
    public void confirmAuthentication() {
    	login = pendingLogin;
    	pendingLogin = null;
    }
    
    /**
     * Determines if this client is a guest
     * (i.e if he is not register in the database and he does not have password).
     * 
     * @return True if the client is guest.
     */
    public boolean isGuest() {
    	return guest;
    }
    
    @Override
    protected void acceptFrame(Frame frame) throws IOException {
    	frame.accept(this);
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
    public void tryLogin(String login, String password) {
    	server.sendAuthRequest(Objects.requireNonNull(login), Objects.requireNonNull(password), this);
    	pendingLogin = login;
    	guest = false;
    }
    
    @Override
    public void tryLogin(String login) {
    	server.sendAuthRequest(Objects.requireNonNull(login), this);
    	pendingLogin = login;
    	guest = true;
    }
    
    @Override
    public void answerFromDatabase(long id, boolean positiveAnswer) {
    	server.tryAuthenticate(id, positiveAnswer);
    }

	@Override
	public void broadcastMessage(String message) throws IllegalStateException {
		if ( Objects.isNull(login) ) {
			// This client is not authenticated to the server.
			log(Level.WARNING, "Client not authenticated to the server");
			return;
		}
		
		server.broadcast(new PublicMessageFromServFrame(login, message), login);
	}

	@Override
	public void sendFrame(Frame frame, String dest) throws IllegalStateException, IllegalArgumentException {
		if ( Objects.isNull(login) ) {
			// This client is not authenticated to the server.
			log(Level.WARNING, "Client not authenticated to the server");
			return;
		}
		
		server.sendFrame(frame, dest);
	}

	@Override
	public void sendBackFrame(Frame frame) {
		queueMessage(Objects.requireNonNull(frame));
	}

	@Override
	public InetAddress getInetAddress() throws IOException {
		return getRemoteAddress().getAddress();
	}
}
