package com.bakdata.conquery.util.dict;

import lombok.Getter;

public class BytesValueNode extends BytesNode implements ValueNode {

	@Getter(onMethod_=@Override)
	private int value;

	public BytesValueNode(byte key, int value) {
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
	public ValueNode getThisAsValuesNode() {
		return this;
	}
	
	@Override
	public String toString() {
		return super.toString()+" -> "+value;
	}
}