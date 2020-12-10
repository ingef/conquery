package com.bakdata.conquery.util.dict;

import static com.bakdata.conquery.util.dict.TTDirection.LEFT;
import static com.bakdata.conquery.util.dict.TTDirection.MIDDLE;
import static com.bakdata.conquery.util.dict.TTDirection.RIGHT;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BytesPatriciaNode extends ABytesNode {

	@Getter
	protected final byte[] key;
	
	@Override
	public int get(byte[] k, int i) {
		byte c = k[i];
		byte first = key[0];
		
		if(c<first) {
			if(getLeft()==null) {
				return -1;
			}
			return getLeft().get(k, i);
		}
		else if(c>first) {
			if(getRight()==null) {
				return -1;
			}
			return getRight().get(k, i);
		}
		else {
			int matchingComponents = 1;
			boolean stillMatches = true;
			while(stillMatches) {
				int pos = matchingComponents+i;
				if(pos<k.length
					&& matchingComponents<key.length
					&& k[pos] == key[matchingComponents]
				) {
					matchingComponents++;
				}
				else {
					stillMatches = false;
				}
			}
			
			if(matchingComponents == key.length) {
				if(i+matchingComponents==k.length) {
					return getValue();
				}
				if(getMiddle()==null) {
					return -1;
				}
				return getMiddle().get(k, i+matchingComponents);
			}
			return -1;
		}
	}
	
	@Override
	public ValueNode getNode(byte[] k, int i) {
		byte c = k[i];
		byte first = key[0];
		
		if(c<first) {
			if(getLeft()==null) {
				return null;
			}
			return getLeft().getNode(k, i);
		}
		else if(c>first) {
			if(getRight()==null) {
				return null;
			}
			return getRight().getNode(k, i);
		}
		else {
			int matchingComponents = 1;
			boolean stillMatches = true;
			while(stillMatches) {
				int pos = matchingComponents+i;
				if (pos < k.length && matchingComponents < key.length && k[pos] == key[matchingComponents]) {
					matchingComponents++;
				}
				else {
					stillMatches = false;
				}
			}
			
			if(matchingComponents == key.length) {
				if(i+matchingComponents==k.length) {
					return getThisAsValueNode();
				}
				if(getMiddle()==null) {
					return null;
				}
				return getMiddle().getNode(k, i+matchingComponents);
			}
			return null;
		}
	}

	@Override
	public ValueNode getNearestNode(byte[] k, int i, ValueNode bestCandidate) {
		byte c = k[i];
		byte first = key[0];

		if(c<first) {
			if(getLeft()==null) {
				return bestCandidate;
			}
			return getLeft().getNearestNode(k, i, bestCandidate);
		}
		else if(c>first) {
			if(getRight()==null) {
				return bestCandidate;
			}
			return getRight().getNearestNode(k, i, bestCandidate);
		}
		else {
			int matchingComponents = 1;
			boolean stillMatches = true;
			while(stillMatches) {
				int pos = matchingComponents+i;
				if(pos<k.length
						&& matchingComponents<key.length
						&& k[pos] == key[matchingComponents]
						) {
					matchingComponents++;
				}
				else {
					stillMatches = false;
				}
			}

			if(matchingComponents == key.length) {
				if(i+matchingComponents==k.length) {
					return getThisAsValueNode();
				}
				if(getMiddle()==null) {
					return getThisAsValueNode();
				}
				return getMiddle().getNearestNode(k, i+matchingComponents, getThisAsValueNode());
			}
			return bestCandidate;
		}
	}

	@Override
	public int put(BytesTTMap map, NodeParent<ABytesNode> parent, TTDirection direction, byte[] k, int i, int value) {
		byte c = k[i];
		byte first = key[0];
		int result = -1;
		
		if(c<first) {
			if(getLeft()==null) {
				setLeft(TTHelper.createBytesValueNode(map, Arrays.copyOfRange(k, i, k.length), value));
			}
			else {
				result = getLeft().put(map, this, LEFT, k, i, value);
			}
		}
		else if(c>first) {
			if(getRight()==null) {
				setRight(TTHelper.createBytesValueNode(map, Arrays.copyOfRange(k, i, k.length), value));
			}
			else {
				result = getRight().put(map, this, RIGHT, k, i, value);
			}
		}
		else {
			int matchingComponents = 1;
			boolean stillMatches = true;
			while(stillMatches) {
				int pos = matchingComponents+i;
				if(pos<k.length
					&& matchingComponents<key.length
					&& k[pos] == key[matchingComponents]
				) {
					matchingComponents++;
				}
				else {
					stillMatches = false;
				}
			}
			
			if(matchingComponents == key.length) {
				if(i+matchingComponents==k.length) {
					result = setValue(map, parent, direction, value);
				}
				else {
					if(getMiddle()==null) {
						setMiddle(TTHelper.createBytesValueNode(map, Arrays.copyOfRange(k, i+matchingComponents, k.length), value));
					}
					else {
						result = getMiddle().put(map, this, MIDDLE, k, i+matchingComponents, value);
					}
				}
			}
			else {
				ABytesNode rep = TTHelper.createBytesNode(Arrays.copyOfRange(key, 0, matchingComponents));
				rep.setLeft(getLeft());
				rep.setRight(getRight());
				
				ABytesNode rep2 = createSplit(map, Arrays.copyOfRange(key, matchingComponents, key.length));
				rep2.setMiddle(getMiddle());
				rep.setMiddle(rep2);
				
				parent.replace(this, direction, rep);

				result = rep.put(map, parent, direction, k, i, value);
			}
		}
		
		added(result);
		return result;
	}
	
	public ValueNode getThisAsValueNode() {
		return null;
	}
	
	public int getValue() {
		return -1;
	}
	
	protected int setValue(BytesTTMap map, NodeParent<ABytesNode> parent, TTDirection direction, int value) {
		BytesPatriciaValueNode rep = new BytesPatriciaValueNode(key, value);
		rep.copyChildrenFrom(this);
		parent.replace(this, direction, rep);
		map.setEntry(rep, value);
		return -1;
	}
	
	protected ABytesNode createSplit(BytesTTMap map, byte[] splitKey) {
		return TTHelper.createBytesNode(splitKey);
	}
	
	@Override
	protected int ownValue() {
		return 0;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(key);
	}

	@Override
	public byte[] key() {
		return key;
	}
}