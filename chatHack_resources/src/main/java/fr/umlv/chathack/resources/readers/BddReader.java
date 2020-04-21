package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.BddNegativeResponceFrame;
import fr.umlv.chathack.resources.frames.BddPositiveResponceFrame;

public class BddReader implements Reader {

	private enum State {
		DONE, WAITING_OPCODE, WAITING_ID, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_OPCODE;

	private byte opCode;
	private long id;




	public BddReader(ByteBuffer bb) {
		this.bb = bb;
	}



	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		bb.flip();

		if (state == State.WAITING_OPCODE && bb.remaining() >= Byte.BYTES) {
			opCode = bb.get();
			state = State.WAITING_ID;
		}else {
			return ProcessStatus.REFILL;
		}

		if (state == State.WAITING_ID && bb.remaining() >= Long.BYTES) {
			id = bb.getLong();
			state = State.DONE;
			return ProcessStatus.DONE;
		}else {
			return ProcessStatus.REFILL;
		}
	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}

		if (opCode == 0) {
			return new BddNegativeResponceFrame(id);
		}
		return new BddPositiveResponceFrame(id);
	}

	@Override
	public void reset() {
		if (bb.position() != 0) {
			bb.compact();
		}

		state = State.WAITING_OPCODE;
		
	}


}
