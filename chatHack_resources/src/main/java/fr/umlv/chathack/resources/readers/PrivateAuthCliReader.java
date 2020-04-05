package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.PrivateAuthCliFrame;

public class PrivateAuthCliReader implements Reader{


	private enum State {
		DONE, WAITING_NAME, WAITING_TOKEN_ID, ERROR
	}
	
	private String name;
	private int tokenId;
	
	private final ByteBuffer bb;
	private State state = State.WAITING_NAME;
	
	private StringReader strReader;
	
	
	
	
	public PrivateAuthCliReader(ByteBuffer bb) {
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
		case WAITING_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				name = (String) strReader.get();

				state = State.WAITING_TOKEN_ID;
			} else {
				return status;
			}
			
			
			
		case WAITING_TOKEN_ID:
			if (bb.remaining() >= Integer.BYTES) {
				tokenId = bb.getInt();
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
		return new PrivateAuthCliFrame(name, tokenId);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_NAME;

		
	}

}
