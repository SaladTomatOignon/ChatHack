package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.PublicMessageFromServFrame;



public class PublicMessageFromServReader implements Reader{
	
	private enum State {
		DONE,WAITING_NAME, WAITING_MESSAGE, ERROR
	}
	
	private String message;
	private String name;
	
	private State state = State.WAITING_NAME;
	
	private StringReader strReader;
	
	
	
	
	public PublicMessageFromServReader(ByteBuffer bb) {
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
				strReader.reset();
				state = State.WAITING_MESSAGE;

			} else {
				return status;
			}
		case WAITING_MESSAGE:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				message = (String) strReader.get();

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
		return new PublicMessageFromServFrame(name, message);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_NAME;

		
	}
	
	
}
