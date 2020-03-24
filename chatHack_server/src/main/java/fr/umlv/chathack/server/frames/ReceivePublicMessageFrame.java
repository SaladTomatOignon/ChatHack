package fr.umlv.chathack.server.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import fr.umlv.chathack.server.core.Context;

public class ReceivePublicMessageFrame implements Frame {

	private String message;
	private String name;

	public ReceivePublicMessageFrame(String name, String message) {
		this.name = name;
		this.message = message;
	}

	@Override
	public void accept(Context ctx) {
		System.out.println("name : " + name + ", message : " + message);
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var messageEncode = cs.encode(message);
		var nameEncode = cs.encode(name);
		bb.put((byte) 1);

		bb.putInt(nameEncode.remaining());
		bb.put(nameEncode);
		
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
		
		return cs.encode(message).remaining() + 1 + 4;
		

	}
	

}
