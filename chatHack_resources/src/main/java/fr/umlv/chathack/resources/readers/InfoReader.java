package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.InfoFrame;

public class InfoReader implements Reader{
	private enum State {
		DONE, WAITING_INFO_CODE, WAITING_MESSAGE, ERROR
	}

	private String message;
	private byte infoCode;

	private final ByteBuffer bb;
	private State state = State.WAITING_INFO_CODE;

	private StringReader strReader;

	public InfoReader(ByteBuffer bb) {
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
		case WAITING_INFO_CODE:
			if (bb.remaining() >= Byte.BYTES) {
				infoCode = bb.get();
				state = State.WAITING_MESSAGE;
			}

		case WAITING_MESSAGE:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				message = (String) strReader.get();
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
		return new InfoFrame(infoCode, message);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_INFO_CODE;
	}

}
