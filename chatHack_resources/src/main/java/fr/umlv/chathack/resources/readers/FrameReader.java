package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import fr.umlv.chathack.resources.frames.Frame;

public class FrameReader implements Reader {

	private enum State {
		DONE, WAITING_OPCODE, WAITING_TRAME, ERROR
	}

	private final ByteBuffer bb;
	private State state = State.WAITING_OPCODE;

	private byte opCode;
	
	private Frame frame;

	private ConnectionAnswerReader connectionAnswerReader;
	private PublicMessageFromServReader publicMessageFromServReader;
	private PrivateRequestReader privateRequestReader;
//	private PrivateAnswerFromCliReader privateAnswerFromCliReader;
	private InfoReader infoReader;
	

	private ConnectionReader connectionReader;
	private PublicMessageFromCliReader publicMessageFromCliReader;
//	private PrivateRequestReader privateRequestReader;
	private PrivateAnswerFromCliReader privateAnswerFromCliReader;
	private PrivateMessageReader privateMessageReader;
	private InitSendFileReader initSendFileReader;
	private DlFileReader dlFileReader;
	
	
	
	private final Map<Byte, Supplier<ProcessStatus>> map = new HashMap<Byte, Supplier<ProcessStatus>>();
	
	
	
	private FrameReader(ByteBuffer bb, ConnectionAnswerReader connectionAnswerReader,
			PublicMessageFromServReader publicMessageFromServReader, PrivateRequestReader privateRequestReader,
			PrivateAnswerFromCliReader privateAnswerFromCliReader, InfoReader infoReader) {
		this.bb = bb;
		this.connectionAnswerReader = connectionAnswerReader;
		this.publicMessageFromServReader = publicMessageFromServReader;
		this.privateRequestReader = privateRequestReader;
		this.privateAnswerFromCliReader = privateAnswerFromCliReader;
	}





	public FrameReader(ByteBuffer bb) {
		this(bb, new ConnectionAnswerReader(bb), new PublicMessageFromServReader(bb), new PrivateRequestReader(bb),  new PrivateAnswerFromCliReader(bb), new InfoReader(bb));
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
//			return test(connectionAnswerReader);
////			status = connectionAnswerReader.process();
////			if (status != ProcessStatus.DONE) {
////				return status;
////			} else {
////				state = State.DONE;
////				bb.compact();
////				frame = (Frame) connectionAnswerReader.get();
////				return ProcessStatus.DONE;
////			}
//
//		case 1: 
//			status = publicMessageReader.process();
//			if (status != ProcessStatus.DONE) {
//				return status;
//			} else {
//				state = State.DONE;
//				bb.compact();
//				frame = (Frame) publicMessageReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 2: 
//			status = privateRequestReader.process();
//			if (status != ProcessStatus.DONE) {
//				return status;
//			} else {
//				state = State.DONE;
//				bb.compact();
//				frame = (Frame) privateRequestReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 3: 
//			status = privateAnswerReader.process();
//			if (status != ProcessStatus.DONE) {
//				return status;
//			} else {
//				state = State.DONE;
//				bb.compact();
//				frame = (Frame) privateAnswerReader.get();
//				return ProcessStatus.DONE;
//			}
//		case 4: // TODO Call apropriate function of coresponding reader
//			System.out.println(4);
//			break;
//		default:
//			throw new IllegalStateException("Non valid Op_Code");
//		}
//		return ProcessStatus.REFILL;
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
		if (bb.position() != 0) {
			bb.compact();
		}
		
		state = State.WAITING_OPCODE;
		switch (opCode) {
		case 0:
			connectionAnswerReader.reset();
			break;
		case 1:
			publicMessageFromServReader.reset();
			break;
		case 2: 
			privateRequestReader.reset();
			break;
		case 3:
			privateAnswerFromCliReader.reset();
			break;
		case 4: 
			infoReader.reset();
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
