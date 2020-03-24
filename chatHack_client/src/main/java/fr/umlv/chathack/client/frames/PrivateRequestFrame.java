package fr.umlv.chathack.client.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateRequestFrame implements Frame{

	private String name;
	
	
	public PrivateRequestFrame(String name) {
		super();
		this.name = name;
	}


	@Override
	public void accept() {
		System.out.println(name);
	}
	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);
		
		bb.put((byte) 2);

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
		
		return cs.encode(name).remaining() + 1 + 4;
		

	}

}
