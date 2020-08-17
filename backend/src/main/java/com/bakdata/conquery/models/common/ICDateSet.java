package com.bakdata.conquery.models.common;

import java.time.LocalDate;
import java.util.Collection;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ICDateSet {
	
	Collection<CDateRange> asRanges();


	boolean contains(LocalDate value);

	boolean contains(int value);

	boolean isEmpty();

	void clear();

	void addAll(ICDateSet other);

	void addAll(Iterable<CDateRange> ranges);

	boolean intersects(CDateRange range);

	CDateRange span();

	void add(CDateRange rangeToAdd);

	void remove(CDateRange rangeToRemove);

	@Override
	String toString();

	@JsonIgnore
	boolean isAll();

	void retainAll(ICDateSet retained);

	void retainAll(CDateRange retained);

	Long countDays();

	int getMinValue();

	int getMaxValue();
}
