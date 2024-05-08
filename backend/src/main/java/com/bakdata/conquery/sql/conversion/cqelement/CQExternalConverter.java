package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.google.common.base.Preconditions;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * TODO this is just a minimal implementation to make form conversion testcases work
 */
public class CQExternalConverter implements NodeConverter<CQExternal> {

	private static final String CQ_EXTERNAL_CTE_NAME = "external";

	@Override
	public Class<? extends CQExternal> getConversionClass() {
		return CQExternal.class;
	}

	@Override
	public ConversionContext convert(CQExternal external, ConversionContext context) {

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		List<QueryStep> unions = external.getValuesResolved()
										 .entrySet().stream()
										 .flatMap(entry -> createRowSelects(entry, functionProvider).stream())
										 .toList();

		Preconditions.checkArgument(!unions.isEmpty(), "Expecting at least 1 converted resolved row when converting a CQExternal");
		QueryStep externalStep = QueryStep.createUnionStep(unions, CQ_EXTERNAL_CTE_NAME, Collections.emptyList());
		return context.withQueryStep(externalStep);
	}

	/**
	 * For each entry, we need to create a SELECT statement of static values for each pid -> date set. For dialects that support date multiranges, 1 row per ID
	 * is sufficient. For other dialects there can be multiple rows with the same pid -> date range from the date set.
	 */
	private static List<QueryStep> createRowSelects(Map.Entry<String, CDateSet> entry, SqlFunctionProvider functionProvider) {

		Field<Object> primaryColumn = DSL.val(entry.getKey()).coerce(Object.class).as(SharedAliases.PRIMARY_COLUMN.getAlias());
		SqlIdColumns ids = new SqlIdColumns(primaryColumn);

		List<ColumnDateRange> validityDateEntries = functionProvider.forCDateSet(entry.getValue(), SharedAliases.DATES_COLUMN);
		return validityDateEntries.stream()
								  .map(validityDateEntry -> createRowSelect(ids, validityDateEntry, functionProvider))
								  .toList();
	}

	/**
	 * Creates a SELECT statement of static values for each pid -> date entry, like
	 * <pre>{@code select 1 as "pid", '[2021-01-01,2022-01-01)'::daterange as "date_range"}</pre>
	 */
	private static QueryStep createRowSelect(SqlIdColumns ids, ColumnDateRange validityDate, SqlFunctionProvider functionProvider) {

		Selects selects = Selects.builder()
								 .ids(ids)
								 .validityDate(Optional.ofNullable(validityDate))
								 .build();

		// not all SQL dialects can create a SELECT statement without a FROM clause,
		// so we ensure there is some no-op table to select the static values from
		Table<? extends Record> table = functionProvider.getNoOpTable();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(table)
						.build();
	}
}
