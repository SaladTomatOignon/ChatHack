package fr.umlv.chathack.resources.readers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import fr.umlv.chathack.resources.frames.PrivateAnswerFrame;


public class PrivateAnswerReader implements Reader {
	private enum State {
		DONE, WAITING_RESPONCE_CODE, WAITING_NAME, WAITING_IP_TYPE, WAITING_IP, WAITING_PORT,
		WAITING_ID, ERROR
	}

	private byte responceCode;
	private String name;
	private byte ipType;
	private byte[] ipV6 = new byte[16];
	private byte[] ipV4 = new byte[4];
	private int port;
	private int id;

	private final ByteBuffer bb;
	private State state = State.WAITING_RESPONCE_CODE;

	private StringReader strReader;

	public PrivateAnswerReader(ByteBuffer bb) {
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
		case WAITING_RESPONCE_CODE:
			if (bb.remaining() >= Byte.BYTES) {
				responceCode = bb.get();
				state = State.WAITING_NAME;
			} else {
				return ProcessStatus.REFILL;
			}

		case WAITING_NAME:
			status = strReader.process();
			if (status == ProcessStatus.DONE) {
				name = (String) strReader.get();
				if (responceCode != 0) {
					state = State.DONE;
					return ProcessStatus.DONE;
				}
				state = State.WAITING_IP_TYPE;

			} else {
				return status;
			}
		case WAITING_IP_TYPE:
			if (bb.remaining() >= Byte.BYTES) {
				ipType = bb.get();
				state = State.WAITING_IP;
			} else {
				return ProcessStatus.REFILL;
			}
		case WAITING_IP:
			if (bb.remaining() >= 4 && ipType == 0) {
				bb.get(ipV4);
				state = State.WAITING_PORT;
			} else {
				if (bb.remaining() >= 16 && ipType == 1) {
					bb.get(ipV6);
					state = State.WAITING_PORT;
				}else {
					return ProcessStatus.REFILL;
				}
				
			}
		case WAITING_PORT:
			if (bb.remaining() >= Integer.BYTES) {
				port = bb.getInt();
				state = State.WAITING_ID;
			} else {
				return ProcessStatus.REFILL;
			}
		case WAITING_ID:
			if (bb.remaining() >= Integer.BYTES) {
				id = bb.getInt();
				state = State.DONE;
				return ProcessStatus.DONE;
			} else {
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
		if (responceCode == 1) {
			return new PrivateAnswerFrame(responceCode, name);
		}
		try {
			if (ipType == 0) {
				return new PrivateAnswerFrame(responceCode, name, InetAddress.getByAddress(ipV4), port, id);

			}
			return new PrivateAnswerFrame(responceCode, name, InetAddress.getByAddress(ipV6), port, id);

		} catch (UnknownHostException e) {
			// TODO 
			System.err.println("Invalid address");
		}
		return null; //normalement ca arrive jamais là
	}

	@Override
	public void reset() {
		strReader.reset();
		state = State.WAITING_RESPONCE_CODE;
	}
	

}
