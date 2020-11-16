package com.bakdata.conquery.models.types.specific.string;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**every implementation must guarantee IDs between 0 and size**/
// TODO remove Iterable implementationm and change to explicit accessor instead
public abstract class StringType extends CType<Integer, Integer> implements Iterable<String> {

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

	public abstract void setUnderlyingDictionary(Dictionary newDict);
}
