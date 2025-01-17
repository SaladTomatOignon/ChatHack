package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateRequestFrame extends AbstractFrame {

	private String name;
	
	
	public PrivateRequestFrame(String name) {
		super();
		this.name = name;
	}
	
	@Override
	public void accept(ClientVisitor client) {
		client.askForPrivateCommunication(name);
	}
	
	@Override
	public void accept(ServerVisitor server) {
		try {
			server.sendFrame(new PrivateRequestFrame(server.getLogin()), name);
		} catch (IllegalArgumentException iae) {
			// The recipient client does not exists.
			server.sendBackFrame(new PrivateAnswerFrame((byte) 2, name));
		}
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
