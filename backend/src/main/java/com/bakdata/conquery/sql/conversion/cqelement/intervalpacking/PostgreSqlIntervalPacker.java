package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;

/**
 * PostgreSql supports interval packing with a native range function.
 * <p>
 * See <a href="https://www.postgresql.org/docs/current/rangetypes.html">&& range function operator</a>
 */
@RequiredArgsConstructor
public class PostgreSqlIntervalPacker implements IntervalPacker {

	private final SqlFunctionProvider functionProvider;

	@Override
	public QueryStep createIntervalPackingSteps(IntervalPackingContext context) {

		String sourceTableName = context.getIntervalPackingTables().getRootTable();
		Field<Object> primaryColumn = QualifyingUtil.qualify(context.getPrimaryColumn(), sourceTableName);
		ColumnDateRange qualifiedValidityDate = context.getValidityDate().qualify(sourceTableName);
		ColumnDateRange aggregatedValidityDate = this.functionProvider.aggregated(qualifiedValidityDate)
																	  .asValidityDateRange(context.getNodeLabel());

		Selects selectsWithAggregatedValidityDate = new Selects(
				primaryColumn,
				Optional.of(aggregatedValidityDate),
				Collections.emptyList()
		);

		return QueryStep.builder()
						.cteName(context.getIntervalPackingTables().cteName(IntervalPackingStep.INTERVAL_COMPLETE))
						.selects(selectsWithAggregatedValidityDate)
						.fromTable(QueryStep.toTableLike(sourceTableName))
						.groupBy(List.of(primaryColumn))
						.build();
	}

}
