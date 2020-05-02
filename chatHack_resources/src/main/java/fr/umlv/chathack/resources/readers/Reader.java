package fr.umlv.chathack.resources.readers;

public interface Reader {
	
	/**
	 * Enum use in every reader. It is use to tell if the ByteBuffer need to be refill or not.
	 *
	 */
	public static enum ProcessStatus {
		DONE, REFILL, ERROR
	};
	
	/**
	 * This method is used to process a part of the ByteBuffer and extract the necessary data 
	 * 
	 * @return DONE if finish, REFILL if needed or ERROR if one occur 
	 */
	public ProcessStatus process();
	
	/**
	 * Get the data extract from the ByteBuffer.
	 * 
	 * @return The object corresponding of the reader 
	 */
	public Object get();
	
	/**
	 * Reset the state of the reader should only be called after the get or if an error occurs 
	 */
	public void reset();

}
