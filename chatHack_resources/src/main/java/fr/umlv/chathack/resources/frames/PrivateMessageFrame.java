package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateMessageFrame implements Frame{

	private int cliId;
	private String message;

	public PrivateMessageFrame(int cliId, String message) {
		this.cliId = cliId;
		this.message = message;
	}

	@Override
	public void accept() {
		System.out.println("cliId : " + cliId + ", message : " + message);
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var messageEncode = cs.encode(message);

		bb.put((byte) 4);

		bb.putInt(cliId);
		
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
		
		return cs.encode(message).remaining() + 1 + 4 + 4;
		

	}

}
