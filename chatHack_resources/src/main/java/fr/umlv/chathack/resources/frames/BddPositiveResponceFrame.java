package fr.umlv.chathack.resources.frames;


public class BddPositiveResponceFrame extends AbstractFrame {
	private long id;

	public BddPositiveResponceFrame(long id) {
		this.id = id;
	}

	@Override
	public void accept(ServerVisitor server) {
		server.answerFromDatabase(id, true);
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
