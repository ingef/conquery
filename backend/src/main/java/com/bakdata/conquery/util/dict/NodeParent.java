package com.bakdata.conquery.util.dict;

public abstract class NodeParent<NODE> {
	protected abstract void replace(NODE oldNode, TTDirection direction, NODE newNode);
}