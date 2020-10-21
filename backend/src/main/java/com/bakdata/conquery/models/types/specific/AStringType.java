package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**every implementation must guarantee IDs between 0 and size**/
public abstract class AStringType<JAVA_TYPE> extends CType<Integer, JAVA_TYPE> implements Iterable<String> {

	public AStringType() {
		super(MajorTypeId.STRING);
	}

	public abstract String getElement(int id);
	
	public abstract int size();

	public abstract int getId(String value);
	
	@JsonIgnore
	public abstract Dictionary getUnderlyingDictionary();

	public abstract void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType);
}
