package fr.umlv.chathack.server.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.server.frames.ConnectionFrame;

public class ConnectionReader implements Reader {

	private enum State {
		DONE, WAITING_PASS_CODE, WAITING_NAME, WAITING_PASS, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_PASS_CODE;

	private boolean passNeed;
	private String name;
	private String pass;

	private StringReader strReader;

	public ConnectionReader(ByteBuffer bb) {
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
		case WAITING_PASS_CODE:
			if (bb.remaining() >= Byte.BYTES) {
				passNeed = bb.get() == 0;
				state = State.WAITING_NAME;
			}

		case WAITING_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				name = (String) strReader.get();
				if (!passNeed) {
					state = State.DONE;
					return ProcessStatus.DONE;
				} else {
					strReader.reset();
					state = State.WAITING_PASS;
				}
			} else {
				return status;
			}

		case WAITING_PASS:
			status = strReader.process();
			if (state == State.WAITING_PASS && status == ProcessStatus.DONE) {
				pass = (String) strReader.get();

				state = State.DONE;
				return ProcessStatus.DONE;

			} else {
				return status;
			}
		default:
			throw new IllegalStateException();

		}

//		
//		if (state == State.WAITING_PASS_CODE && bb.remaining() >= Byte.BYTES) {
//			passNeed = bb.get() == 0;
//			state = State.WAITING_NAME;
//		}
//		var status = strReader.process();
//		if (state == State.WAITING_NAME && status == ProcessStatus.DONE) {
//			name = (String) strReader.get();
//			if (!passNeed) {
//				state = State.DONE;
//				return ProcessStatus.DONE;
//			} else {
//				strReader.reset();
//				state = State.WAITING_PASS;
//			}
//		} else {
//			return status;
//		}
//		status = strReader.process();
//		System.out.println("pass status : " + status);
//		if (state == State.WAITING_PASS && status == ProcessStatus.DONE) {
//			pass = (String) strReader.get();
//
//			state = State.DONE;
//			return ProcessStatus.DONE;
//
//		} else {
//			return status;
//		}
//		return ProcessStatus.REFILL;
	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		if (passNeed) {
			return new ConnectionFrame(name, pass, passNeed);
		} else {
			return new ConnectionFrame(name);
		}
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_PASS_CODE;

	}

}
