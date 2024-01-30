package com.bakdata.conquery.sql.conversion.model;

/**
 * A CteStep represents a common table expression.
 */
public interface CteStep {

	String cteName(String nodeLabel);
	CteStep predecessor();

}
