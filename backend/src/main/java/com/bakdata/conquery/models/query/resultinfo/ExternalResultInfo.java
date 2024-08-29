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


	public ExternalResultInfo(String name, ResultType type) {
		super(Collections.emptySet());
		this.name = name;
		this.type = type;
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return null;
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return name;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public Printer createPrinter(PrintSettings printSettings) {
		return printSettings.getPrinterFactory().printerFor(type, printSettings);
	}
}
