package com.bakdata.conquery.models.query;

import java.util.Objects;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.google.common.base.Strings;

import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @AllArgsConstructor @NoArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {
	public static final String GROOVY_VARIABLE = "columnInfo";
	/**
	 * Non static shell because thread safety is not given.
	 */
	private final GroovyShell groovyShell = new GroovyShell(new CompilerConfiguration());
	private String columnNamerScript = null;

	/**
	 * Generates the name for a query result column. 
	 * The name is either determined by a configured script or by a standard procedure
	 */
	@Override
	public String columnName(SelectResultInfo columnInfo) {
		if(isPrettyPrint()) {
			// Use the provided script
			groovyShell.setProperty(GROOVY_VARIABLE, columnInfo);
			Object result = groovyShell.evaluate(columnNamerScript);
			return Objects.toString(result);
		}
		else {
			// Use the standard procedure
			return Objects.toString(columnInfo.getSelect().getId().toStringWithoutDataset());
		}
	}
	
	/**
	 * Determines if column names and values in the columns have a special formating.
	 * @return True if pretty print should be used.
	 */
	public boolean isPrettyPrint() {
		return !Strings.isNullOrEmpty(columnNamerScript);
	}
}
