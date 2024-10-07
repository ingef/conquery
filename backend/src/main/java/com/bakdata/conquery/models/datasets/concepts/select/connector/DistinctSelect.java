package com.bakdata.conquery.models.datasets.concepts.select.connector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.index.InternToExternMapper;
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

		return new MappedPrinter(getMapping().resolve())
				.andThen(printerFactory.getListPrinter(printerFactory.getStringPrinter(printSettings), printSettings));
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT<>(super.getResultType());
	}

	/**
	 * Ensures that mapped values are still distinct.
	 */
	private record MultiMappedDistinctPrinter(InternToExternMapper mapper) implements Printer<Collection<String>> {

		@Override
		public Collection<String> apply(Collection<String> values) {
			final Set<String> out = new HashSet<>();

			for (String value : values) {
				out.addAll(mapper.externalMultiple(value));
			}

			return out;
		}
	}
}
