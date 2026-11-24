package com.bakdata.conquery.sql.translate.ir;

/** An intermediate representation that can be rebound to a qualified SQL table or CTE name. */
public interface Qualifiable<T> {

	T qualify(String qualifier);
}
