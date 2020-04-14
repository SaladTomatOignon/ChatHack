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

	private ConnectionReader connectionReader;
	private PublicMessageFromCliReader publicMessageFromCliReader;
	private PrivateRequestReader privateRequestReader;
	private PrivateAnswerFromCliReader privateAnswerFromCliReader;
	private PrivateAuthCliReader privateAuthCliReader;
	private PrivateMessageReader privateMessageReader;
	private InitSendFileReader initSendFileReader;
	private DlFileReader dlFileReader;
	private ConnectionAnswerReader connectionAnswerReader;
	private PublicMessageFromServReader publicMessageFromServReader;
//	private PrivateRequestReader privateRequestReader;
	private PrivateAnswerReader privateAnswerReader;
	private InfoReader infoReader;

	private final Map<Byte, Supplier<ProcessStatus>> map = new HashMap<Byte, Supplier<ProcessStatus>>();

	private FrameReader(ByteBuffer bb, ConnectionReader connectionReader,
			PublicMessageFromCliReader publicMessageFromCliReader, PrivateRequestReader privateRequestReader,
			PrivateAnswerFromCliReader privateAnswerFromCliReader, PrivateAuthCliReader privateAuthCliReader,
			PrivateMessageReader privateMessageReader, InitSendFileReader initSendFileReader, DlFileReader dlFileReader,
			ConnectionAnswerReader connectionAnswerReader, PublicMessageFromServReader publicMessageFromServReader,
			PrivateAnswerReader privateAnswerReader, InfoReader infoReader) {

		this.bb = bb;
		this.connectionReader = connectionReader;
		this.publicMessageFromCliReader = publicMessageFromCliReader;
		this.privateRequestReader = privateRequestReader;
		this.privateAnswerFromCliReader = privateAnswerFromCliReader;
		this.privateAuthCliReader = privateAuthCliReader;
		this.privateMessageReader = privateMessageReader;
		this.initSendFileReader = initSendFileReader;
		this.dlFileReader = dlFileReader;
		this.connectionAnswerReader = connectionAnswerReader;
		this.publicMessageFromServReader = publicMessageFromServReader;
		this.infoReader = infoReader;

		map.put((byte) 0, () -> processReader(connectionReader));
		map.put((byte) 1, () -> processReader(publicMessageFromCliReader));
		map.put((byte) 2, () -> processReader(privateRequestReader));
		map.put((byte) 3, () -> processReader(privateAnswerFromCliReader));
		map.put((byte) 4, () -> processReader(privateAuthCliReader));
		map.put((byte) 5, () -> processReader(privateMessageReader));
		map.put((byte) 6, () -> processReader(initSendFileReader));
		map.put((byte) 7, () -> processReader(dlFileReader));
		map.put((byte) 8, () -> processReader(connectionAnswerReader));
		map.put((byte) 9, () -> processReader(publicMessageFromServReader));
		map.put((byte) 10, () -> processReader(privateAnswerReader));
		map.put((byte) 11, () -> processReader(infoReader));
	}

	public FrameReader(ByteBuffer bb) {
		this(bb, new ConnectionReader(bb), new PublicMessageFromCliReader(bb), new PrivateRequestReader(bb),
				new PrivateAnswerFromCliReader(bb), new PrivateAuthCliReader(bb), new PrivateMessageReader(bb),
				new InitSendFileReader(bb), new DlFileReader(bb), new ConnectionAnswerReader(bb),
				new PublicMessageFromServReader(bb), new PrivateAnswerReader(bb), new InfoReader(bb));
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

		if (state == State.WAITING_TRAME) {
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
			connectionReader.reset();
			break;
		case 1:
			publicMessageFromCliReader.reset();
			break;
		case 2:
			privateRequestReader.reset();
			break;
		case 3:
			privateAnswerFromCliReader.reset();
			break;
		case 4:
			privateAuthCliReader.reset();
			break;
		case 5:
			privateMessageReader.reset();
			break;
		case 6:
			initSendFileReader.reset();
			break;
		case 7:
			dlFileReader.reset();
			break;
		case 8:
			connectionAnswerReader.reset();
			break;
		case 9:
			publicMessageFromServReader.reset();
			break;
		case 10:
			privateRequestReader.reset();
			break;
		case 11:
			privateAnswerReader.reset();
			break;
		case 12:
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
