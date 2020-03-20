package fr.umlv.chathack.server.frames;

public class ConnectionFrame implements Frame {
	private String name;
	private String pass;
	private boolean passNeed;

	public ConnectionFrame(String name, String pass, boolean passNeed) {
		this.name = name;
		this.pass = pass;
		this.passNeed = passNeed;
	}

	public ConnectionFrame(String name) {
		this(name, "", false);
	}

	@Override
	public void accept() {
		System.out.println("name : " + name + ", pass : " + pass + " passNeed : " + passNeed);
	}

}
