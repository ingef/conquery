package com.bakdata.conquery.models.query;

import java.util.Objects;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.google.common.base.Strings;

import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @AllArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {
	public static final String GROOVY_VARIABLE = "columnInfo";
	/**
	 * Non static shell because thread safety is not given.
	 */
	private final GroovyShell groovyShell = new GroovyShell(new CompilerConfiguration());

	private final boolean prettyPrint;
	private String columnNamerScript = null;
	

	/**
	 * Generates the name for a query result column. 
	 * The name is either determined by a configured script or by a standard procedure
	 */
	@Override
	public String columnName(SelectResultInfo columnInfo) {
		if(!Strings.isNullOrEmpty(columnNamerScript)) {
			// Use the provided script
			groovyShell.setProperty(GROOVY_VARIABLE, columnInfo);
			Object result = groovyShell.evaluate(columnNamerScript);
			return Objects.toString(result);
		}
		else {
			// Use the standard procedure
			return String.format("%s %s",columnInfo.getCqConcept().getLabel(),columnInfo.getSelect().getLabel());
		}
	}
}
