package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.AuthBddFrame;

//pretty sure it's useless
public class AuthBddReader implements Reader{
	private enum State {
		DONE, WAITING_ID, WAITING_LOGIN, WAITING_PASS, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_ID;

	private long id;
	private String login;
	private String pass;

	private StringReader strReader;

	public AuthBddReader(ByteBuffer bb) {
		this.bb = bb;
		strReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		ProcessStatus status;
		switch (state) {
		case WAITING_ID:
			if (bb.remaining() >= Long.BYTES) {
				id = bb.getLong();
				state = State.WAITING_LOGIN;
			}

		case WAITING_LOGIN:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				login = (String) strReader.get();
				state = State.WAITING_PASS;
			} else {
				return status;
			}

		case WAITING_PASS:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				pass = (String) strReader.get();

				state = State.DONE;
				return ProcessStatus.DONE;

			} else {
				return status;
			}
		default:
			throw new IllegalStateException();

		}


	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new AuthBddFrame(id, login, pass);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_ID;

	}
}
