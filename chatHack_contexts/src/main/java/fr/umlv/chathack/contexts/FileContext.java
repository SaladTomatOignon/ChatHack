package fr.umlv.chathack.contexts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class FileContext {
	private static final int BUFFER_SIZE = 512_000; // Buffer of 512 Ko

	private final int size;
	private int totalReceived;
	private final ByteBuffer data;
	private final ByteBuffer overageData;
	private final FileOutputStream outputStream;
	private State state;

	public enum State {
		REFILL, FULL
	}

	/**
	 * Constructor of a fileContext.</br>
	 * It contains informations about a file for download such as the file name, its
	 * size and its content in bytes.</br>
	 * The full content is not stored in this object, a buffer is filled up as data
	 * is received from the channel. Then when the buffer is full, the content is
	 * written in a file, and the file continues to fill until it reaches its size.
	 * 
	 * @param name The file name
	 * @param size The full size of the file
	 */
	public FileContext(int size, FileOutputStream outputStream) {
		if (size <= 0) {
			throw new IllegalArgumentException("The file size must be positive.");
		}

		this.size = size;
		this.totalReceived = 0;
		this.data = ByteBuffer.allocate(BUFFER_SIZE);
		this.overageData = ByteBuffer.allocate(BUFFER_SIZE);
		this.outputStream = Objects.requireNonNull(outputStream);
		this.state = State.REFILL;
	}

	/**
	 * Retrieve the state of the buffer. REFILL if the buffer can receive more data,
	 * and FULL if the buffer is full OR if all of the file data has been received.
	 * 
	 * @return The state of the buffer
	 */
	public State state() {
		if (totalReceived == size) {
			return State.FULL;
		}

		return state;
	}

	/**
	 * Fill the buffer with the given data.</br>
	 * Update the state of the main buffer : FULL if the buffer is full, REFILL if
	 * not.
	 * 
	 * @param data The data to add to the buffer.
	 * 
	 * @throws IllegalStateException If the buffer is already full.
	 */
	public void fillBuffer(byte[] data) throws IllegalStateException {
		if (state != State.REFILL) {
			throw new IllegalStateException("The file buffer can not receive data");
		}

		int remaining = this.data.remaining();
		/*
		 * If the buffer can not contain all of the data received, we put the
		 * "extra bytes" in another buffer until the main buffer is empty.
		 */
		if (data.length > remaining) {
			this.data.put(data, 0, remaining);
			this.overageData.put(data, remaining, data.length - remaining);
		} else {
			this.data.put(data);
		}

		state = this.data.hasRemaining() ? State.REFILL : State.FULL;
		totalReceived += data.length;
	}

	/**
	 * If the buffer is full, transfers the data in the stream corresponding to the
	 * file.
	 * 
	 * @return False if all of the content as been transfered in the file, True
	 *         otherwise.
	 * 
	 * @throws IllegalStateException If the buffer was not full.
	 * @throws IOException
	 */
	public boolean flush() throws IllegalStateException, IOException {
		if (state != State.FULL && totalReceived < size) {
			throw new IllegalStateException("The buffer is not full");
		}

		/*
		 * TODO Transférer le contenu de 'data' dans le fichier outputStream, Vider
		 * 'data', Transférer le contenu de 'overageData' dans 'data', Vider 'overage',
		 * 
		 * Renvoyer faux quand tout le contenu du fichier a été transféré.
		 */
		data.flip();
		byte[] arr = new byte[data.remaining()];
		data.get(arr);

		outputStream.write(arr);
		totalReceived += arr.length;

		data.clear();
		overageData.flip();
		data.put(overageData);
		overageData.clear();

		return size != totalReceived;
	}
}
