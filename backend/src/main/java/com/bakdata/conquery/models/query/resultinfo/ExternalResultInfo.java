package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
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
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		return printerFactory.printerFor(type, printSettings);
	}

	@Override
	public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
		if (type instanceof ResultType.ListT<?> list && list.getElementType().equals(ResultType.Primitive.STRING)) {
			return resultSetProcessor::getStringList;
		}

		return resultSetProcessor::getString;
	}
}
