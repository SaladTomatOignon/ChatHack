package fr.umlv.chathack.resources.frames;

public class BddNegativeResponceFrame implements Frame{
	private long id;

	public BddNegativeResponceFrame(long id) {
		this.id = id;
	}

	@Override
	public void accept(ServerVisitor server) {
		// TODO
	}

	@Override
	public byte[] getBytes() {
		throw new IllegalStateException("this methos should never be called");
	}

	@Override
	public int size() {
		return Long.BYTES + 1;
		

	}
}
