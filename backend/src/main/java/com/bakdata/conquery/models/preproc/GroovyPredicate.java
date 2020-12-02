package com.bakdata.conquery.models.preproc;

import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;

/**
 * A condition that is a groovy script and thus able to represent everything.
 */
@Slf4j
public abstract class GroovyPredicate extends Script {

	public boolean filterRow(String[] row){
		setProperty("row", row);
		log.trace("Row = {}", row);
		return run();
	}

	@Override
	public abstract Boolean run();
}
