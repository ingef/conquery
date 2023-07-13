package com.bakdata.conquery.models.query.results;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface EntityResult {

	String getEntityId();
	/**
	 * Provides the number of columns this result contains.
	 */
	int columnCount();
	Stream<Object[]> streamValues();
	
	/**
	 * Returns a list of the computed result line for this entity on the query.
	 */
	List<Object[]> listResultLines();
	
	/**
	 * Allows to modify the underlying result directly. The return value of the line modifier is the new line.
	 * So the modifier can change the array without reallocation by return the reference it received. Alternative it can 
	 * allocate a new result line and return that reference instead. 
	 * @param lineModifier A modifier(-chain) for a result line.
	 */
	void modifyResultLinesInplace(UnaryOperator<Object[]> lineModifier);
}
