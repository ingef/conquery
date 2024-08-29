package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.AllValuesAggregator;
import com.bakdata.conquery.models.query.resultinfo.printers.MappedPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.DistinctSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DISTINCT", base = Select.class)
public class DistinctSelect extends MappableSingleColumnSelect {

	@JsonCreator
	public DistinctSelect(@NsIdRef Column column,
						  @NsIdRef InternToExternMapper mapping) {
		super(column, mapping);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new AllValuesAggregator<>(getColumn());
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(super.getResultType());
	}

	@Override
	public SelectConverter<DistinctSelect> createConverter() {
		return new DistinctSelectConverter();
	}

	@Override
	public Printer createPrinter(PrintSettings printSettings, PrinterFactory printerFactory) {
		if(getMapping() == null){
			return super.createPrinter(printSettings, printerFactory);
		}

		return printerFactory.getListPrinter(new MappedPrinter(getMapping()), printSettings);
	}
}
