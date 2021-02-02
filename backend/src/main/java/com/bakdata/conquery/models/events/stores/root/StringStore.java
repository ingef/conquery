package com.bakdata.conquery.models.events.stores.root;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;

/**
 * Every implementation must guarantee IDs between 0 and size.
 *
 */
@NoArgsConstructor
public abstract class StringStore extends ColumnStore<Integer> implements Iterable<String> {


	public abstract int getString(int event);

	@Override
	public abstract StringStore doSelect(int[] starts, int[] length) ;

	public abstract String getElement(int id);
	
	public abstract int size();

	public abstract int getId(String value);
	
	@JsonIgnore
	public abstract Dictionary getUnderlyingDictionary();

	public abstract void setUnderlyingDictionary(DictionaryId newDict);

	/**
	 * After applying DictionaryMapping a new store might be needed.
	 */
	public abstract void setIndexStore(IntegerStore newType);

	public void loadDictionaries(NamespacedStorage storage) {}
}
