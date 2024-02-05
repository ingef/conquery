package com.bakdata.conquery.sql.conversion.model;

import javax.annotation.Nullable;

/**
 * A CteStep represents a common table expression.
 */
public interface CteStep {

	String getSuffix();

	default String cteName(String nodeLabel) {
		return "%s-%s".formatted(nodeLabel, getSuffix());
	}

	@Nullable
	default CteStep getPredecessor() {
		return null;
	}

}
