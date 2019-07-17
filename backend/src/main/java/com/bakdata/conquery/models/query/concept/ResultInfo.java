package com.bakdata.conquery.models.query.concept;

import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;

@AllArgsConstructor @Getter @Wither
public class ResultInfo {
	private final String name;
	private final ResultType type;
	/**
	 * Keeps track of the number of columns, that are generated with the same base name.
	 * Start with 0 for no other occurence.
	 */
	private final AtomicInteger sameNameOcurrences;
	/**
	 * Calculated index for this column. Should be {@code <= sameNameOcurrences}. If both are 0, the postfix can be omitted.
	 */
	private final int postfix;
	
	public String getUniqueName() {
		return (sameNameOcurrences != null && sameNameOcurrences.get() > 0) ? name + "_" + postfix : name;
	}
}
