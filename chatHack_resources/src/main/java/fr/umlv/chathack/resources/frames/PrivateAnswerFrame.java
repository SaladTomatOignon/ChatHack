package fr.umlv.chathack.resources.frames;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateAnswerFrame implements Frame {
	
	private byte responceCode;
	private String name;
	private byte ipType;
	private InetSocketAddress address;
	private int id;
	
	
	
	
	public PrivateAnswerFrame(byte responceCode, String name, InetAddress ip, int port, int id) {
        this.responceCode = responceCode;
        this.name = name;
        this.ipType = (byte) (ip instanceof Inet4Address ? 0 : 1);
        this.address = new InetSocketAddress(ip, port);
        this.id = id;
    }




	public PrivateAnswerFrame(byte connectionAccept, String name) {
		this.responceCode = connectionAccept;
		this.name = name;
	}




	public void accept() {
		System.out.println("connectionAccept : " + responceCode + " name : " + name);
	}
	
	@Override
	public byte[] getBytes() {
		var cs = StandardCharsets.UTF_8;
		var bb = ByteBuffer.allocate(1024);
		var nameEncode = cs.encode(name);
		
		bb.put((byte) 10 );
		
		bb.put(responceCode);
		
		bb.putInt(nameEncode.remaining());
		bb.put(nameEncode);
		
		if (responceCode == 0) {
			bb.put(ipType);
			bb.put(address.getAddress().getAddress());
			bb.putInt(address.getPort());
			bb.putInt(id);
		}
		
		bb.flip();
		byte[] arr = new byte[bb.remaining()];
		bb.get(arr);
		return arr;
	}
	
	@Override
	public int size() {
		var cs = StandardCharsets.UTF_8;
		if (responceCode == 0) {
			return 1 + 1 + 4 + cs.encode(name).remaining() + 1 + address.getAddress().getAddress().length + 4 + 4;
		}
		return cs.encode(name).remaining() + 2 + 4;
		

	}
	
	
	
}