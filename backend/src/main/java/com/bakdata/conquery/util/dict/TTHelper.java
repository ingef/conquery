package com.bakdata.conquery.util.dict;

public final class TTHelper {
	
	private TTHelper() {}

	public static ABytesNode createBytesValueNode(BytesTTMap map, byte[] key, int value) {
		int len = key.length;
		ABytesNode n;
		if(len==1) {
			n = new BytesValueNode(key[0], value);
		}
		else if(len > 1) {
			n = new BytesPatriciaValueNode(key, value);
		}
		else {
			throw new IllegalStateException();
		}
		
		map.setEntry((ValueNode)n, value);
		return n;
	}
	
	public static  ABytesNode createBytesNode(byte[] key) {
		int len = key.length;
		if(len==1) {
			return new BytesNode(key[0]);
		}
		else if(len > 1) {
			return new BytesPatriciaNode(key);
		}
		else {
			throw new IllegalStateException();
		}
	}


	public static byte[] concat(byte a, byte[] b) {
		byte[] res = new byte[1+b.length];
		res[0] = a;
		System.arraycopy(b, 0, res, 1, b.length);
		return res;
	}
	
	public static byte[] concat(byte[] a, byte b) {
		byte[] res = new byte[a.length+1];
		System.arraycopy(a, 0, res, 0, a.length);
		res[a.length] = b;
		return res;
	}
	
	public static byte[] concat(byte[] a, byte[] b) {
		byte[] res = new byte[a.length+b.length];
		System.arraycopy(a, 0, res, 0, a.length);
		System.arraycopy(b, 0, res, a.length, b.length);
		return res;
	}
}