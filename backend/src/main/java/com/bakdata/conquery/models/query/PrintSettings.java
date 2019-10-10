package com.bakdata.conquery.models.query;

import java.util.Objects;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;

import groovy.lang.GroovyShell;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @AllArgsConstructor @NoArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {
	private final GroovyShell groovyShell = new GroovyShell(new CompilerConfiguration());
	private String columnNamerScript = null;

	@Override
	public String columnName(SelectResultInfo info) {
		if(columnNamerScript != null) {
			groovyShell.setProperty("columnInfo", info);
			Object result = groovyShell.evaluate(columnNamerScript);
			return Objects.toString(result);
		}
		else {
			return info.getSelect().getId().toStringWithoutDataset();
		}
	}
	
	public boolean isPrettyPrint() {
		return columnNamerScript != null;
	}
}
