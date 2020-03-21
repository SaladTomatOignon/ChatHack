package fr.umlv.chathack.server.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateAnswerFrame implements Frame {
	private boolean connectionAccept;
	private String name;
	
	
	
	
	public PrivateAnswerFrame(boolean connectionAccept, String name) {
		this.connectionAccept = connectionAccept;
		this.name = name;
	}




	public void accept() {
		System.out.println("connectionAccept : " + connectionAccept + " name : " + name);
	}
	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);
		
		bb.put((byte) 3 );
		
		if(connectionAccept) {
			bb.put((byte) 0);
		}else {
			bb.put((byte) 1);
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
		
		return cs.encode(name).remaining() + 2 + 4;
		

	}
	
	
	
}
