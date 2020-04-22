package fr.umlv.chathack.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.umlv.chathack.contexts.Client;
import fr.umlv.chathack.contexts.ClientContext;
import fr.umlv.chathack.resources.frames.ConnectionFrame;
import fr.umlv.chathack.resources.frames.DlFileFrame;
import fr.umlv.chathack.resources.frames.InitSendFileFrame;
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
	private final Map<String, Queue<PrivateMessageFrame>> privatePendingMessages; // Messages sent to a client whose communication has not yet been established. Key:login ; Value:Messages queue
	
	private final String login;
	private final String password;
	private final Path filesRepertory;
	private int fileId = 0;
    
    private final Thread mainThread;
	
	public ChatHackClient(InetSocketAddress server, Path filesRepertory, String login) throws IOException {
		this(server, filesRepertory, login, "");
	}
	
	public ChatHackClient(InetSocketAddress server, Path filesRepertory, String login, String password) throws IOException {
		logger.addHandler(new FileHandler("client_log.log"));
		logger.setUseParentHandlers(false);
		
		this.publicServer = Objects.requireNonNull(server);
		this.selector = Selector.open();
		
		this.privateClients = new HashMap<>();
		this.privatePendingClients = new HashMap<>();
		this.privateAskingClients = new HashMap<>();
		this.privatePendingMessages = new HashMap<>();
		
		this.filesRepertory = Objects.requireNonNull(filesRepertory);
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
    	
    	logger.log(Level.INFO, "Connection with " + sc.getRemoteAddress() + " (" +
    			(key.equals(publicServerChannelKey) ? "public server" : ctx.getLogin()) + ") established.");
    	
    	if ( key.equals(publicServerChannelKey) ) {											// Public server connection
    		/* Sending authentication request for public server */
    		ctx.queueMessage(new ConnectionFrame(login, password, !password.isEmpty()));
    	} else {																			// Private server connection
    		/* Sending authentication request for private server */
    		ctx.queueMessage(new PrivateAuthCliFrame(this.login, ctx.getTokenId()));
    		
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
    	
    	logger.log(Level.INFO, "Private communication possible with " + login);
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
    		logger.log(Level.INFO, "Creating private server...");
        	privateServerSocketChannel = ServerSocketChannel.open();
        	privateServerSocketChannel.bind(null);
        	privateServerSocketChannel.configureBlocking(false);
        	privateServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        	logger.log(Level.INFO, "Private server started on port " + privateServerSocketChannel.socket().getLocalPort());
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
    		id = Math.toIntExact(System.currentTimeMillis() % Integer.MAX_VALUE); // Assigning a unique random identifier.
    	} while (isTokenIdExisting(id));
    	
    	privatePendingClients.put(id, login);
    	
    	return new PrivateAnswerFromCliFrame(login, privateServerSocketChannel.socket().getLocalPort(), id);
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
    	if ( login.equals(this.login) ) {
    		System.out.println("You can't establish a private communication with yourself");
    		return;
    	}
    	
    	if ( privateClients.containsKey(login) ) {									// If communication has already been established
    		ClientContext ctx = privateClients.get(login);
    		
    		ctx.queueMessage(new PrivateMessageFrame(msg));
    	} else if ( privatePendingMessages.containsKey(login) ) {					// If request for private communication has been sent but not yet established
    		privatePendingMessages.get(login).add(new PrivateMessageFrame(msg));
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
        		privatePendingMessages.put(login, new LinkedList<PrivateMessageFrame>());
        		privatePendingMessages.get(login).add(new PrivateMessageFrame(msg));
    		}
    	}
    }
    
    /**
     * Send a file to the recipient client, chunk by chunk.
     * 
     * @param fileName The name of the file in the files directory.
     * @param login The login of the recipient client.
     * @throws FileNotFoundException  it should never happened
     */
    public void sendFile(String fileName, String login) {
    	/**
    	 * TODO : Faire l'envoi de fichier. Eventuellement lancer un nouveau thread qui le fait, pour ne pas bloquer le reste.
    	 * Les infos :
    	 * context du client à qui envoyer le fichier : ClientContext ctx = privateClients.get(login);
    	 * Envoi de la frame au client : ctx.queueMessage(frame).
    	 * 
    	 * D'abord vérifier si le login fait partis des clients connectés : privateClients.contains(login)
    	 * Aussi vérifier si le fichier existe bien dans le repertoire des fichiers 'filesRepertory'.
    	 */
    	if (!privateClients.containsKey(login)) {
    		logger.info("Connection must be open to send file");
    		return ;
    	}
    	
    	var path = filesRepertory + "/" + fileName;
    	var f = new File(path);
    	if (!f.exists()) {
    		logger.info("File : " + path + " must exist to be send.");
    		return ;
    	}
    	if (f.length() > Integer.MAX_VALUE) {
    		logger.info("File too big to be send");
    		return ;
    	}
    	var currentId = fileId;
    	fileId++;
    	var ctx = privateClients.get(login);
    	ctx.queueMessage(new InitSendFileFrame(fileName, (int) f.length(), currentId)); 
    	new Thread(() ->{
    		try (FileInputStream ios = new FileInputStream(filesRepertory + "/" + fileName)) {
    			byte[] buffer = new byte[1024];
        		var read = 0;
        		var cpt = 0;
        		try {
    				while ((read = ios.read(buffer)) != -1) {
    					ctx.queueMessage(new DlFileFrame(currentId, read, buffer)); 
    					cpt++;
    					if (cpt%20 == 0) {
    						try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								return ;
							}
    					}
    				}
    			} catch (IOException e) {
    				logger.info("problem while sending file");
    	    		return ;
    			}
    		} catch (FileNotFoundException e1) {
    			System.err.println("File not fount"); //this should never happened 
    			return ;
    		} catch (IOException e2) {
				System.err.println("problem while closing FileInputStream");
			}
    		
    	}).start();
    }
    
	@Override
	public void log(Level level, String msg) {
		logger.log(level, msg);
	}

	@Override
	public void log(Level level, String msg, Throwable thrw) {
		logger.log(level, msg, thrw);
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
		System.out.println("Reply with '/" + login + " no' if you want to refuse." +
						   "Other replies to " + login + " will have the effect of accepting the communication.");
    	
    	privateAskingClients.put(login, ctx);
    }
    
    @Override
    public FileOutputStream createNewFile(String fileName) {
    	/* TODO Créer un fichier unique (Faire attention aux noms dupliqués)
    	 * dans le répertoire 'filesRepertory'.
    	 */
    	File dir = new File(filesRepertory.toString());
		var pathNumber = 1;
		if (!dir.exists())
			dir.mkdirs();
		var path = filesRepertory + "/" +  fileName;
		File f = new File(path);
		try {
			// Create a new file like fileName (i) if already exist
			while(!f.createNewFile()) {
				var splited = path.split("\\.");
				if (splited.length >= 2) {
					splited[splited.length - 1 ] = " (" + pathNumber + ")." + splited[splited.length - 1 ];
				}else {
					splited[0] = splited[0] +  " (" + pathNumber +")";
				}
				var newPath = String.join("", splited);
				f = new File(newPath);
				pathNumber++;
			}
		} catch (IOException e) {
			System.err.println("File can't be created");
		}
    	try {
			return new FileOutputStream(f.getPath(), true); // Create the file in append mode
		} catch (FileNotFoundException e) {
			System.err.println("Probleme while creating FileOutputStream");
			return null; //Should never happened 
		}
    	
    }
}
