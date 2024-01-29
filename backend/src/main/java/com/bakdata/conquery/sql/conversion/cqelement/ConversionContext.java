package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.NodeConversions;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import org.jooq.Field;

@Value
@With
@Builder(toBuilder = true)
public class ConversionContext implements Context {

	SqlConnectorConfig config;
	NodeConversions nodeConversions;
	SqlDialect sqlDialect;
	NameGenerator nameGenerator;
	@Singular
	List<QueryStep> querySteps;
	SqlQuery finalQuery;
	Field<Object> primaryColumn;
	CDateRange dateRestrictionRange;
	boolean negation;
	boolean isGroupBy;

	public boolean dateRestrictionActive() {
		return this.dateRestrictionRange != null;
	}

	/**
	 * Adds a query step to the list of {@link QueryStep} of this context.
	 */
	public ConversionContext withQueryStep(QueryStep queryStep) {
		return this.toBuilder().queryStep(queryStep).build();
	}

	/**
	 * @return Creates a child context that keeps the meta information of this context, but empties the list of already converted querySteps.
	 */
	public ConversionContext createChildContext() {
		return this.toBuilder().clearQuerySteps().build();
	}

}
