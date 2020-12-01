package com.bakdata.conquery.models.events.stores.types.specific.string;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.types.ColumnStore;
import com.bakdata.conquery.models.events.stores.types.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Every implementation must guarantee IDs between 0 and size.
 *
 * Abstract
 */
public abstract class StringType extends ColumnStore<Integer> implements Iterable<String> {

	public StringType() {
		super(MajorTypeId.STRING);
	}

	@Override
	public abstract StringType select(int[] starts, int[] length) ;

	public abstract String getElement(int id);
	
	public abstract int size();

	public abstract int getId(String value);
	
	@JsonIgnore
	public abstract Dictionary getUnderlyingDictionary();

	public abstract void setUnderlyingDictionary(DictionaryId newDict);

	public abstract void setIndexStore(ColumnStore<Long> newType);

	public void loadDictionaries(NamespacedStorage storage) {}
}
