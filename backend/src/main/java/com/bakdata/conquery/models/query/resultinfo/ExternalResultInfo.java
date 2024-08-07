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

	public ExternalResultInfo(String name, ResultType type) {
		this(name, type, null, ResultPrinters.defaultPrinter(type), Collections.emptySet());
	}
	public ExternalResultInfo(String name, ResultType type, String description, ResultPrinters.Printer printer, Set<SemanticType> semantics) {
		super(semantics);
		this.name = name;
		this.type = type;
		this.description = description;
		this.printer = printer;
	}


	@Override
	public String userColumnName(PrintSettings printSettings) {
		return null;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return name;
	}
}
