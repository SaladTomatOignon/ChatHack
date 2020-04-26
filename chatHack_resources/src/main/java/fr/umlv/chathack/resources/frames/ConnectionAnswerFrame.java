package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;

public class ConnectionAnswerFrame extends AbstractFrame {

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
		
		bb.put((byte) 8);
		
		bb.put((byte) responceCode);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	
	}

	@Override
	public void accept(ClientVisitor client) {
		switch ( responceCode ) {
			case 0 : System.out.println("Connection accepted by the server"); break;
			case 1 : System.out.println("Connection refused by the server : Invalid logins"); break;
			case 2 : System.out.println("Connection refused by the server : Login already existing"); break;
			case 3 : System.out.println("Connection refused by the server : The server does not allow new clients"); break;
		}
	}

	
	
	
	
}
