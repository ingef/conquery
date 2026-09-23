package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.sql.compiler.conversion.operation.ResolvedSelectConverter;
import com.bakdata.conquery.sql.compiler.conversion.operation.SelectConversionContext;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.SubstringRange;

/** Resolves application columns and ranges before invoking the shared select compiler. */
public final class ResolvedSelectAdapter {
	private static final ResolvedSelectConverter CONVERTER = new ResolvedSelectConverter();

	private ResolvedSelectAdapter() {
	}

	public static SelectConversionContext context(String name, SelectContext<?> context) {
		return context(name, context, List.of());
	}

	public static SelectConversionContext context(
			String name,
			SelectContext<?> context,
			List<SelectConversionContext.ConceptColumnSource> conceptColumnSources
	) {
		Map<String, String> conceptColumnTables = context.getTables() instanceof ConceptSqlTables tables
				? tables.getConnectorTables().stream()
						.filter(connectorTables -> connectorTables.getConnector() != null)
						.collect(Collectors.toMap(
								connectorTables -> connectorTables.getConnector().resolveTableId().getTable(),
								connectorTables -> connectorTables.cteName(ConceptCteStep.PREPROCESSING),
								(first, ignored) -> first
						))
				: Map.of();
		return new SelectConversionContext(context.getCompilerDialect(), context.getNameGenerator(), context.getTables(), context.getIds(),
				context.getValidityDate(), context.getNameGenerator().legacyOperationName(name), conceptColumnTables, conceptColumnSources);
	}

	public static ConnectorSqlSelects connectorSelect(ResolvedSelect select, String name, SelectContext<?> context) {
		return CONVERTER.convert(select, context(name, context));
	}

	public static ConceptSqlSelects conceptSelect(ResolvedSelect select, String name, SelectContext<?> context) {
		return CONVERTER.conceptSelect(select, context(name, context));
	}

	public static ConnectorSqlSelects connectorSelect(
			ResolvedSelect select,
			String name,
			SelectContext<?> context,
			List<SelectConversionContext.ConceptColumnSource> conceptColumnSources
	) {
		return CONVERTER.convert(select, context(name, context, conceptColumnSources));
	}

	public static ConceptSqlSelects conceptSelect(
			ResolvedSelect select,
			String name,
			SelectContext<?> context,
			List<SelectConversionContext.ConceptColumnSource> conceptColumnSources
	) {
		return CONVERTER.conceptSelect(select, context(name, context, conceptColumnSources));
	}

	static ConnectorSqlSelects values(MappableSingleColumnSelect select, BuiltInSelects.ValueOperation operation, SelectContext<?> context) {
		return CONVERTER.convert(new BuiltInSelects.Values(select.getName(), EntitySchemaAdapter.from(select.getColumn().resolve()),
				operation, substring(select.getSubstringRange())),
				new SelectConversionContext(context.getCompilerDialect(), context.getNameGenerator(), context.getTables(), context.getIds(),
						context.getValidityDate(), context.getNameGenerator().legacyOperationName(select.getName())));
	}

	public static Optional<SubstringRange> substring(Range.IntegerRange range) {
		if (range == null || range.isAll()) {
			return Optional.empty();
		}
		return Optional.of(new SubstringRange(range.getMin() == null ? 0 : range.getMin(), Optional.ofNullable(range.getMax())));
	}
}
