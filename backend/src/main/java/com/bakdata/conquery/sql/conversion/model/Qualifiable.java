package com.bakdata.conquery.sql.conversion.model;

public interface Qualifiable<T> {

	T qualify(String qualifier);

}
