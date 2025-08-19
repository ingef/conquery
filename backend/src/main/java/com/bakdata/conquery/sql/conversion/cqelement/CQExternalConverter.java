package com.bakdata.conquery.sql.conversion.cqelement;

import static com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider.SQL_UNIT_SEPARATOR;

import java.util.*;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class CQExternalConverter implements NodeConverter<CQExternal> {

	private static final String CQ_EXTERNAL_IDS_CTE_NAME = "external_ids";
	private static final String CQ_EXTERNAL_EXTRAS_CTE_NAME = "external_extra";
	private static final String UNDERSCORE = "_";
	private static final String WHITESPACE = " ";

	@Override
	public Class<? extends CQExternal> getConversionClass() {
		return CQExternal.class;
	}

	@Override
	public ConversionContext convert(CQExternal external, ConversionContext context) {
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		QueryStep externalIdsCte = createExternalIdsCte(external, functionProvider);
		ConversionContext withExternalIdCte = context.withQueryStep(externalIdsCte);
		if (!external.isWithExtras()) {
			return withExternalIdCte;
		}
		QueryStep externalExtrasCte = createExternalExtrasCte(external, functionProvider);
		return withExternalIdCte.withExternalExtras(externalExtrasCte);
	}

	private static QueryStep createExternalIdsCte(CQExternal external, SqlFunctionProvider functionProvider) {
		List<QueryStep> unions = new ArrayList<>();
		for (Map.Entry<String, CDateSet> entry : external.getValuesResolved().entrySet()) {
			List<QueryStep> rowSelects = createRowSelects(entry, functionProvider);
			unions.addAll(rowSelects);
		}
		Preconditions.checkArgument(!unions.isEmpty(), "Expecting at least 1 converted resolved row when converting a CQExternal");
		return QueryStep.createUnionAllStep(unions, CQ_EXTERNAL_IDS_CTE_NAME, Collections.emptyList());
	}

	private QueryStep createExternalExtrasCte(CQExternal external, SqlFunctionProvider functionProvider) {
		List<QueryStep> unions = new ArrayList<>();
		for (Map.Entry<String, CDateSet> entry : external.getValuesResolved().entrySet()) {
			List<Map.Entry<String, List<String>>> extrasForId = external.getExtrasForId(entry.getKey());
			QueryStep rowSelects = createRowSelects(entry, extrasForId, functionProvider);
			unions.add(rowSelects);
		}
		Preconditions.checkArgument(!unions.isEmpty(), "Expecting at least 1 converted resolved row when converting a CQExternal");
		return QueryStep.createUnionAllStep(unions, CQ_EXTERNAL_EXTRAS_CTE_NAME, Collections.emptyList());
	}

	/**
	 * For each entry, we need to create a SELECT statement of static values for each pid -> date set. For dialects that support date multiranges,
	 * 1 row per ID is sufficient. For other dialects there can be multiple rows with the same pid -> date range from the date set.
	 */
	private static List<QueryStep> createRowSelects(Map.Entry<String, CDateSet> entry, SqlFunctionProvider functionProvider) {
		SqlIdColumns ids = createIdSelect(entry);
		List<ColumnDateRange> validityDateEntries = functionProvider.forCDateSet(entry.getValue(), SharedAliases.DATES_COLUMN);
		return validityDateEntries.stream()
								  .map(validityDateEntry -> createIdRowSelect(ids, validityDateEntry, functionProvider))
								  .toList();
	}

	/**
	 * For each entry, we need to create a SELECT statement of static values for each pid -> extras.
	 */
	private QueryStep createRowSelects(
			Map.Entry<String, CDateSet> entry,
			List<Map.Entry<String, List<String>>> extra,
			SqlFunctionProvider functionProvider
	) {
		SqlIdColumns ids = createIdSelect(entry);
		List<SqlSelect> extraSelects = extra.stream().map(CQExternalConverter::createExtraColumnValue).collect(Collectors.toList());
		return createExtraRowSelect(ids, extraSelects, functionProvider);
	}

	private static SqlIdColumns createIdSelect(Map.Entry<String, CDateSet> entry) {
		Field<Object> primaryColumn = DSL.val(entry.getKey()).coerce(Object.class).as(SharedAliases.PRIMARY_COLUMN.getAlias());
		return new SqlIdColumns(primaryColumn);
	}

	private static FieldWrapper<?> createExtraColumnValue(Map.Entry<String, List<String>> extraEntry) {
		String extraValues = extraEntry.getValue().stream()
									   .map(DSL::val)
									   .map(Field::toString)
									   .collect(Collectors.joining(SQL_UNIT_SEPARATOR));
		final Name alias = DSL.name(extraEntry.getKey().replace(WHITESPACE, UNDERSCORE));
		final Field<?> withAlias = DSL.field(extraValues).as(alias);
		return new FieldWrapper<>(withAlias);
	}

	/**
	 * Creates a SELECT statement of static values for each pid -> date entry, like
	 * <pre>{@code select 1 as "pid", '[2021-01-01,2022-01-01)'::daterange as "date_range"}</pre>
	 */
	private static QueryStep createIdRowSelect(
			SqlIdColumns ids,
			ColumnDateRange validityDate,
			SqlFunctionProvider functionProvider
	) {
		Selects selects = Selects.builder()
								 .ids(ids)
								 .validityDate(Optional.ofNullable(validityDate))
								 .build();
		return wrapInQueryStep(selects, functionProvider);
	}

	/**
	 * Creates a SELECT statement of static values for each pid -> extras, like
	 * <pre>{@code select 1 as "pid", '1' as "payload_1"}</pre>
	 */
	private static QueryStep createExtraRowSelect(
			SqlIdColumns ids,
			List<SqlSelect> extraSelects,
			SqlFunctionProvider functionProvider
	) {
		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelects(extraSelects)
								 .build();
		return wrapInQueryStep(selects, functionProvider);
	}

	private static QueryStep wrapInQueryStep(Selects selects, SqlFunctionProvider functionProvider) {

		// not all SQL dialects can create a SELECT statement without a FROM clause,
		// so we ensure there is some no-op table to select the static values from
		Table<? extends Record> table = functionProvider.getNoOpTable();

		return QueryStep.builder()
						.selects(selects)
						.fromTable(table)
						.build();
	}

}
