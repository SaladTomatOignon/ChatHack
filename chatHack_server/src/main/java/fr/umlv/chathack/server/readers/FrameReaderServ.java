package fr.umlv.chathack.server.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.server.frames.Frame;




public class FrameReaderServ implements Reader{
	private enum State {
		DONE, WAITING_OPCODE, WAITING_TRAME, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_OPCODE;

	private byte opCode;
	
	private ConnectionReader connectionReader;
	private PublicMessageReader publicMessageReader;
	private PrivateRequestReader privateRequestReader;
	private PrivateAnswerReader privateAnswerReader;
	
	private Frame frame;
	

	public FrameReaderServ(ByteBuffer bb) {
		this.bb = bb;
		connectionReader = new ConnectionReader(bb);
		publicMessageReader = new PublicMessageReader(bb);
		privateRequestReader = new PrivateRequestReader(bb);
		privateAnswerReader = new PrivateAnswerReader(bb);
	}
	
//	public static FrameReaderServ createFrameReaderCli(ByteBuffer bb) {
//		var reader = new FrameReaderServ(bb);
//		return reader;
//	}

	
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
			status = connectionReader.process();
			if (status != ProcessStatus.DONE) {
				bb.compact();
				return status;
			} else {
				state = State.DONE;
				frame = (Frame) connectionReader.get();
				return ProcessStatus.DONE;
			}
		case 1: 
			status = publicMessageReader.process();
			if (status != ProcessStatus.DONE) {
				bb.compact();
				return status;
			} else {
				state = State.DONE;
				frame = (Frame) publicMessageReader.get();
				return ProcessStatus.DONE;
			}
		case 2: 
			status = privateRequestReader.process();
			if (status != ProcessStatus.DONE) {
				bb.compact();
				return status;
			} else {
				state = State.DONE;
				frame = (Frame) privateRequestReader.get();
				return ProcessStatus.DONE;
			}
		case 3: 
			status = privateAnswerReader.process();
			if (status != ProcessStatus.DONE) {
				bb.compact();
				return status;
			} else {
				state = State.DONE;
				frame = (Frame) privateAnswerReader.get();
				return ProcessStatus.DONE;
			}
		default:
			return ProcessStatus.ERROR;
		}
	}

	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		
		return frame;
	}

	@Override
	public void reset() {
		state = State.WAITING_OPCODE;
		bb.compact();
		switch (opCode) {
		case 0:
			connectionReader.reset();
			break;
		case 1:
			publicMessageReader.reset();
			break;
		case 2: 
			privateRequestReader.reset();
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