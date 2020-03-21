package fr.umlv.chathack.server.frames;

public class PrivateRequestFrame implements Frame{

	private String name;
	
	
	public PrivateRequestFrame(String name) {
		super();
		this.name = name;
	}


	@Override
	public void accept() {
		System.out.println(name);
	}

}
