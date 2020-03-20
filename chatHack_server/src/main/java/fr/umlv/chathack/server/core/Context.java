package fr.umlv.chathack.server.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.umlv.chathack.server.frames.Frame;
import fr.umlv.chathack.server.readers.FrameReader;

public class Context {
    final private SelectionKey key;
    final private SocketChannel sc;
    final private ChatHackServer server;
    final private ByteBuffer bbin;
    final private ByteBuffer bbout;
    final private FrameReader freader;
    
    private boolean closed;
    private boolean logged; // If the client is logged to the server
	
    public Context(ChatHackServer server, SelectionKey key){
        this.key = key;
        this.sc = (SocketChannel) key.channel();
        this.server = server;
        this.bbin = ByteBuffer.allocate(ChatHackServer.BUFFER_SIZE);
        this.bbout = ByteBuffer.allocate(ChatHackServer.BUFFER_SIZE);
        this.freader = new FrameReader(bbin);
        
        this.closed = false;
        this.logged = false;
    }
    
    /**
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call
     * to process and after the call
     *
     */
    private void processIn() {
    	switch ( freader.process() ) {
    		case DONE :
    			Frame frame = (Frame) freader.get();
    			freader.reset();
    			
    			frame.accept();
    			break;
    		case REFILL :
    			break;
    		case ERROR :
    			// TODO : Gestion des erreurs lorsqu'un paquet reçu n'est pas conforme.
    			freader.reset();
    			break;
    	}
    }
    
    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doRead and after the call
     *
     * @throws IOException
     */
    void doRead() throws IOException {
        if ( sc.read(bbin) == -1 ) {
            closed = true;
        }
        
        processIn();
    }
    
    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call
     * to doWrite and after the call
     *
     * @throws IOException
     */

    void doWrite() throws IOException {
        bbout.flip();
        
        sc.write(bbout);
        
        bbout.compact();
    }
}
