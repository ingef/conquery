package com.bakdata.conquery.models.preproc;

import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;

/**
 * A condition that is a groovy script and thus able to represent everything.
 */
public abstract class GroovyPredicate extends Script {
	
	@Getter @Setter
	private String[] row;
	
	@Override
	public abstract Boolean run();
	
	@Override
	public Object getProperty(String property) {
		if("row".equals(property)) {
			return row;
		}
		return super.getProperty(property);
	}
}
