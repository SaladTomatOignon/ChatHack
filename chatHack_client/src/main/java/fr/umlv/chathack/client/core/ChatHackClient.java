package fr.umlv.chathack.client.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.Client;
import fr.umlv.chathack.contexts.ClientContext;
import fr.umlv.chathack.resources.frames.ConnectionFrame;
import fr.umlv.chathack.resources.frames.PrivateAnswerFromCliFrame;
import fr.umlv.chathack.resources.frames.PrivateAuthCliFrame;
import fr.umlv.chathack.resources.frames.PrivateMessageFrame;
import fr.umlv.chathack.resources.frames.PrivateRequestFrame;
import fr.umlv.chathack.resources.frames.PublicMessageFromCliFrame;

public class ChatHackClient implements Client {
	static final private Logger logger = Logger.getLogger(ChatHackClient.class.getName());
	
	private final InetSocketAddress publicServer;
	private final Selector selector;
	private SelectionKey publicServerChannelKey;
	
	private ServerSocketChannel privateServerSocketChannel;
	private final Map<String, ClientContext> privateClients; // Clients to whom a connection has been established. Key:login ; Value:ClientContext
	private final Map<Integer, String> privatePendingClients; // Clients asking for connection and having a token ID but not yet connected. Key:ClientID ; Value:Login
	private final Map<String, ClientContext> privateAskingClients; // Clients asking for connection and not having a token ID. Key:Login ; Value:ClientContext
	private final Map<String, Queue<PublicMessageFromCliFrame>> privatePendingMessages; // Messages sent to a client whose communication has not yet been established. Key:login ; Value:Messages queue
	
	private final String login;
	private final String password;
    
    private final Thread mainThread;
	
	public ChatHackClient(InetSocketAddress server, String login) throws IOException {
		this(server, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, String login, String password) throws IOException {
		this.publicServer = Objects.requireNonNull(server);
		this.selector = Selector.open();
		
		this.privateClients = new HashMap<>();
		this.privatePendingClients = new HashMap<>();
		this.privateAskingClients = new HashMap<>();
		this.privatePendingMessages = new HashMap<>();
		
		this.login = Objects.requireNonNull(login);
		this.password = Objects.requireNonNull(password);
		
		this.mainThread = new Thread(this::run);
	}
	
	/**
	 * Start the client on a new thread, with reset parameters.
	 * Listen to the public server and allow another thread to send frames to it.<br>
	 * <br>
	 * Give the possibility to also communicate with another server and sending to it
	 * private messages and files.<br>
	 * <br>
	 * Finally, this client can also become a server to establish itself a communication
	 * with another client.
	 * 
	 * @throws IOException if connection to any server failed or the creation of the server failed.
	 */
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
	 * Stop the client thread.<br>
	 * It does not reset client parameters (buffers, queue and frame reader)
	 * 
	 */
	public void stop() {
		mainThread.interrupt();
	}
	
	/**
	 * Reset client parameters and initiates connection with server.
	 * 
	 * @throws IOException
	 */
	private void init() throws IOException {
		/* Reset parameters */
		privateServerSocketChannel = null;
		privateClients.clear();
		privatePendingClients.clear();
		privateAskingClients.clear();
		privatePendingMessages.clear();
		
		/* Initialization of public server connection */
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(publicServer));
		
		publicServerChannelKey = sc.register(selector, SelectionKey.OP_CONNECT);
		publicServerChannelKey.attach(new ClientContext(publicServerChannelKey, this));
	}
	
	/**
	 * Start treating keys in the selector while the associated thread is uninterrupted.
	 */
	private void run() {
		logger.log(Level.INFO, "Starting communication");
		while ( !Thread.interrupted() ) {
			try {
				selector.select(this::treatKey, 100);
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
	}
	
	/**
	 * Perform an action according to the available state of the given SelectionKey.
	 * 
	 * @param key The key ready for an I/O action.
	 * 
     * @throws UncheckedIOException if connection or acceptance caused an IOException.
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
				((ClientContext) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ClientContext) key.attachment()).doRead();
			}
		} catch (IOException e) {
			silentlyClose(key);
		}
	}
    
    /**
     * Finishes the process of connection with server if possible.<br>
     * Then, if the server corresponds to the public server,
     * send the connection request with login / password.<br>
     * Else (it corresponds to a private server) send the private string message
     * that the client wanted to send.
     * 
     * @param key The key corresponding to the server to which we want to connect.
     * 
     * @throws IOException
     */
    private void doConnect(SelectionKey key) throws IOException {
    	SocketChannel sc = (SocketChannel) key.channel();
    	ClientContext ctx = (ClientContext) key.attachment();
    	
    	if ( !sc.finishConnect() ) {
    		return;
    	}
    	
    	logger.log(Level.INFO, "Connection with " + sc.getRemoteAddress() + " OK");
    	
    	if ( key.equals(publicServerChannelKey) ) {											// Public server connection
    		/* Sending authentication request for public server */
    		ctx.queueMessage(new ConnectionFrame(login, password, !password.isEmpty()));
    	} else {																			// Private server connection
    		/* Sending authentication request for private server */
    		ctx.queueMessage(new PrivateAuthCliFrame(ctx.getLogin(), ctx.getTokenId()));
    		
    		/* Sending messages that were pending */
    		while ( !privatePendingMessages.get(ctx.getLogin()).isEmpty() ) {
    			ctx.queueMessage(privatePendingMessages.get(ctx.getLogin()).remove());
    		}
    		privatePendingMessages.remove(ctx.getLogin());
    		
    		/* Adding the private client to the list */
    		addPrivateClient(ctx.getLogin(), ctx);
    	}
	}
    
    /**
     * Accept the client to the private server, and assign him its clientContext.
     * 
     * @param key The key associated to the private server
     * @throws IOException
     */
    private void doAccept(SelectionKey key) throws IOException {
        SocketChannel sc = privateServerSocketChannel.accept();
        
        if ( Objects.isNull(sc) ) {
        	return;
        }
        
        sc.configureBlocking(false);
        SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientContext(clientKey, this));
    }
	
    /**
     * Close the connection with the server.<br>
     * If the connection corresponded to a private server, the corresponding client
     * is removed from the clients list.
     * <br>
     * It does not throw exception if an I/O error occurs.
     */
    private void silentlyClose(SelectionKey key) {
    	SocketChannel sc = (SocketChannel) key.channel();
        ClientContext ctx = (ClientContext) key.attachment();
        
        if ( !key.equals(publicServerChannelKey) ) {
        	privateClients.remove(ctx.getLogin());
        }
        
        try {
        	logger.log(Level.INFO, "Connection closed with " + sc.getRemoteAddress());
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with server", e);
        }
    }
    
    /**
     * Add client informations to the list of private clients.
     * 
     * @param login The client login.
     * @param ctx The Context associated to the client.
     */
    private void addPrivateClient(String login, ClientContext ctx) {
    	if ( !Objects.isNull(privateClients.put(login, ctx)) ) {
    		logger.log(Level.WARNING, login + " was already registered in private client's list");
    	}
    	
    	logger.log(Level.INFO, "Communication established with " + login);
    }
    
    @Override
    public void tryAuthenticate(int id, String login, ClientContext ctx) {
    	if ( privatePendingClients.containsKey(id) && privatePendingClients.get(id).equals(login) ) {
    		addPrivateClient(privatePendingClients.get(id), ctx);
    		privatePendingClients.remove(id);
    		
    		ctx.setLogin(login);
    		ctx.setTokenId(id);
    	} else {
    		logger.log(Level.WARNING, "Someone tried with failure to authenticate with login " + login);
    	}
    }
    
    /**
     * Creates the private server and register it to the selector.
     * 
     * @throws IOException
     */
    private void createPrivateServer() {
    	if ( !Objects.isNull(privateServerSocketChannel) ) {
    		throw new IllegalStateException("Private server already instanciated");
    	}
    	
    	try {
        	privateServerSocketChannel = ServerSocketChannel.open();
        	privateServerSocketChannel.bind(null);
        	privateServerSocketChannel.configureBlocking(false);
        	privateServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        	logger.log(Level.INFO, "Private server started on port " + privateServerSocketChannel.socket().getLocalPort()); // TODO Vérifier que le numéro de port est correct.
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Error while creating private server", e);
    		privateServerSocketChannel = null;
    	}
    }
    
    /**
     * Determines if the given token ID is already in use.
     * 
     * @param tokenID The token ID
     * @return True if the given token ID is already in use.
     */
    private boolean isTokenIdExisting(int tokenID) {
    	return privatePendingClients.containsKey(tokenID) ||
    		   privateClients.values().stream().anyMatch(ctx -> (ctx.getTokenId() == tokenID));
    }
    
    /**
     * Assign the given login with an unique ID, then return the port
     * of the private server to which the client can connect.<br>
     * If the private server does not exist, this method creates it.
     * 
     * @param login The login to assign.
     * 
     * @return The frame containing the ID assigned and the port of the private server.
     */
    private PrivateAnswerFromCliFrame assignAndGetNewPrivateId(String login) {
    	if ( Objects.isNull(privateServerSocketChannel) ) {
    		createPrivateServer();
    	}
    	
    	int id;
    	do {
    		id = Math.toIntExact(System.currentTimeMillis() % Integer.MAX_VALUE); // Assigning a random identifier.
    	} while (isTokenIdExisting(id));
    	
    	privatePendingClients.put(id, login);
    	
    	return new PrivateAnswerFromCliFrame(login, privateServerSocketChannel.socket().getLocalPort(), id); // TODO Vérifier que le numéro de port est correct.
    }

    /**
     * Add a string message to the public server's queue.
     * 
     * @param msg The message to send.
     */
    public void sendPublicMessage(String msg) {
    	ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    	
    	ctx.queueMessage(new PublicMessageFromCliFrame(msg));
    }
    
    /**
     * Add a string message to the private client's queue.<br>
     * If the login does not correspond to an existing private client,
     * send a request to the server to create a new communication with this potential client.<br>
     * <br>
     * Sent messages while the communication has not been established are pending, and sent
     * after the connection is established.
     * 
     * @param msg The message to send.
     * @param login The login of the recipient client.
     */
    public void sendPrivateMessage(String msg, String login) {
    	if ( privateClients.containsKey(login) ) {									// If communication has already been established
    		ClientContext ctx = privateClients.get(login);
    		
    		ctx.queueMessage(new PrivateMessageFrame(msg));
    	} else if ( privatePendingMessages.containsKey(login) ) {					// If request for private communication has been sent but not yet established
    		privatePendingMessages.get(login).add(new PublicMessageFromCliFrame(msg));
    	} else {																	// If request for private communication establishment has not been sent
    		ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    		
    		/* There are 2 cases :
    		 * - This client is requesting to establish a private communication.
    		 * - This client is responding to a private communication request.
    		 */
    		
    		// If this client is responding to a private communication request.
    		if ( privateAskingClients.containsKey(login) ) {
    			if ( msg.toUpperCase().equals("NO") ) {	// Refusing the private communication.
    				ctx.queueMessage(new PrivateAnswerFromCliFrame(login));
    			} else {								// Accepting the private communication.
    				ctx.queueMessage(assignAndGetNewPrivateId(login));
    			}
    			privateAskingClients.remove(login);
    		}
    		
    		// If this client is requesting to establish a private communication.
    		else {
        		/* Ask for private communication establishment */
        		ctx.queueMessage(new PrivateRequestFrame(login));
        		
        		/* Queuing messages */
        		privatePendingMessages.put(login, new LinkedList<PublicMessageFromCliFrame>());
        		privatePendingMessages.get(login).add(new PublicMessageFromCliFrame(msg));	
    		}
    	}
    }
    
    @Override
    public void connectToPrivateServer(InetSocketAddress server, String login, int tokenID) throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(server));
		
		var privateServerKey = sc.register(selector, SelectionKey.OP_CONNECT);
		var ctx = new ClientContext(privateServerKey, this);
		ctx.setLogin(login);
		ctx.setTokenId(tokenID);
		privateServerKey.attach(ctx);
    }
    
    @Override
    public void clearPendingMessages(String login) {
    	privatePendingMessages.remove(login);
    }
    
    @Override
    public void addAskingClient(String login, ClientContext ctx) {
		System.out.println(login + " wants to establish a communication with you.");
		System.out.println("Reply with '/" + login + " no' if you want to refuse. Other replies will have the effect of accepting the communication.");
    	
    	privateAskingClients.put(login, ctx);
    }
}
