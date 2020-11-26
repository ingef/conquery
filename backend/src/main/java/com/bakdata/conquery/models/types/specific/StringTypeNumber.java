package com.bakdata.conquery.models.types.specific;

import java.util.Iterator;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_NUMBER")
public class StringTypeNumber extends AStringType<Number> {

	//used as a compact intset
	private Range<Integer> range;
	@Nonnull
	protected VarIntType numberType;
	
	@JsonCreator
	public StringTypeNumber(Range<Integer> range, VarIntType numberType) {
		super(numberType.getPrimitiveType());
		this.range = range;
		this.numberType = numberType;
	}
	
	@Override
	public boolean canStoreNull() {
		return numberType.canStoreNull();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return numberType.estimateMemoryBitWidth();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[numberType=" + numberType + "]";
	}

	@Override
	public Iterator<String> iterator() {
		return IntStream
			.rangeClosed(
				range.getMin(),
				range.getMax()
			)
			.mapToObj(Integer::toString)
			.iterator();
	}

	@Override
	public String getElement(int id) {
		return Integer.toString(id);
	}

	@Override
	public int size() {
		return range.getMax()
			- range.getMin()
			+ 1;
	}

	@Override
	public int getId(String value) {
		try {
			int result = Integer.parseInt(value);
			if(range.contains(result)) {
				return result;
			}
			return -1;
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		throw new UnsupportedOperationException();
	}
}
