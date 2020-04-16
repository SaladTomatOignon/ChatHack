package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class InfoFrame implements Frame{

	private byte infoCode;
	private String message;
	
	
	
	
	public InfoFrame(byte infoCode, String message) {
		this.infoCode = infoCode;
		this.message = message;
	}

	@Override
	public void accept(ServerVisitor server) {
		Level level = null;
		
		switch (infoCode) {
			case 0:
				level = Level.INFO;
				break;
			case 1:
				level = Level.WARNING;
				break;
			case 2:
				level = Level.SEVERE;
				break;
		}
		
		System.out.println("Message received : " + message);
		server.log(level, "Message received : " + message);
	}

	@Override
	public void accept(ClientVisitor client) {
		Level level = null;
		
		switch (infoCode) {
			case 0:
				level = Level.INFO;
				break;
			case 1:
				level = Level.WARNING;
				break;
			case 2:
				level = Level.SEVERE;
				break;
		}
		
		System.out.println("Message received : " + message);
		client.log(level, "Message received : " + message);
	}
	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var messageEncode = cs.encode(message);
		
		bb.put((byte) 11 );
		
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
