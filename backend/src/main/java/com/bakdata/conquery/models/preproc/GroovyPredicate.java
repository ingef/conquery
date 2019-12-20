package com.bakdata.conquery.models.preproc;

import groovy.lang.Script;

/**
 * A condition that is a groovy script and thus able to represent everything.
 */
public abstract class GroovyPredicate extends Script {

	public boolean filterRow(String[] row){
		setProperty("row", row);
		return run();
	}

	@Override
	public abstract Boolean run();
}
