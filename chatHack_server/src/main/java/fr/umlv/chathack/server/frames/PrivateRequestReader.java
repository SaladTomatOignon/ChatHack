package fr.umlv.chathack.server.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.server.frames.PrivateRequestFrame;



public class PrivateRequestReader implements Reader{
	
	private enum State {
		DONE, WAITING_NAME, ERROR
	}
	
	
	private String name;
	
	private final ByteBuffer bb;
	private State state = State.WAITING_NAME;
	
	private StringReader strReader;
	
	
	
	
	
	
	public PrivateRequestReader(ByteBuffer bb) {
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

				state = State.DONE;
				return ProcessStatus.DONE;

			} else {
				return status;
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
		return new PrivateRequestFrame(name);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_NAME;
		
	}

	
	
}
