package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.PrivateAnswerFromCliFrame;

public class PrivateAnswerFromCliReader implements Reader {
	private enum State {
		DONE, WAITING_RESPONCE_CODE, WAITING_NAME, WAITING_PORT, WAITING_ID, ERROR
	}

	private String name;
	private byte responceCode;
	private int port;
	private int id;

	private final ByteBuffer bb;
	private State state = State.WAITING_RESPONCE_CODE;

	private StringReader strReader;

	public PrivateAnswerFromCliReader(ByteBuffer bb) {
		this.bb = bb;
		this.strReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		ProcessStatus status;
		switch (state) {
		case WAITING_RESPONCE_CODE:
			if (bb.remaining() >= Byte.BYTES) {
				responceCode = bb.get();
				state = State.WAITING_NAME;
			} else {
				return ProcessStatus.REFILL;
			}

		case WAITING_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				name = (String) strReader.get();
				if (responceCode == 1) {
					state = State.DONE;
					return ProcessStatus.DONE;
				} else {
					state = State.WAITING_PORT;
				}

			} else {
				return ProcessStatus.REFILL;
			}
		case WAITING_PORT:
			if (bb.remaining() >= Integer.BYTES) {
				port = bb.getInt();
				state = State.WAITING_ID;
			} else {
				return ProcessStatus.REFILL;
			}
		case WAITING_ID:
			if (bb.remaining() >= Integer.BYTES) {
				id = bb.getInt();
				state = State.DONE;
				return ProcessStatus.DONE;
			} else {
				return ProcessStatus.REFILL;
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
		if (responceCode == 0) {
			return new PrivateAnswerFromCliFrame(name, port, id);
		}
		return new PrivateAnswerFromCliFrame(name);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_RESPONCE_CODE;
	}

}
