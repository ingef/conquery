package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * <pre>
 *  The two additional CTEs this aggregator creates:
 * 	<ol>
 * 	    <li>
 * 	        Select distinct values of a column.
 *            {@code
 * 	        	"distinct" as (
 *     				select distinct "pid", "column"
 *     				from "preprocessing"
 *  			)
 *            }
 * 	    </li>
 * 	    <li>
 * 	        String agg all distinct values of the column.
 *            {@code
 * 	        "aggregated" as (
 *    			 select
 *    			   "select-1-distinct"."pid",
 *    			   string_agg(cast("column" as varchar), cast(' ' as varchar) ) as "select-1"
 *    			 from "distinct"
 *    			 group by "pid"
 *   			)
 *            }
 * 	    </li>
 * 	</ol>
 * </pre>
 */
public class DistinctSelectConverter {

	@Getter
	@RequiredArgsConstructor
	private enum DistinctSelectCteStep implements CteStep {

		DISTINCT_SELECT("distinct", null),
		STRING_AGG("aggregated", DISTINCT_SELECT);

		private final String suffix;
		private final DistinctSelectCteStep predecessor;
	}


	public static ConnectorSqlSelects connectorSelect(BuiltInSelects.Values distinctSelect, SelectConversionContext selectContext) {

		String alias = selectContext.alias();

		SqlTables tables = selectContext.tables();
		SingleColumnSqlSelect preprocessingSelect = SubstringSelect.getSubstringSelect(distinctSelect.column(), distinctSelect.substring(), selectContext.tables().getRootTable(), alias);

		QueryStep distinctSelectCte = createDistinctSelectCte(preprocessingSelect, alias, selectContext);
		QueryStep aggregatedCte = createAggregationCte(selectContext, preprocessingSelect, distinctSelectCte, alias);

		SingleColumnSqlSelect finalSelect = preprocessingSelect.qualify(tables.cteName(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(preprocessingSelect)
								  .additionalPredecessor(Optional.of(aggregatedCte))
								  .finalSelect(finalSelect)
								  .build();
	}

	private static QueryStep createAggregationCte(
			SelectConversionContext selectContext,
			SingleColumnSqlSelect preprocessingSelect,
			QueryStep distinctSelectCte,
			String alias
	) {
		CompilerDialect functionProvider = selectContext.dialect();
		Field<String> castedColumn = functionProvider.cast(preprocessingSelect.qualify(distinctSelectCte.getCteName()).select(), SQLDataType.VARCHAR);
		Field<String> aggregatedColumn = functionProvider.stringAggregation(castedColumn, DSL.toChar((char) 31), List.of(castedColumn))
														 .as(alias);

		SqlIdColumns ids = distinctSelectCte.getQualifiedSelects().getIds();

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(new FieldWrapper<>(aggregatedColumn))
								 .build();

		return QueryStep.builder()
						.cteName(selectContext.nameGenerator().cteStepName(DistinctSelectCteStep.STRING_AGG, alias))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(distinctSelectCte.getCteName()))
						.groupBy(ids.toFields())
						.predecessor(distinctSelectCte)
						.build();
	}

	private static QueryStep createDistinctSelectCte(
			SingleColumnSqlSelect preprocessingSelect,
			String alias,
			SelectConversionContext selectContext
	) {
		// values to aggregate must be event-filtered first
		String preprocessingTable = selectContext.tables().cteName(ConceptCteStep.PREPROCESSING);
		SingleColumnSqlSelect qualified = preprocessingSelect.qualify(preprocessingTable);

		SqlIdColumns ids = selectContext.ids().qualify(preprocessingTable);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(qualified)
								 .build();

		return QueryStep.builder()
						.cteName(selectContext.nameGenerator().cteStepName(DistinctSelectCteStep.DISTINCT_SELECT, alias))
						.selectDistinct(true)
						.selects(selects)
						.fromTable(QueryStep.toTableLike(preprocessingTable))
						.build();
	}
}
