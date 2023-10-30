package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.NodeConversions;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;

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
	CDateRange dateRestrictionRange;
	boolean negation;
	boolean isGroupBy;

	public boolean dateRestrictionActive() {
		return this.dateRestrictionRange != null;
	}

}
