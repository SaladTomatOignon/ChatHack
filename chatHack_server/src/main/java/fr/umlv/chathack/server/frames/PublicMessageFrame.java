package fr.umlv.chathack.server.frames;

public class PublicMessageFrame implements Frame {
	
	private String message;
	
	public PublicMessageFrame(String message) {
		this.message = message;
	}

	@Override
	public void accept() {
		System.out.println(message);
	}

}
