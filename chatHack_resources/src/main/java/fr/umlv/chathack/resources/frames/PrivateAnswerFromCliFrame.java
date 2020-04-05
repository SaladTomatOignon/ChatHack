package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateAnswerFromCliFrame implements Frame {
	private byte responceCode;
	private String name;

	private int port;
	private int id;
	
	
	
	
	
	
	public PrivateAnswerFromCliFrame(byte responceCode, String name, int port, int id) {
		this.responceCode = responceCode;
		this.name = name;
		this.port = port;
		this.id = id;
	}




	public PrivateAnswerFromCliFrame(byte connectionAccept, String name) {
		this(connectionAccept, name, -1, -1);
	}




	public void accept() {
		System.out.println("connectionAccept : " + responceCode + " name : " + name);
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
		
		bb.putInt(nameEncode.remaining());
		bb.put(nameEncode);
		
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
