package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	private final FilterConverterService filterConverterService;
	private final SelectConverterService selectConverterService;

	public CQConceptConverter(FilterConverterService filterConverterService, SelectConverterService selectConverterService) {
		this.filterConverterService = filterConverterService;
		this.selectConverterService = selectConverterService;
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept node, ConversionContext context) {
		if (node.getTables().size() > 1) {
			throw new IllegalArgumentException("More than 1 table in a concept is not yet supported");
		}


		CQTable table = node.getTables().get(0);

		SelectConditionStep<Record> query = context.getDslContext().select(getSelects(table, context))
												   .from(getTableName(table))
												   .where(getFilters(table, context));

		return context.withQuery(query);
	}

	private Set<Field<?>> getSelects(CQTable table, ConversionContext context) {
		// primary column of the table is always a field of the select statement
		Field<?> primaryColumn = DSL.field(getPrimaryColumnName(table));
		// filter out possible duplicates if primary column and a select field from conceptSelects match
		Set<Field<?>> selects = new LinkedHashSet<>();
		selects.add(primaryColumn);
		// all other fields for the select statement are obtained from the concepts selects
		selects.addAll(getConceptSelects(table, context));
		return selects;
	}

	private String getTableName(CQTable table) {
		return table.getConnector().getTable().getName();
	}

	private List<Condition> getFilters(CQTable table, ConversionContext context) {
		return table.getFilters().stream()
					.map(filter -> filterConverterService.convert(filter, context))
					.toList();
	}

	private String getPrimaryColumnName(CQTable table) {
		// TODO add primary column to table
		return "pid";
	}

	private List<Field<?>> getConceptSelects(CQTable table, ConversionContext context) {
		return table.getSelects().stream()
					.map(select -> selectConverterService.convert(select, context))
					.collect(Collectors.toList());
	}

}
