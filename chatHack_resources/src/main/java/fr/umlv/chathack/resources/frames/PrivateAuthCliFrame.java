package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateAuthCliFrame extends AbstractFrame {
	private String name;
	private int tokenId;
	

	public PrivateAuthCliFrame(String name, int tokenId) {
		this.name = name;
		this.tokenId = tokenId;
	}

	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		
		return cs.encode(name).remaining() + 1 + 4 + 4;
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);

		bb.put((byte) 4);

		
		
		bb.putInt(nameEncode.remaining());
		bb.put(nameEncode);
		
		bb.putInt(tokenId);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	
	}

	@Override
	public void accept(ClientVisitor client) {
		client.tryAuthenticate(tokenId, name);
	}
}
