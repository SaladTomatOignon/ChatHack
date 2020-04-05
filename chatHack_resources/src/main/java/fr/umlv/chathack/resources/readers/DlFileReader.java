package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.DlFileFrame;


public class DlFileReader implements Reader{
	private enum State {
		DONE, WAITING_FILE_ID, WAITING_DATA_SIZE, WAITING_DATA, ERROR
	}
	
	private int fileId;
	private int dataSize;
	private byte[] data = new byte[1024];
	
	private final ByteBuffer bb;
	private State state = State.WAITING_FILE_ID;
	
	
	
	
	
	public DlFileReader(ByteBuffer bb) {
		this.bb = bb;
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {

			
			
			
		case WAITING_FILE_ID:
			if (bb.remaining() >= Integer.BYTES) {
				fileId = bb.getInt();
				state = State.WAITING_FILE_ID;
			}else {
				return ProcessStatus.REFILL;
			}
			
		case WAITING_DATA_SIZE:
			if (bb.remaining() >= Integer.BYTES) {
				dataSize = bb.getInt();
				state = State.WAITING_FILE_ID;
			}else {
				return ProcessStatus.REFILL;
			}
			
		case WAITING_DATA:
			if (bb.remaining() >= dataSize) {
				bb.get(data, 0, dataSize);
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
		return new DlFileFrame(fileId, dataSize, data);
	}

	@Override
	public void reset() {
		state = State.WAITING_FILE_ID;

		
	}
}
