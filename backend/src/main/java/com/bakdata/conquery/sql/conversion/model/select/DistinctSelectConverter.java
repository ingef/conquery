package com.bakdata.conquery.sql.conversion.model.select;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
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
 *     				from "event_filter"
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
public class DistinctSelectConverter implements SelectConverter<DistinctSelect> {

	@Getter
	@RequiredArgsConstructor
	private enum DistinctSelectCteStep implements CteStep {

		DISTINCT_SELECT("distinct", null),
		STRING_AGG("aggregated", DISTINCT_SELECT);

		private final String suffix;
		private final DistinctSelectCteStep predecessor;
	}

	@Override
	public ConnectorSqlSelects connectorSelect(DistinctSelect distinctSelect, SelectContext<ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(distinctSelect);

		ConnectorSqlTables tables = selectContext.getTables();
		FieldWrapper<Object> preprocessingSelect = new FieldWrapper<>(field(name(tables.getRootTable(), distinctSelect.getColumn().getColumn())).as(alias));

		QueryStep distinctSelectCte = createDistinctSelectCte(preprocessingSelect, alias, selectContext);
		QueryStep aggregatedCte = createAggregationCte(selectContext, preprocessingSelect, distinctSelectCte, alias);

		ExtractingSqlSelect<Object> finalSelect = preprocessingSelect.qualify(tables.cteName(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(preprocessingSelect)
								  .additionalPredecessor(Optional.of(aggregatedCte))
								  .finalSelect(finalSelect)
								  .build();
	}

	private static QueryStep createAggregationCte(
			SelectContext<ConnectorSqlTables> selectContext,
			FieldWrapper<Object> preprocessingSelect,
			QueryStep distinctSelectCte,
			String alias
	) {
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		Field<String> castedColumn = functionProvider.cast(preprocessingSelect.qualify(distinctSelectCte.getCteName()).select(), SQLDataType.VARCHAR);
		Field<String> aggregatedColumn = functionProvider.stringAggregation(castedColumn, DSL.toChar(ResultSetProcessor.UNIT_SEPARATOR), List.of(castedColumn))
														 .as(alias);

		SqlIdColumns ids = distinctSelectCte.getQualifiedSelects().getIds();

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(new FieldWrapper<>(aggregatedColumn))
								 .build();

		return QueryStep.builder()
						.cteName(selectContext.getNameGenerator().cteStepName(DistinctSelectCteStep.STRING_AGG, alias))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(distinctSelectCte.getCteName()))
						.groupBy(ids.toFields())
						.predecessor(distinctSelectCte)
						.build();
	}

	private static QueryStep createDistinctSelectCte(
			FieldWrapper<Object> preprocessingSelect,
			String alias,
			SelectContext<ConnectorSqlTables> selectContext
	) {
		// values to aggregate must be event-filtered first
		String eventFilterTable = selectContext.getTables().cteName(ConceptCteStep.EVENT_FILTER);
		ExtractingSqlSelect<Object> qualified = preprocessingSelect.qualify(eventFilterTable);
		SqlIdColumns ids = selectContext.getIds().qualify(eventFilterTable);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(qualified)
								 .build();

		return QueryStep.builder()
						.cteName(selectContext.getNameGenerator().cteStepName(DistinctSelectCteStep.DISTINCT_SELECT, alias))
						.selectDistinct(true)
						.selects(selects)
						.fromTable(QueryStep.toTableLike(eventFilterTable))
						.build();
	}
}
