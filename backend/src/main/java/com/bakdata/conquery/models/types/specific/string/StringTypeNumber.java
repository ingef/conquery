package com.bakdata.conquery.models.types.specific.string;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_NUMBER")
@ToString(of = "delegate")
public class StringTypeNumber extends StringType {

	@Nonnull
	protected ColumnStore<Long> delegate;
	//used as a compact intset
	private Range<Integer> range;

	// Only used for setting values in Preprocessing.
	@JsonIgnore
	private transient Map<Integer, String> dictionary;

	@JsonCreator
	public StringTypeNumber(Range<Integer> range, ColumnStore<Long> numberType) {
		super();
		this.range = range;
		this.delegate = numberType;
	}

	public StringTypeNumber(Range<Integer> range, ColumnStore<Long> numberType, Map<Integer, String> dictionary) {
		this(range, numberType);
		this.dictionary = dictionary;
	}

	@Override
	public long estimateEventBytes() {
		return delegate.estimateEventBytes();
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
			return Integer.parseInt(value);
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
	public void setIndexStore(ColumnStore<Long> indexStore) {

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
