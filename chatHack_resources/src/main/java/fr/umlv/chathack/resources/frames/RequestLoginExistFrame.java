package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestLoginExistFrame implements Frame{
	private long id;
	private String login;

	public RequestLoginExistFrame(long id, String login) {
		this.id = id;
		this.login = login;
	}

	@Override
	public void accept(ServerVisitor server) {
		// TODO
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var loginEncode = cs.encode(login);
		
		bb.put((byte) 2);
		bb.putLong(id);

		bb.putInt(loginEncode.remaining());
		bb.put(loginEncode);
		
		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}

	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		return cs.encode(login).remaining() + 1 + Long.BYTES + 4;
		

	}

}
