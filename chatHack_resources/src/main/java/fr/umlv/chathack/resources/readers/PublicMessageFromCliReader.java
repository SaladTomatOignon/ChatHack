package fr.umlv.chathack.resources.readers;

import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.PublicMessageFromCliFrame;




public class PublicMessageFromCliReader implements Reader{
	
	private enum State {
		DONE, WAITING_MESSAGE, ERROR
	}
	
	private String message;
	
	private State state = State.WAITING_MESSAGE;
	
	private StringReader strReader;
	
	
	
	
	public PublicMessageFromCliReader(ByteBuffer bb) {
		this.strReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		ProcessStatus status;
		switch (state) {
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
		return new PublicMessageFromCliFrame(message);
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_MESSAGE;

		
	}
	
	
}
