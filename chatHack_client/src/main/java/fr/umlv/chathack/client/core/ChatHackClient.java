package fr.umlv.chathack.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
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
	private final Map<String, Queue<String>> privatePendingFiles; // Files sent to a client whose communication has not yet been established. Key:login ; Value:Files queue
	
	private final String login;
	private final String password;
	private final Path filesRepertory;
	private int fileId;
    
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
		this.privatePendingFiles = new HashMap<>();
		
		this.login = Objects.requireNonNull(login);
		this.password = Objects.requireNonNull(password);
		this.filesRepertory = Objects.requireNonNull(filesRepertory);
		this.fileId = 0;
		
		this.mainThread = new Thread(this::run);
	}
	
	/**
	 * Starts the client on a new thread, with reset parameters.
	 * Listens to the public server and allow another thread to send frames to it.<br>
	 * <br>
	 * Gives the possibility to also communicate with another server and sending to it
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
			throw tunneled.getCause();
		}
	}
	
	/**
	 * Stops the client thread.<br>
	 * It does not reset client parameters (buffers, queue and frame reader)
	 * 
	 */
	public void stop() {
		logger.log(Level.INFO, "Stopping the main thread");
		
		mainThread.interrupt();
	}
	
	/**
	 * Resets client parameters and initiates connection with server.
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
	 * Starts treating keys in the selector while the associated thread is uninterrupted.
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
	 * Performs an action according to the available state of the given SelectionKey.
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
     * sends the connection request with login / password.<br>
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
    	
    	try {
        	if ( !sc.finishConnect() ) {
        		return;
        	}	
    	} catch (ConnectException e) {
    		System.err.println("Error : Impossible to connect to the server");
    		logger.log(Level.SEVERE, "Connection refused to the server", e);
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
    		
    		/* Adding the private client to the list */
    		addPrivateClient(ctx.getLogin(), ctx);
    		
    		/* Sending messages that were pending */
    		if ( privatePendingMessages.containsKey(ctx.getLogin()) ) {
        		while ( !privatePendingMessages.get(ctx.getLogin()).isEmpty() ) {
        			ctx.queueMessage(privatePendingMessages.get(ctx.getLogin()).remove());
        		}
        		privatePendingMessages.remove(ctx.getLogin());
    		}

    		/* Sending files that were pending */
    		if ( privatePendingFiles.containsKey(ctx.getLogin()) ) {
        		while ( !privatePendingFiles.get(ctx.getLogin()).isEmpty() ) {
        			sendFile(privatePendingFiles.get(ctx.getLogin()).remove(), ctx.getLogin());
        		}
        		privatePendingFiles.remove(ctx.getLogin());
    		}
    	}
	}
    
    /**
     * Accepts the client to the private server, and assign him its clientContext.
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
     * Closes the connection with the server.<br>
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
        	System.out.println("Connection interrupted with " + ctx.getLogin());
        } else {
        	System.out.println("Connection interrupted with public server");
        }
        
        try {
        	logger.log(Level.INFO, "Connection closed with " + sc.getRemoteAddress());
            sc.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, "Error while closing connexion with server", e);
        }
    }
    
    /**
     * Adds client informations to the list of private clients.
     * 
     * @param login The client login.
     * @param ctx The Context associated to the client.
     */
    private void addPrivateClient(String login, ClientContext ctx) {
    	if ( !Objects.isNull(privateClients.put(login, ctx)) ) {
    		logger.log(Level.WARNING, login + " was already registered in private client's list");
    	}
    	
    	logger.log(Level.INFO, "Private communication possible with " + login);
    	System.out.println("Private communication possible with " + login);
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
     * Assigns the given login with an unique ID, then return the port
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
    
    /**
     * Checks if the public server is accessible or not,
     * prints a message if it's not and return false.
     * 
     * @return true if the public server is accessible, false otherwise.
     */
    private boolean ensurePublicServerValid() {
    	if ( publicServerChannelKey.isValid() ) {
    		return true;
    	}
    	
    	System.out.println("The public server is no longer accessible");
    	return false;
    }

    /**
     * Adds a string message to the public server's queue.
     * 
     * @param msg The message to send.
     */
    public void sendPublicMessage(String msg) {
    	if ( !ensurePublicServerValid() ) {
    		return;
    	}
    	
    	ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    	
    	ctx.queueMessage(new PublicMessageFromCliFrame(msg));
    }
    
    /**
     * Sends a frame to the public server in order to request the recipient client 'login'
     * to establish a private communication.</br>
     * Creates the messages and files queue to send after and if communication is establish.
     * 
     * @param login The login of the recipient client.
     */
    private void sendPrivateRequest(String login) {
    	if ( !ensurePublicServerValid() ) {
    		return;
    	}
    	
    	ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    	
		ctx.queueMessage(new PrivateRequestFrame(login));
		System.out.println("Requesting " + login + " to establish a private communication...");
		
		/* Creating messages's queue */
		privatePendingMessages.put(login, new LinkedList<PrivateMessageFrame>());
		privatePendingFiles.put(login, new LinkedList<String>());
    }
    
    /**
     * Performs action according to the given response to the private communication request.</br>
     * Removes the given client from the 'asking clients list' if the answer is 'no'.</br>
     * Establishes a connection and registers the given client as a private client otherwise.
     * 
     * @param response The answer to the private communication request.
     * @param login The login of the recipient client.
     */
    private void answerToPrivateRequest(String response, String login) {
    	if ( !ensurePublicServerValid() ) {
    		return;
    	}
    	
    	ClientContext ctx = (ClientContext) publicServerChannelKey.attachment();
    	
		if ( response.toUpperCase().equals("NO") ) {	// Refusing the private communication.
			ctx.queueMessage(new PrivateAnswerFromCliFrame(login));
		} else {										// Accepting the private communication.
			ctx.queueMessage(assignAndGetNewPrivateId(login));
		}
		privateAskingClients.remove(login);
    }
    
    /**
     * Adds a string message to the private client's queue.<br>
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
    		/* There are 2 cases :
    		 * - This client is requesting to establish a private communication.
    		 * - This client is responding to a private communication request.
    		 */
    		
        	if ( !ensurePublicServerValid() ) {
        		// Impossible to access the public server.
        		return;
        	}
    		
    		// If this client is responding to a private communication request.
    		if ( privateAskingClients.containsKey(login) ) {
    			answerToPrivateRequest(msg, login);
    		}
    		
    		// If this client is requesting to establish a private communication.
    		else {
        		/* Ask for private communication establishment */
    			sendPrivateRequest(login);
        		
        		/* Queuing messages */
        		privatePendingMessages.get(login).add(new PrivateMessageFrame(msg));
    		}
    	}
    }
    
    /**
     * Starts a new thread which send the content of a file by rounds of chunks,
     * until the whole file is sent.
     * 
     * @param ctx The recipient client context.
     * @param fileName The file path to send.
     * @param currentId The file ID.
     */
    private void sendFileByRounds(ClientContext ctx, String fileName, int currentId) {
		log(Level.INFO, "Starting sending the file " + fileName);

		new Thread(() -> {
			try (FileInputStream ios = new FileInputStream(filesRepertory.resolve(fileName).toString())) {
				byte[] buffer = new byte[524_288];
				var read = 0;
				var pos = 0;
				var dataSize = 0;
				try {
					while ((read = ios.read(buffer)) != -1) {
						while (ctx.getQueueSize() > 0) {
							
							var lock = ctx.getLock();
							lock.lock();
							try {
								ctx.getIsQueueEmptyCondition().await();
							} catch (InterruptedException e) {
								return;
							}finally {
								lock.unlock();
							}
						}
						while(pos < read) {
							
							var subArray = Arrays.copyOfRange(buffer, pos, pos+1024 > read ? read : pos + 1024);
							dataSize = pos+1024 > read ? read - pos : 1024;
							pos += 1024;
							var frame = new DlFileFrame(currentId, dataSize, subArray);
							ctx.queueMessage(frame);

						}
						pos = 0;
						
					}
					System.out.println("File " + fileName + " uploaded");
					log(Level.INFO, "Sending of file " + fileName + " complete");
				} catch (IOException ioe) {
					log(Level.SEVERE, "Error while reading the file to send", ioe);
					return;
				}
			} catch (FileNotFoundException fnfe) {
				log(Level.SEVERE, "File not found", fnfe); // this should never happened
				return;
			} catch (IOException ioe2) {
				log(Level.SEVERE, "Error while closing FileInputStream", ioe2);
			}
		}).start();
    }
    
    /**
     * Sends a file to the recipient client, by rounds of chunks.
     * 
     * @param fileName The name of the file in the files directory.
     * @param login The login of the recipient client.
     * 
     * @throws FileNotFoundException It should never happened.
     */
    public void sendFile(String fileName, String login) {
		if (!privateClients.containsKey(login)) {				// If communication has not been established yet
			if ( privatePendingFiles.containsKey(login) ) {		// If request for private communication has been sent but not yet established
				privatePendingFiles.get(login).add(fileName);
			} else {											// If request for private communication establishment has not been sent
	    		/* There are 2 cases :
	    		 * - This client is requesting to establish a private communication.
	    		 * - This client is responding to a private communication request.
	    		 */
				
	        	if ( !ensurePublicServerValid() ) {
	        		// Impossible to access the public server.
	        		return;
	        	}
	    		
	    		// If this client is responding to a private communication request.
	    		if ( privateAskingClients.containsKey(login) ) {
	    			answerToPrivateRequest(fileName, login);
	    		}
	    		
	    		// If this client is requesting to establish a private communication.
	    		else {
	        		/* Ask for private communication establishment */
	    			sendPrivateRequest(login);
	        		
	        		/* Queuing messages */
	    			privatePendingFiles.get(login).add(fileName);
	    		}
			}
			
			return;
		}

		var path = filesRepertory.resolve(fileName).toString();
		var f = new File(path);
		
		if (!f.exists()) {
			System.err.println("Can't resolve path '" + path + "'");
			return;
		}
		
		if (f.length() > Integer.MAX_VALUE) {
			System.err.println("Only files smaller than 2GB are allowed");
			return;
		}
		
		var currentId = fileId;
		fileId++;
		
		var ctx = privateClients.get(login);
		ctx.queueMessage(new InitSendFileFrame(fileName, (int) f.length(), currentId));
		
		sendFileByRounds(ctx, fileName, currentId);
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
    public void clearPendingFiles(String login) {
    	privatePendingFiles.remove(login);
    }
    
    @Override
    public void addAskingClient(String login, ClientContext ctx) {
		System.out.println(login + " wants to establish a communication with you.");
		System.out.println("Reply to " + login + " with 'no' if you want to refuse. " +
						   "Other replies to " + login + " will have the effect of accepting the communication.");
    	
    	privateAskingClients.put(login, ctx);
    }
    
    @Override
    public FileOutputStream createNewFile(String fileName) {
		File dir = new File(filesRepertory.toString());
		var pathNumber = 1;
		if (!dir.exists())
			dir.mkdirs();
		var path = filesRepertory.resolve(fileName).toString();
		File f = new File(path);
		try {
			// Creates a new file like fileName (i) if already exist
			while (!f.createNewFile()) {
                var splited = path.split("\\.");
                if (splited.length > 1) {
                	splited[splited.length - 2] =  splited[splited.length - 2] + "_(" + pathNumber + ")";
                } else {
                    //length == 1
                    splited[0] = splited[0] + "(" + pathNumber + ")";
                }
                var newPath = String.join(".", splited);
                f = new File(newPath);
                pathNumber++;
            }
		} catch (IOException e) {
			System.err.println("File can't be created");
		}
		try {
			return new FileOutputStream(f.getPath(), true); // Creates the file in append mode
		} catch (FileNotFoundException e) {
			System.err.println("Probleme while creating FileOutputStream");
			return null; // Should never happened
		}
    }
}
