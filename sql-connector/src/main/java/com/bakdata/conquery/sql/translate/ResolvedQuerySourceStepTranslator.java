package com.bakdata.conquery.sql.translate;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.FinalQueryStepComposer;
import com.bakdata.conquery.sql.translate.query.ResolvedQueryStepTranslator;
import com.bakdata.conquery.sql.translate.query.interval.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingContext;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingCteStep;

/** Translates any resolved query source into a named relation suitable for use by another query. */
public final class ResolvedQuerySourceStepTranslator {

	private final ResolvedQueryStepTranslator queryTranslator = new ResolvedQueryStepTranslator();

	public QueryStep translate(ResolvedQuerySource source, TranslatorDialect dialect, SqlNameGenerator names) {
		return switch (source) {
			case ResolvedQuerySource.Standard standard -> translateStandard(standard, dialect, names);
			case ResolvedQuerySource.Form form -> FormSqlTranslator.translateStep(form.query(), dialect, names, this, true);
			case ResolvedQuerySource.TableExport export -> TableExportSqlTranslator.translateStep(
					export.query(), dialect, names, this, true);
			case ResolvedQuerySource.Union union -> translateUnion(union, dialect, names);
		};
	}

	private QueryStep translateStandard(
			ResolvedQuerySource.Standard source,
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		ResolvedQueryStepTranslator.TranslatedQuerySteps compiled = queryTranslator.translate(
				source.query(), dialect, names, Optional.empty());
		return FinalQueryStepComposer.composeReusable(
				compiled.step(), compiled.externalValues(), source.query().includeValidityDate(), dialect,
				names.reusableStepName("query_result"));
	}

	private QueryStep translateUnion(
			ResolvedQuerySource.Union source,
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		List<QueryStep> children = source.children().stream().map(child -> translate(child, dialect, names)).toList();
		String unionName = names.reusableStepName("query_union");
		List<QueryStep> projections = children.stream()
				.map(child -> projectUnionChild(child, source.includeValidityDate(), dialect, unionName))
				.toList();
		QueryStep union = QueryStep.createUnionStep(
				projections,
				unionName,
				children,
				false);
		if (!source.includeValidityDate()) {
			return union;
		}
		IntervalPackingContext context = IntervalPackingContext.builder()
				.ids(union.getSelects().getIds())
				.daterange(union.getSelects().getValidityDate().orElseThrow())
				.predecessor(Optional.of(union))
				.tables(IntervalPackingCteStep.createTables(union, dialect, names))
				.build();
		return AnsiSqlIntervalPacker.aggregateAsValidityDate(context);
	}

	private static QueryStep projectUnionChild(
			QueryStep child,
			boolean includeValidityDate,
			TranslatorDialect dialect,
			String unionName
	) {
		Selects childSelects = child.getQualifiedSelects();
		Optional<ColumnDateRange> validity = includeValidityDate
				? Optional.of(childSelects.getValidityDate().orElseGet(dialect::emptyDateRange)
					.asValidityDateRange(unionName))
				: Optional.empty();
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(childSelects.getIds().getPrimaryColumn()))
				.validityDate(validity)
				.build();
		return QueryStep.builder()
				.selects(selects)
				.fromTable(QueryStep.toTableLike(child.getCteName()))
				.build();
	}
}
