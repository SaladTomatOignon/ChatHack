package fr.umlv.chathack.server.readers;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import fr.umlv.chathack.server.frames.Frame;




public class FrameReader implements Reader{
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
	
	private final Map<Byte, Supplier<ProcessStatus>> map = new HashMap<Byte, Supplier<ProcessStatus>>();
	

	public FrameReader(ByteBuffer bb, ConnectionReader connectionReader, PublicMessageReader publicMessageReader,
			PrivateRequestReader privateRequestReader, PrivateAnswerReader privateAnswerReader) {
		super();
		this.bb = bb;
		this.connectionReader = connectionReader;
		this.publicMessageReader = publicMessageReader;
		this.privateRequestReader = privateRequestReader;
		this.privateAnswerReader = privateAnswerReader;
		map.put((byte) 0, () -> processReader(connectionReader));
		map.put((byte) 1, () -> processReader(publicMessageReader));
		map.put((byte) 2, () -> processReader(privateRequestReader));
		map.put((byte) 3, () -> processReader(privateAnswerReader));
	}

	public FrameReader(ByteBuffer bb) {
		this(bb, new ConnectionReader(bb), new PublicMessageReader(bb), new PrivateRequestReader(bb), new PrivateAnswerReader(bb));
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
		
		if(state == State.WAITING_TRAME) {
			return map.get(opCode).get();
		}
		
		return ProcessStatus.REFILL;
		
		
//		ProcessStatus status;
//		switch (opCode) {
//		case 0:
//			status = connectionReader.process();
//			if (status != ProcessStatus.DONE) {
//				bb.compact();
//				return status;
//			} else {
//				state = State.DONE;
//				frame = (Frame) connectionReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 1: 
//			status = publicMessageReader.process();
//			if (status != ProcessStatus.DONE) {
//				bb.compact();
//				return status;
//			} else {
//				state = State.DONE;
//				frame = (Frame) publicMessageReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 2: 
//			status = privateRequestReader.process();
//			if (status != ProcessStatus.DONE) {
//				bb.compact();
//				return status;
//			} else {
//				state = State.DONE;
//				frame = (Frame) privateRequestReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 3: 
//			status = privateAnswerReader.process();
//			if (status != ProcessStatus.DONE) {
//				bb.compact();
//				return status;
//			} else {
//				state = State.DONE;
//				frame = (Frame) privateAnswerReader.get();
//				return ProcessStatus.DONE;
//			}
//		default:
//			return ProcessStatus.ERROR;
//		}
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
		if (bb.position() != 0) {
            bb.compact();
        }
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
		case 3: 
			privateAnswerReader.reset();
			break;
		
		}
	}
	
	
	private ProcessStatus processReader(Reader reader) {
		var status = reader.process();
		if (status != ProcessStatus.DONE) {
			bb.compact();
			return status;
		} else {
			state = State.DONE;
			frame = (Frame) reader.get();
			return ProcessStatus.DONE;
		}
	}
}
