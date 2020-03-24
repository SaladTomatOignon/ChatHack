package fr.umlv.chathack.server.frames;

import java.nio.ByteBuffer;

import fr.umlv.chathack.server.core.Context;

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
	public void accept(Context ctx) {
		switch ( responceCode ) {
			case 0 : System.out.println("Connexion acceptée par le serveur"); break;
			case 1 : System.out.println("Connexion refusée par le serveur : Identifiants invalides"); break;
			case 2 : System.out.println("Connexion refusée par le serveur : Login déjà existant"); break;
		}
	}

	
	
	
	
}
