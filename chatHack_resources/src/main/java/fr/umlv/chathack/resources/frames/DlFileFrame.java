package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DlFileFrame extends AbstractFrame {
	private int fileId;
	private int dataSize;
	private byte data[];
	
	
	public DlFileFrame(int fileId, int dataSize, byte[] data) {
		this.fileId = fileId;
		this.dataSize = dataSize;
		this.data = Arrays.copyOfRange(data, 0, dataSize);
	}
	
	
	
	@Override
	public void accept(ClientVisitor client) {
		client.downloadFile(fileId, data);
	}

	@Override
	public byte[] getBytes() {
		var bb = ByteBuffer.allocate(2048);

		bb.put((byte) 7);

		bb.putInt(fileId);
		
		bb.putInt(dataSize);
		
		bb.put(data);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		return dataSize + 1 + 4 + 4;
	}
	
		
}
