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


}