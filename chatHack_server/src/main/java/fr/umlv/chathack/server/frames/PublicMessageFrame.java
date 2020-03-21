package fr.umlv.chathack.server.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PublicMessageFrame implements Frame {

	private String message;

	public PublicMessageFrame(String message) {
		this.message = message;
	}

	@Override
	public void accept() {
		System.out.println(message);
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var messageEncode = cs.encode(message);
		bb.put((byte) 1);

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
