package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.types.ResultType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ExternalResultInfo extends ResultInfo {

	private final String name;
	private final ResultType type;
	private final String description;
	private final Printer printer;

	public ExternalResultInfo(String name, ResultType type, PrintSettings settings) {
		super(Collections.emptySet(), settings);
		this.name = name;
		this.type = type;
		this.description = null;
		this.printer = settings.getPrinterFactory().printerFor(type, settings);
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
