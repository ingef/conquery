package com.bakdata.conquery.sql.conversion.context;

import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.NodeConversions;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import org.jooq.Record;
import org.jooq.Select;

@Value
@With
@Builder(toBuilder = true)
public class ConversionContext implements Context {

	SqlConnectorConfig config;
	NodeConversions nodeConversions;
	SqlDialect sqlDialect;
	@Singular
	List<QueryStep> querySteps;
	Select<Record> finalQuery;
	CDateRange dateRestrictionRange;
	int queryStepCounter;
	boolean negation;
	boolean isGroupBy;

	public boolean dateRestrictionActive() {
		return this.dateRestrictionRange != null;
	}

	/**
	 * Adds a converted {@link QueryStep} to the list of query steps of this {@link ConversionContext} and increments its conceptCounter by 1.
	 */
	public ConversionContext withQueryStep(QueryStep queryStep) {
		return this.toBuilder()
				.queryStep(queryStep)
				.queryStepCounter(queryStepCounter + 1)
				.build();
	}

}
