package fr.umlv.chathack.server.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.server.frames.ConnectionAnswerFrame;

public class ConnectionAnswerReader implements Reader{
	private enum State {
		DONE, WAITING_RESPONCE_CODE, ERROR
	}

	private byte responceCode;

	private final ByteBuffer bb;
	private State state = State.WAITING_RESPONCE_CODE;

	
	
	public ConnectionAnswerReader(ByteBuffer bb) {
		this.bb = bb;
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
		case WAITING_RESPONCE_CODE:
			if (bb.remaining() >= Byte.BYTES) {
				responceCode = bb.get();
				state = State.DONE;
				return ProcessStatus.DONE;
			}else {
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
		return new ConnectionAnswerFrame(responceCode);
	}

	@Override
	public void reset() {
		state = State.WAITING_RESPONCE_CODE;
		
	}
	
	
	
	
	
}
