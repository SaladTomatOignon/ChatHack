package fr.umlv.chathack.resources.frames;

public abstract class AbstractFrame implements Frame {
	private static final long START_TIME = System.nanoTime();
	private final long creationTime;

	public AbstractFrame() {
		creationTime = System.nanoTime() - START_TIME;
	}
	
	@Override
	public long getCreationTime() {
		return creationTime;
	}
}
