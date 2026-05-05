package com.bakdata.conquery.sql.conversion.model.select;

import static org.jooq.impl.DSL.field;

import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;

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
public class ClickhouseDistinctSelectConverter implements SelectConverter<DistinctSelect> {


	@Override
	public ConnectorSqlSelects connectorSelect(DistinctSelect distinctSelect, SelectContext<ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(distinctSelect);

		ConnectorSqlTables tables = selectContext.getTables();
		SingleColumnSqlSelect preprocessingSelect =
				MappableSingleColumnSelect.getSubstringSelect(distinctSelect.getColumn().get(), distinctSelect.getSubstringRange(), selectContext, alias);

		String eventFilterTable = selectContext.getTables().cteName(ConceptCteStep.EVENT_FILTER);
		SingleColumnSqlSelect qualified = preprocessingSelect.qualify(eventFilterTable);

		FieldWrapper<?> grouped = new FieldWrapper<>(field("arrayFilter(x -> x <> '' and x is not null, groupUniqArray({0}))", Object.class, qualified.select()).as(alias), qualified.select().getName());

		SingleColumnSqlSelect finalSelect = grouped.qualify(tables.cteName(ConceptCteStep.AGGREGATION_SELECT));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(preprocessingSelect)
								  .aggregationSelect(grouped)
								  .finalSelect(finalSelect)
								  .build();
	}


}
