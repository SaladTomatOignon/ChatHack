package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.InitSendFileFrame;


public class InitSendFileReader implements Reader{

	private enum State {
		DONE, WAITING_FILE_NAME, WAITING_FILE_SIZE, WAITING_FILE_ID , ERROR
	}
	
	private String fileName;
	private int fileSize;
	private int fileId;
	
	private final ByteBuffer bb;
	private State state = State.WAITING_FILE_NAME;
	
	private StringReader strReader;
	
	
	
	
	public InitSendFileReader(ByteBuffer bb) {
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

			
		case WAITING_FILE_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				fileName = (String) strReader.get();

				state = State.WAITING_FILE_SIZE;

			} else {
				return status;
			}
			
		case WAITING_FILE_SIZE:
			if (bb.remaining() >= Integer.BYTES) {
				fileSize = bb.getInt();
				state = State.WAITING_FILE_ID;
			}else {
				return ProcessStatus.REFILL;
			}
			
		case WAITING_FILE_ID:
			if (bb.remaining() >= Integer.BYTES) {
				fileId = bb.getInt();
				state = State.DONE;
				return ProcessStatus.DONE;
			}else {
				return ProcessStatus.REFILL;
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
		return new InitSendFileFrame(fileName, fileSize, fileId);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_FILE_NAME;

		
	}

}
