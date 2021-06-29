package com.bakdata.conquery.apiv1.query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.query.concept.filter.CQUnfilteredTable;
import com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefKeys;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan;
import com.bakdata.conquery.models.query.queryplan.TableExportQueryPlan.TableExportDescription;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * A TABLE_EXPORT creates a full export of the given tables. It ignores selects completely.
 */
@Slf4j
@Getter
@Setter
@CPSType(id = "TABLE_EXPORT", base = QueryDescription.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class TableExportQuery extends Query {

	@Valid
	@NotNull
	@NonNull
	protected Query query;
	@NotNull
	private Range<LocalDate> dateRange = Range.all();
	@NotEmpty
	@Valid
	private List<CQUnfilteredTable> tables;

	@InternalOnly
	@NsIdRefKeys
	private Map<Column, Integer> positions;

	@JsonIgnore
	private ResultInfo[] resultInfos = null;

	@Override
	public TableExportQueryPlan createQueryPlan(QueryPlanContext context) {
		List<TableExportDescription> resolvedConnectors = new ArrayList<>();

		for (CQUnfilteredTable table : tables) {
			Connector connector = table.getTable();

			// if no dateColumn is provided, we use the default instead which is always the first one.
			// Set to null if none-available in the connector.
			final Column validityDateColumn = findValidityDateColumn(connector, table.getDateColumn());

			final TableExportDescription exportDescription = new TableExportDescription(
					connector.getTable(),
					validityDateColumn
			);

			resolvedConnectors.add(exportDescription);
		}

		return new TableExportQueryPlan(
				query.createQueryPlan(context),
				CDateRange.of(dateRange),
				resolvedConnectors,
				positions
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);

		AtomicInteger currentPosition = new AtomicInteger(1); // First is dates

		positions = new HashMap<>();

		Map<SecondaryIdDescription, Integer> secondaryIdPositions = new HashMap<>();

		// SecondaryIds are pulled to the front and grouped over all tables
		tables.stream()
			  .map(cqUnfilteredTable -> cqUnfilteredTable.getTable().getTable().getColumns())
			  .flatMap(Arrays::stream)
			  .map(Column::getSecondaryId)
			  .filter(Objects::nonNull)
			  .distinct()
			  .sorted(Comparator.comparing(SecondaryIdDescription::getLabel))
			  // First position is dates, so
			  .forEach(secondaryId -> secondaryIdPositions.put(secondaryId, currentPosition.getAndIncrement()));


		for (CQUnfilteredTable table : tables) {
			Connector connector = table.getTable();
			final Column validityDateColumn = findValidityDateColumn(connector, table.getDateColumn());

			if (validityDateColumn != null) {
				positions.putIfAbsent(validityDateColumn, 0);
			}

			// Set column positions, set SecondaryId positions to precomputed ones.
			for (Column column : connector.getTable().getColumns()) {
				positions.computeIfAbsent(column, col -> col.getSecondaryId() != null
														  ? secondaryIdPositions.get(col.getSecondaryId())
														  : currentPosition.getAndIncrement());
			}
		}

		resultInfos = createResultInfos(currentPosition.get(), secondaryIdPositions, positions);
	}

	private static ResultInfo[] createResultInfos(int size, Map<SecondaryIdDescription, Integer> secondaryIdPositions, Map<Column, Integer> positions) {

		ResultInfo[] infos = new ResultInfo[size];

		infos[0] = ConqueryConstants.DATES_INFO;

		for (Map.Entry<SecondaryIdDescription, Integer> e : secondaryIdPositions.entrySet()) {
			SecondaryIdDescription desc = e.getKey();
			Integer pos = e.getValue();
			infos[pos] = new SimpleResultInfo(desc.getLabel(), ResultType.IdT.INSTANCE);
		}

		for (Map.Entry<Column, Integer> entry : positions.entrySet()) {

			// 0 Position is date, already covered
			final int position = entry.getValue();

			// SecondaryIds are pulled to the front, already covered.
			final Column column = entry.getKey();

			if (position == 0 || column.getSecondaryId() != null) {
				continue;
			}

			infos[position] = new SimpleResultInfo(column.getTable().getLabel() + " - " + column.getLabel(), ResultType.resolveResultType(column.getType()));
		}

		return infos;
	}

	private static Column findValidityDateColumn(Connector connector, ValidityDateContainer dateColumn) {
		// if no dateColumn is provided, we use the default instead which is always the first one.
		// Set to null if none-available in the connector.
		if (dateColumn != null) {
			return dateColumn.getValue().getColumn();
		}

		if (!connector.getValidityDates().isEmpty()) {
			return connector.getValidityDates().get(0).getColumn();
		}

		return null;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		if (resultInfos == null) {
			return;
		}

		for (ResultInfo resultInfo : resultInfos) {
			collector.add(resultInfo);
		}
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}