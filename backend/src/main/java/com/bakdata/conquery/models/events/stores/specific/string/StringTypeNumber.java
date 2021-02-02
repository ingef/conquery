package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The strings are only numbers and can therefore be used directly.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_NUMBER")
@ToString(of = "delegate")
public class StringTypeNumber implements StringStore {

	@Nonnull
	protected IntegerStore delegate;
	//used as a compact intset
	private Range<Integer> range;
	// Only used for setting values in Preprocessing.
	@JsonIgnore
	private transient Map<Integer, String> dictionary;
	public StringTypeNumber(Range<Integer> range, IntegerStore numberType, Map<Integer, String> dictionary) {
		this(range, numberType);
		this.dictionary = dictionary;
	}

	@JsonCreator
	public StringTypeNumber(Range<Integer> range, IntegerStore numberType) {
		super();
		this.range = range;
		this.delegate = numberType;
	}

	@Override
	public int getLines() {
		return delegate.getLines();
	}

	@Override
	public Object createScriptValue(int event) {
		return delegate.createScriptValue(event);
	}

	@Override
	public long estimateEventBits() {
		return delegate.estimateEventBits();
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
	public void setIndexStore(IntegerStore indexStore) {
	}

	@Override
	public StringTypeNumber select(int[] starts, int[] length) {
		return new StringTypeNumber(range, delegate.select(starts, length));
	}


	@Override
	public int getString(int event) {
		return (int) getDelegate().getInteger(event);
	}

	@Override
	public void setNull(int event) {
		getDelegate().setNull(event);
	}

	@Override
	public void setString(int event, int value) {
		getDelegate().setInteger(event, value);
	}

	@Override
	public boolean has(int event) {
		return getDelegate().has(event);
	}
}
