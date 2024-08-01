package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalResultInfo extends ResultInfo {

	private final String name;
	private final ResultType type;
	private final String description;
	private final Set<SemanticType> semantics;
	private final ResultPrinters.Printer printer;

	public ExternalResultInfo(String name, ResultType type) {
		this(name, type, null, Collections.emptySet(), ResultPrinters.defaultPrinter(type));
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
