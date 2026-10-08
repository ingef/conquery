package com.bakdata.conquery.sql.conversion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter;
import com.bakdata.conquery.sql.model.export.ResolvedExportTable;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;

/** Resolves backend table-export DTOs into the connector-owned export model. */
public final class ResolvedTableExportMapper {

	private final ResolvedQueryMapper queryMapper;
	private final Clock clock;
	private final String defaultPrimaryColumn;

	public ResolvedTableExportMapper(ResolvedQueryMapper queryMapper, Clock clock, String defaultPrimaryColumn) {
		this.queryMapper = queryMapper;
		this.clock = clock;
		this.defaultPrimaryColumn = defaultPrimaryColumn;
	}

	public ResolvedTableExportQuery map(TableExportQuery query, CompilerDialect dialect) {
		if (!(query.getQuery() instanceof ConceptQuery prerequisite)) {
			throw new UnsupportedOperationException("Resolved table exports currently require a concept-query prerequisite");
		}
		DateRange restriction = new DateRange(Optional.ofNullable(query.getDateRange().getMin()),
				Optional.ofNullable(query.getDateRange().getMax()));
		LocalDate endDate = restriction.endInclusive().orElseGet(() -> LocalDate.now(clock));
		List<ResolvedExportTable> tables = query.getConcepts().stream()
				.flatMap(concept -> concept.getTables().stream().map(table -> table(
						concept, table, query.getPositions(), endDate)))
				.toList();
		return new ResolvedTableExportQuery(
				queryMapper.map(prerequisite, Optional.empty(), prerequisite.getResultInfos()),
				restriction,
				tables,
				ResolvedOperationAdapter.resultColumns(query.getResultInfos())
		);
	}

	private ResolvedExportTable table(CQConcept concept, CQTable table, Map<ColumnId, Integer> positions,
			LocalDate endDate) {
		com.bakdata.conquery.models.datasets.concepts.Connector connector = table.getConnector().resolve();
		SqlTable sqlTable = EntitySchemaAdapter.from(connector.getResolvedTable());
		org.jooq.Field<String> primary = TablePrimaryColumnUtil.findPrimaryColumn(
				connector.getResolvedTable(), defaultPrimaryColumn);
		ResolvedColumn primaryId = new ResolvedColumn(sqlTable.logicalId() + ".primary-id", sqlTable,
				primary.getName(), com.bakdata.conquery.models.datasets.ColumnType.STRING, false);
		int width = TableExportQuery.calculateWidth(positions) - 1;
		List<Optional<ResolvedColumn>> output = new ArrayList<>(java.util.Collections.nCopies(width, Optional.empty()));
		for (Column column : connector.getResolvedTable().getColumns()) {
			Integer position = positions.get(column.getId());
			if (position != null) {
				output.set(position - 1, Optional.of(EntitySchemaAdapter.from(column)));
			}
		}
		return new ResolvedExportTable(
				concept.userLabel(Locale.ROOT),
				connector.getName(),
				sqlTable,
				primaryId,
				ResolvedQueryMapper.validityDate(table.findValidityDate()),
				connector.getResolvedTable().getName(),
				output,
				table.getFilters().stream().map(filter -> ResolvedOperationAdapter.filter(filter, endDate)).toList()
		);
	}
}
