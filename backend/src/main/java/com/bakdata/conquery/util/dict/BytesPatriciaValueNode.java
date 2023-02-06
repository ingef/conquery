package com.bakdata.conquery.util.dict;

import lombok.Getter;

public class BytesPatriciaValueNode extends BytesPatriciaNode implements ValueNode {

	@Getter(onMethod_=@Override)
	private int value;

	public BytesPatriciaValueNode(byte[] key, int value) {
		super(key);
		this.value = value;
	}
	
	@Override
	protected int setValue(BytesTTMap map, NodeParent<ABytesNode> parent, TTDirection direction, int value) {
		int lastValue = this.value;
		this.value = value;
		return lastValue;
	}
	
	@Override
	protected ABytesNode createSplit(BytesTTMap map, byte[] splitKey) {
		return TTHelper.createBytesValueNode(map, splitKey, value);
	}

	@Override
	public ValueNode getThisAsValueNode() {
		return this;
	}
	
	@Override
	public String toString() {
		return super.toString()+" -> "+value;
	}
}