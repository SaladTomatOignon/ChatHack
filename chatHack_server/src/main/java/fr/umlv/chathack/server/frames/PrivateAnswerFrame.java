package fr.umlv.chathack.server.frames;

public class PrivateAnswerFrame implements Frame {
	private boolean connectionAccept;
	private String name;
	
	
	
	
	public PrivateAnswerFrame(boolean connectionAccept, String name) {
		this.connectionAccept = connectionAccept;
		this.name = name;
	}




	public void accept() {
		System.out.println("connectionAccept : " + connectionAccept + " name : " + name);
	}
	
	
}
