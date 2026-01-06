package com.bakdata.conquery.models.query.resultinfo.printers.common;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;

/**
 * Simple wrapper to turn results into Lists.
 */
public class ListResultInfo extends ResultInfo {

	private final ResultInfo resultInfo;

	public ListResultInfo(ResultInfo resultInfo) {
		super(resultInfo.getSemantics());
		this.resultInfo = resultInfo;
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return resultInfo.userColumnName(printSettings);
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return resultInfo.defaultColumnName(printSettings);
	}

	@Override
	public ResultType getType() {
		return new ResultType.ListT<>(resultInfo.getType());
	}

	@Override
	public String getDescription() {
		return resultInfo.getDescription();
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		return printerFactory.getListPrinter(resultInfo.createPrinter(printerFactory, printSettings), printSettings);
	}
}
