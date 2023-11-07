package com.bakdata.conquery.sql.conversion.model;

public interface CteStep {

	String cteName(String nodeLabel);
	CteStep predecessor();

}
