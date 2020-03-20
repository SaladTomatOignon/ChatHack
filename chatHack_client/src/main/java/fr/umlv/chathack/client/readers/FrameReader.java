package fr.umlv.chathack.client.readers;

import java.nio.ByteBuffer;

public class FrameReader implements Reader {

	private enum State {
		DONE, WAITING_OPCODE, WAITING_TRAME, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_OPCODE;

	private byte opCode;

	private InitConnectionReader iCR;

	public FrameReader(ByteBuffer bb) {
		this.bb = bb;
	}

	public static FrameReader createFrameReaderCli(ByteBuffer bb) {
		var reader = new FrameReader(bb);
		reader.iCR = new InitConnectionReader(bb);
		return reader;
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		bb.flip();

		if (state == State.WAITING_OPCODE && bb.remaining() >= Byte.BYTES) {
			opCode = bb.get();
			state = State.WAITING_TRAME;
		}
		ProcessStatus status;
		switch (opCode) {
		case 0:
			status = iCR.process();
			if (status != ProcessStatus.DONE) {
				return status;
			} else {
				state = State.DONE;
				System.out.println("responce code : " + (byte) iCR.get());
				return ProcessStatus.DONE;
			}

		case 1: // TODO Call apropriate function of coresponding reader
			System.out.println(1);
			break;
		case 2: // TODO Call apropriate function of coresponding reader
			System.out.println(2);
			break;
		case 3: // TODO Call apropriate function of coresponding reader
			System.out.println(3);
			break;
		case 4: // TODO Call apropriate function of coresponding reader
			System.out.println(4);
			break;
		default:
			throw new IllegalStateException("Non valid Op_Code");
		}
		return ProcessStatus.REFILL;
	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}

		return opCode;
	}

	@Override
	public void reset() {
		state = State.WAITING_OPCODE;
		switch (opCode) {
		case 0:
			iCR.reset();
		case 1: // TODO Call apropriate function of coresponding reader
			System.out.println(1);
			break;
		case 2: // TODO Call apropriate function of coresponding reader
			System.out.println(2);
			break;
		case 3: // TODO Call apropriate function of coresponding reader
			System.out.println(3);
			break;
		case 4: // TODO Call apropriate function of coresponding reader
			System.out.println(4);
			break;
		default:
			throw new IllegalStateException("Non valid Op_Code");
		}
	}

}
