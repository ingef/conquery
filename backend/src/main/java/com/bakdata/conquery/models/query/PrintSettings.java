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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @RequiredArgsConstructor @AllArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {
	public static final String GROOVY_VARIABLE = "columnInfo";
	/**
	 * Non static shell because thread safety is not given.
	 */
	private final GroovyShell groovyShell = new GroovyShell(new CompilerConfiguration());

	private final boolean prettyPrint;
	/**
	 * Assuming the Script has already been validated (from loading the config)
	 */
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
			if(result != null) {				
				return Objects.toString(result);
			}
			log.info("The column namer script returned null: {}\nFalling back to standard format",columnNamerScript);
		}
		
		// Use the standard procedure
		return String.format("%s %s",columnInfo.getCqConcept().getLabel(),columnInfo.getSelect().getLabel());
		
	}
}
