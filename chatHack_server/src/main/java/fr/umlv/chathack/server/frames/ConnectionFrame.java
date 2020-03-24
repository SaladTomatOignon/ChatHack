package fr.umlv.chathack.server.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import fr.umlv.chathack.server.core.Context;

public class ConnectionFrame implements Frame {
	private String name;
	private String pass;
	private boolean passNeed;

	public ConnectionFrame(String name, String pass, boolean passNeed) {
		this.name = name;
		this.pass = pass;
		this.passNeed = passNeed;
	}

	public ConnectionFrame(String name) {
		this(name, "", false);
	}

	@Override
	public void accept(Context ctx) {
		byte connectionStatus = passNeed ? ctx.tryLogin(name, pass) : ctx.tryLogin(name);
		ctx.queueMessage(new ConnectionAnswerFrame(connectionStatus));
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);
		bb.put((byte) 0);

		if (passNeed) {
			bb.put((byte) 0);
			bb.putInt(nameEncode.remaining());
			bb.put(nameEncode);
			var passEncode = cs.encode(pass);
			bb.putInt(passEncode.remaining());
			bb.put(passEncode);
		} else {
			bb.put((byte) 1);
			bb.putInt(nameEncode.remaining());
			bb.put(nameEncode);
		}
		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		if (passNeed) {
			return cs.encode(name).remaining() + cs.encode(pass).remaining() + 2 + 8;
		} else {
			return cs.encode(name).remaining() + 2 + 4;
		}

	}

}
