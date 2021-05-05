package com.bakdata.conquery.models.identifiable.ids;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
//TODO is this class necessary?
public class IdIterator {
	@EqualsAndHashCode.Include
	private final List<String> data;
	private int index;
	
	public IdIterator(List<String> data) {
		this.data = data;
		this.index = data.size();
	}
	
	public List<String> getRemaining() {
		if(index <= 0) {
			throw new IllegalStateException("No more remaining values in "+data);
		}
		return data.subList(0, index);
	}
	
	public int remaining() {
		return index;
	}
	
	public String next() {
		return data.get(--index);
	}

	public IdIterator first(int i) {
		return new IdIterator(data.subList(0, i));
	}

	public void consumeAll() {
		index = 0;
	}

	public IdIterator splitOff(int n) {
		IdIterator result = new IdIterator(data.subList(index-n, index));
		index -= n;
		return result;
	}

	public void internNext() {
		data.set(index-1, data.get(index-1).intern());
	}
}
