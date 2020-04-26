package fr.umlv.chathack.resources.frames;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateAnswerFromCliFrame extends AbstractFrame {
	
	private byte responceCode;
	private String name;

	private int port;
	private int id;
	
	
	
	private PrivateAnswerFromCliFrame(byte responceCode, String name, int port, int id) {
        this.responceCode = responceCode;
        this.name = name;
        this.port = port;
        this.id = id;
    }
	
	
    public PrivateAnswerFromCliFrame(String name, int port, int id) {
        this((byte) 0, name, port, id);
    }




    public PrivateAnswerFromCliFrame(String name) {
        this((byte) 1, name, -1, -1);
    }
    
    
    @Override
    public void accept(ServerVisitor server) throws IOException {
    	if ( responceCode == 0 ) {		// Request accepted
    		server.sendFrame(new PrivateAnswerFrame((byte) 0, server.getLogin(), server.getInetAddress(), port, id), name);
    	} else {						// Request refused
    		server.sendFrame(new PrivateAnswerFrame((byte) 1, server.getLogin()), name);
    	}
    }

	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);
		
		bb.put((byte) 3 );
		
		bb.put(responceCode);
		
		bb.putInt(nameEncode.remaining());
		bb.put(nameEncode);
		
		if (responceCode == 0) {
			bb.putInt(port);
			bb.putInt(id);
		}
		
		
		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		if (responceCode == 0) {
			return cs.encode(name).remaining() + 2 + 4 + 4 + 4;
		}
		return cs.encode(name).remaining() + 2 + 4;
		

	}
	
	
	
}
