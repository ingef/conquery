package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ExternalResultInfo extends ResultInfo {

	private final String name;
	private final ResultType type;
	private final String description;
	private final ResultPrinters.Printer printer;

	public ExternalResultInfo(String name, ResultType type, PrintSettings settings) {
		this(name, type, null, ResultPrinters.defaultPrinter(type, settings), Collections.emptySet(), settings);
	}
	public ExternalResultInfo(String name, ResultType type, String description, ResultPrinters.Printer printer, Set<SemanticType> semantics, PrintSettings settings) {
		super(semantics, settings);
		this.name = name;
		this.type = type;
		this.description = description;
		this.printer = printer;
	}


	@Override
	public String userColumnName() {
		return null;
	}

	@Override
	public String defaultColumnName() {
		return name;
	}
}
