package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import fr.umlv.chathack.resources.frames.Frame;

/**
 * 
 * This class is used to process the data in the ByteBuffer bb.
 * The state is used to know what to do when the method process is called.
 * The state WAITING_OPCODE mean that we are still waiting for the first byte of the frame. 
 * As soon as you got it, the method will process the data of the corresponding reader. 
 * the map is only use to associate an opCode to a reader. 
 * 
 * When the frame is totally receive the Frame will be created. 
 * And the method return the Frame corresponding to the data processed. 
 *
 */
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
		this.privateAnswerReader = privateAnswerReader;
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
	
	/**
	 * This method will try to get the opCode then it will call the associated function in the map.
	 */
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
			var fun =  map.get(opCode);
			if (fun == null) {
				return ProcessStatus.ERROR;
			}
			return fun.get();
		}

		return ProcessStatus.REFILL;
	}


	/**
	 * It will return the Frame corresponding of the one extracted from the ByteBuffer.
	 */
	@Override
	public Object get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}

		return frame;
	}

	
	/**
	 * Reset the object in his initial state.
	 */
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
			privateAnswerReader.reset();
			break;
		case 11:
			infoReader.reset();
			break;
		}
	}
	/**
	 * function to execute the reader's process method
	 * @param reader
	 * @return ProcessStatus DONE if finish or REFILL if needed or ERROR if one occur 
	 */
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
