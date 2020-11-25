package com.bakdata.conquery.models.types.specific.string;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_NUMBER")
public class StringTypeNumber extends StringType {

	@Nonnull
	protected CType<Long> delegate;
	//used as a compact intset
	private Range<Integer> range;

	// Only used for setting values in Preprocessing.
	@JsonIgnore
	private transient Map<Integer, String> dictionary;

	@JsonCreator
	public StringTypeNumber(Range<Integer> range, CType<Long> numberType) {
		super();
		this.range = range;
		this.delegate = numberType;
	}

	public StringTypeNumber(Range<Integer> range, CType<Long> numberType, Map<Integer, String> dictionary) {
		this(range, numberType);
		this.dictionary = dictionary;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return delegate.estimateMemoryFieldSize();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + delegate + "]";
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
	public Object createPrintValue(Integer value) {
		return value;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return value.toString();
	}

	@Override
	public String getElement(int id) {
		return Integer.toString(id);
	}

	@Override
	public int size() {
		return range.getMax() - range.getMin() + 1;
	}

	@Override
	public int getId(String value) {
		try {
			int result = Integer.parseInt(value);
			if (range.contains(result)) {
				return result;
			}
			return -1;
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {

	}

	@Override
	public StringTypeNumber select(int[] starts, int[] length) {
		return new StringTypeNumber(range, delegate.select(starts, length));
	}

	@Override
	public Integer get(int event) {
		return getDelegate().get(event).intValue();
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			getDelegate().set(event, null);
		}
		else {
			getDelegate().set(event, Long.valueOf(dictionary.get(value)));
		}
	}

	@Override
	public boolean has(int event) {
		return getDelegate().has(event);
	}
}
