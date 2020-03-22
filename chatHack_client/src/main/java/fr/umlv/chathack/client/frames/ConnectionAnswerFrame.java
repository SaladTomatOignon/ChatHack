package fr.umlv.chathack.client.frames;

import java.nio.ByteBuffer;

public class ConnectionAnswerFrame implements Frame{

	private byte responceCode;
	
	public ConnectionAnswerFrame(byte responceCode) {
		this.responceCode = responceCode;
	}

	@Override
	public int size() {
		return 2;
	}

	@Override
	public byte[] getBytes() {
		var bb = ByteBuffer.allocate(1024);
		
		bb.put((byte) 0);
		
		bb.put((byte) responceCode);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	
	}

	@Override
	public void accept() {
		System.out.println("ConnectionAnswerFrame : " + responceCode);
		
	}

	
	
	
	
}
