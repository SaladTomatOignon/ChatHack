package fr.umlv.chathack.client.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.client.frames.PrivateAnswerFrame;


public class PrivateAnswerReader implements Reader{
	private enum State {
		DONE, WAITING_RESPONCE_CODE, WAITING_NAME, ERROR
	}

	private String name;
	private byte responceCode;

	private final ByteBuffer bb;
	private State state = State.WAITING_RESPONCE_CODE;

	private StringReader strReader;

	public PrivateAnswerReader(ByteBuffer bb) {
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
			}

		case WAITING_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				name = (String) strReader.get();
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
		return new PrivateAnswerFrame(responceCode, name);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_RESPONCE_CODE;
	}

}