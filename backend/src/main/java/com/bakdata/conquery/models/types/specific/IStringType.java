package com.bakdata.conquery.models.types.specific;

/**every implementation must guarantee IDs between 0 and size**/
public interface IStringType extends Iterable<String> {

	String getElement(int id);
	
	//int getStringId(String string);

	int size();

	int getId(String value);
}
