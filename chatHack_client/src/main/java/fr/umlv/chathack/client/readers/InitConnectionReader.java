package fr.umlv.chathack.client.readers;

import java.nio.ByteBuffer;

public class InitConnectionReader implements Reader {
	private enum State {
		DONE, WAITING_RESPONSE_CODE, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_RESPONSE_CODE;

	private byte responseCode;

	public InitConnectionReader(ByteBuffer bb) {
		this.bb = bb;
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		try {
			if (state == State.WAITING_RESPONSE_CODE && bb.remaining() >= Byte.BYTES) {
				responseCode = bb.get();
				state = State.DONE;
				return ProcessStatus.DONE;
			} else {
				return ProcessStatus.REFILL;
			}
		} finally {
			bb.compact();
		}
	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return responseCode;
	}

	@Override
	public void reset() {
		state = State.WAITING_RESPONSE_CODE;
	}

}
