package fr.umlv.chathack.resources.frames;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class InitSendFileFrame implements Frame{
	
	private String fileName;
	private int fileSize;
	private int fileId;

	

	public InitSendFileFrame(String fileName, int fileSize, int fileId) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileId = fileId;
	}

	@Override
	public void accept(ClientVisitor client) {
		System.out.println("fileName : " + fileName  + ", fileSize : " + fileSize + ", fileId : " + fileId);
	}

	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var fileNameEncode = cs.encode(fileName);

		bb.put((byte) 6);

		bb.putInt(fileNameEncode.remaining());
		bb.put(fileNameEncode);
		
		bb.putInt(fileSize);
		bb.putInt(fileId);

		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		
		return cs.encode(fileName).remaining() + 1 + 4 + 4 + 4;
		

	}

}
