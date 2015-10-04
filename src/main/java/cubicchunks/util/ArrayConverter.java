package cubicchunks.util;

public class ArrayConverter {

	public static final byte[] toByteArray(char[] arr) {
		byte[] b = new byte[arr.length * 2];
		int i = 0;
		for(char c : arr) {
			b[i++] = (byte) (c&0xFF);
			b[i++] = (byte) (c >>> 8);
		}
		return b;
	}

	public static final char[] toCharArray(byte[] b) {
		if((b.length&1) != 0) {
			throw new IllegalArgumentException("Byte array length must be even number, but it's: " + b.length);
		}

		char[] arr = new char[b.length/2];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = (char) (b[i*2] | (b[i*2+1] << 8));
		}
		return arr;
	}
}
