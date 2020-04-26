package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AuthBddFrame extends AbstractFrame {
	private long id;
	private String login;
	private String pass;

	public AuthBddFrame(long id, String login, String pass) {
		this.id = id;
		this.login = login;
		this.pass = pass;
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
		var passEncode = cs.encode(pass);
		bb.put((byte) 1);
		bb.putLong(id);

		bb.putInt(loginEncode.remaining());
		bb.put(loginEncode);
		
		bb.putInt(passEncode.remaining());
		bb.put(passEncode);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}

	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		return cs.encode(login).remaining() + cs.encode(pass).remaining() + 1 + Long.BYTES + 8;
		

	}

}
