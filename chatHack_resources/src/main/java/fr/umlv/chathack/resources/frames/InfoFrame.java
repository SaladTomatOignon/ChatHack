package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class InfoFrame implements Frame{

	private byte infoCode;
	private String message;
	
	
	
	
	public InfoFrame(byte infoCode, String message) {
		this.infoCode = infoCode;
		this.message = message;
	}




	public void accept() {
		System.out.println("infoCode : " + infoCode + " message : " + message);
	}
	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var messageEncode = cs.encode(message);
		
		bb.put((byte) 4 );
		
		bb.put(infoCode);
		
		bb.putInt(messageEncode.remaining());
		bb.put(messageEncode);
		
		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		
		return cs.encode(message).remaining() + 2 + 4;
		

	}
	
}
