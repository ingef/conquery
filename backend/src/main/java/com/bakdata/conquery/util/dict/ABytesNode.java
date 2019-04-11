package com.bakdata.conquery.util.dict;

public abstract class ABytesNode extends ANode<ABytesNode> {
	
	public abstract int put(BytesTTMap map, NodeParent<ABytesNode> parent, TTDirection direction, byte[] k, int i, int value);
	
	public abstract int get(byte[] k, int i);

	public abstract ValueNode getNode(byte[] k, int i);
	public abstract ValueNode getNearestNode(byte[] k, int i, ValueNode bestCandidate);

	public abstract byte[] key();
}