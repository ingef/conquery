package com.bakdata.conquery.sql.compiler.ir.concept;

import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.PREPROCESSING;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;

/** Assembles SQL-resolved connector operations into the input for the connector CTE pipeline. */
@UtilityClass
public class ConnectorCtePipelineAssembler {

	public ConnectorCtePipelineInput assemble(
			ConnectorCtePlan plan,
			SqlIdColumns ids,
			ColumnDateRange rawValidityDate,
			List<ConnectorSqlSelects> selects,
			List<SqlFilters> filters,
			Optional<QueryStep> stratificationTable
	) {
		List<ConnectorSqlSelects> allSqlSelects = Stream.concat(
				selects.stream(),
				filters.stream().map(SqlFilters::getSelects)
		).toList();
		ColumnDateRange validityDate = rawValidityDate.asValidityDateRange(plan.connectorName());
		PreprocessingCteInput preprocessing = new PreprocessingCteInput(
				plan.tables().getPredecessor(PREPROCESSING),
				ids,
				rawValidityDate,
				validityDate,
				allSqlSelects,
				filters,
				stratificationTable
		);
		List<SqlSelect> eventDateSelects = selects.stream()
				.flatMap(sqlSelects -> sqlSelects.getEventDateSelects().stream())
				.toList();
		List<QueryStep> additionalPredecessors = allSqlSelects.stream()
				.flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessor().stream())
				.toList();

		return new ConnectorCtePipelineInput(
				plan.tables(),
				preprocessing,
				eventDateSelects,
				additionalPredecessors,
				plan.withIntervalPacking(),
				plan.excludedFromTimeAggregation()
		);
	}
}
