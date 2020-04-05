package fr.umlv.chathack.client.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
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

import fr.umlv.chathack.contexts.ClientContext;
import fr.umlv.chathack.resources.frames.ConnectionFrame;
import fr.umlv.chathack.resources.frames.PrivateRequestFrame;
import fr.umlv.chathack.resources.frames.SendPublicMessageFrame;

public class ChatHackClient {
	static final private Logger logger = Logger.getLogger(ChatHackClient.class.getName());
	static final private int PRIVATE_SERVER_PORT = 7777;
	
	private final InetSocketAddress publicServer;
	private final Selector selector;
	private SelectionKey publicServerChannelKey;
	
	private ServerSocketChannel privateServerSocketChannel;
	private final Map<String, ClientContext> privateClients; // Clients to whom a connection has been established. Key:login ; Value:ClientContext
	private final Map<Integer, String> privatePendingClients; // Clients asking for connection but not yet connected. Key:ClientID ; Value:Login
	private final Map<String, Queue<SendPublicMessageFrame>> privatePendingMessages; // Messages sent to a client whose communication has not yet been established. Key:login ; Value:Messages queue
	
	private final String login;
	private final String password;
    
    private final Thread mainThread;
	
	public ChatHackClient(InetSocketAddress server, String login) throws IOException {
		this(server, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, String login, String password) throws IOException {
		this.publicServer = Objects.requireNonNull(server);
		this.selector = Selector.open();
		this.publicServerChannelKey = null;
		
		this.privateServerSocketChannel = null;
		this.privateClients = new HashMap<>();
		this.privatePendingClients = new HashMap<>();
		this.privatePendingMessages = new HashMap<>();
		
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
	 * Reset client parameters (buffers, queue and frame reader) and init connection with server
	 * 
	 * @throws IOException
	 */
	private void init() throws IOException {
		// TODO reinitialiser tous les parametres
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(publicServer));
		
		publicServerChannelKey = sc.register(selector, SelectionKey.OP_CONNECT);
		publicServerChannelKey.attach(new ClientContext(publicServerChannelKey));
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
			logger.log(Level.INFO, "Connection lost with server due to IOException", e);
			silentlyClose(key);
		}
	}
    
    /**
     * Finishes the process of connection with server if possible.
     * Then, if the server corresponds to the public server,
     * send the connection request with login / password.
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
    	
    	if ( key.equals(publicServerChannelKey) ) {											// Public server connection
    		/* Sending authentication request for public server */
    		ctx.queueMessage(new ConnectionFrame(login, password, !password.isEmpty()));
    	} else {																			// Private server connection
    		/* Sending authentication request for private server */
    		// TODO Envoyer la requete d'authentification au serveur privé
    		// ctx.queueMessage(new privateAuthRequest(ctx.getLogin(), ctx.getTokenID());
    		
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
        clientKey.attach(new ClientContext(clientKey));
    }
	
    /**
     * Close the connection with the private client and remove him from the list.
     * It does not throw exception if an I/O error occurs.
     */
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        ClientContext ctx = (ClientContext) key.attachment();
        
        privateClients.remove(ctx.getLogin());
        try {
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
    }
    
    /**
     * Authenticates the given SelectionKey to this server if the given token ID is correct.
	 * Authentication succeed if the pair ID - login exists in the pending client list.
     * Removes the id from the "pending" list after authentication.
     * 
     * @param id The token ID.
     * @param login The login.
     * @param ctx The client Context to authenticate.
     */
    private void tryAuthenticate(int id, String login, ClientContext ctx) {
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
        	privateServerSocketChannel.bind(new InetSocketAddress(PRIVATE_SERVER_PORT));
        	privateServerSocketChannel.configureBlocking(false);
        	privateServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);	
    	} catch (IOException e) {
    		logger.log(Level.SEVERE, "Error while creating private server", e);
    		privateServerSocketChannel = null;
    	}
    }
    
    /**
     * Assign the given login with an unique ID, then return the port
     * of the private server to which the client can connect.
     * If the private server does not exist, this method creates it.
     * 
     * @param login The login to assign.
     * @return The frame containing the ID assigned and the port of the private server.
     */
    private void assignAndGetNewPrivateId(String login) {
    	if ( Objects.isNull(privateServerSocketChannel) ) {
    		createPrivateServer();
    	}
    	
    	// TODO Rendre cette boucle plus jolie
    	int id;
    	do {
    		id = Math.toIntExact(System.currentTimeMillis() % Integer.MAX_VALUE);
    		int copyID = id;
    		if ( !privatePendingClients.containsKey(id) && privateClients.values().stream().noneMatch(ctx -> (ctx.getTokenId() == copyID)) ) {
    			break;
    		}
    	} while (true);
    	
    	privatePendingClients.put(id, login);
    	
    	// TODO Return la frame "Réponse à la demande de communication privée avec un autre client"
    }

    /**
     * Add a string message to the public server's queue.
     * 
     * @param msg The message to send.
     */
    public void sendPublicMessage(String msg) {
    	ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    	
    	ctx.queueMessage(new SendPublicMessageFrame(msg));
    }
    
    /**
     * Add a string message to the private client's queue.
     * If the login does not correspond to an existing private client,
     * send a request to the server to create a new communication with this potential client.
     * 
     * Sent messages while the communication has not been established are pending, and sent
     * after the connection is established.
     * 
     * @param msg The message to send.
     * @param login The login of the recipient client.
     */
    public void sendPrivateMessage(String msg, String login) {
    	if ( privateClients.containsKey(login) ) {									// If communication has already been established
    		ClientContext ctx = privateClients.get(login);
    		
//    		ctx.queueMessage(new PrivateMessageFrame(msg));
    	} else if ( privatePendingMessages.containsKey(login) ) {					// If request for private communication has been sent but not yet established
    		privatePendingMessages.get(login).add(new SendPublicMessageFrame(msg));
    	} else {																	// If request for private communication establishment has not been sent
    		ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    		
    		/* Ask for private communication establishment */
    		ctx.queueMessage(new PrivateRequestFrame(login));
    		
    		/* Queuing messages */
    		privatePendingMessages.put(login, new LinkedList<SendPublicMessageFrame>());
    		privatePendingMessages.get(login).add(new SendPublicMessageFrame(msg));
    	}
    }
    
    /**
     * Connect the client to the server corresponding to another client's private server.
     * Then register the server to selector and the server's owner to the private clients list.
     * 
     * @param server The server to connect to.
     * @param login The login of the server owner.
     * @param id The id assigned to the communication with the server.
     * @throws IOException 
     */
    private void connectToPrivateServer(InetSocketAddress server, String login, int tokenID) throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(Objects.requireNonNull(server));
		
		var privateServerKey = sc.register(selector, SelectionKey.OP_CONNECT);
		var ctx = new ClientContext(privateServerKey);
		ctx.setLogin(login);
		ctx.setTokenId(tokenID);
		privateServerKey.attach(ctx);
    }
}
