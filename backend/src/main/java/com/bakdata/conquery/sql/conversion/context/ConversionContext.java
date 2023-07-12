package com.bakdata.conquery.sql.conversion.context;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.conversion.NodeConverterService;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import org.jooq.Record;
import org.jooq.Select;

import java.util.List;

@Value
@With
@Builder(toBuilder = true)
public class ConversionContext {

	SqlConnectorConfig config;
	NodeConverterService nodeConverterService;
	SqlDialect sqlDialect;
	@Singular
	List<QueryStep> querySteps;
	Select<Record> finalQuery;
	boolean negation;
	CDateRange dateRestrictionRange;
	int queryStepCounter;


	public boolean dateRestrictionActive() {
		return this.dateRestrictionRange != null;
	}

	public ConversionContext withQueryStep(QueryStep queryStep) {
		return this.toBuilder()
				   .queryStep(queryStep)
				   .queryStepCounter(queryStepCounter + 1)
				   .build();
	}

}
