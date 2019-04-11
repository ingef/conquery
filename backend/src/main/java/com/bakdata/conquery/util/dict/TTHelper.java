package com.bakdata.conquery.util.dict;

import java.util.ArrayDeque;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;

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

	public static void write(Output output, ABytesNode root) {
		byte[] flagBytes = new byte[] {0};
		BitStore flags = Bits.asStore(flagBytes);
		
		ArrayDeque<ABytesNode> openList = new ArrayDeque<>();
		openList.add(root);
		while(!openList.isEmpty()) {
			flagBytes[0] = 0;
			ABytesNode n = openList.removeFirst();
			flags.setBit(0, n instanceof ValueNode);
			flags.setBit(1, n instanceof BytesPatriciaNode);
			flags.setBit(2, n.getLeft() != null);
			flags.setBit(3, n.getMiddle() != null);
			flags.setBit(4, n.getRight() != null);
			output.write(flagBytes[0]);
			
			if(n instanceof BytesPatriciaNode) {
				output.writeInt(n.key().length, true);
				output.writeBytes(n.key());
			}
			else {
				output.writeByte(((BytesNode)n).getKey());
			}
			if(n instanceof ValueNode) {
				output.writeInt(((ValueNode) n).getValue(), true);
			}
			
			if(n.getRight() != null) {
				openList.addFirst(n.getRight());
			}
			if(n.getMiddle() != null) {
				openList.addFirst(n.getMiddle());
			}
			if(n.getLeft() != null) {
				openList.addFirst(n.getLeft());
			}
		}
	}

	public static ABytesNode read(Input input) {
		BitStore flags = Bits.asStore(new byte[] {input.readByte()});

		ABytesNode n;
		if(flags.getBit(1)) {
			byte[] key = new byte[input.readInt(true)];
			input.readBytes(key);
			if(flags.getBit(0)) {
				int value = input.readInt(true);
				n = new BytesPatriciaValueNode(key, value);
			}
			else {
				n = new BytesPatriciaNode(key);
			}
		}
		else {
			byte key = input.readByte();
			if(flags.getBit(0)) {
				int value = input.readInt(true);
				n = new BytesValueNode(key, value);
			}
			else {
				n = new BytesNode(key);
			}
		}
		if(flags.getBit(2)) {
			n.setLeft(read(input));
		}
		if(flags.getBit(3)) {
			n.setMiddle(read(input));
		}
		if(flags.getBit(4)) {
			n.setRight(read(input));
		}
		
		return n;
	}
}