package com.bakdata.conquery.models.datasets.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.AllValuesAggregator;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MappedPrinter;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.select.DistinctSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DISTINCT", base = Select.class)
public class DistinctSelect extends MappableSingleColumnSelect {

	@JsonCreator
	public DistinctSelect(ColumnId column,
						  InternToExternMapperId mapping) {
		super(column, mapping);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new AllValuesAggregator<>(getColumn().resolve());
	}

	@Override
	public SelectConverter<DistinctSelect> createConverter() {
		return new DistinctSelectConverter();
	}

	@Override
	public Printer<?> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if(getMapping() == null){
			return super.createPrinter(printerFactory, printSettings);
		}

		return printerFactory.getListPrinter(new MappedPrinter(getMapping().resolve()), printSettings);
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT<>(super.getResultType());
	}
}
