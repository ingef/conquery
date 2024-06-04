package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
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

@Value
@With
@Builder(toBuilder = true)
public class ConversionContext implements Context {

	DatabaseConfig config;

	NodeConversions nodeConversions;

	SqlDialect sqlDialect;

	NameGenerator nameGenerator;

	@Singular
	List<QueryStep> querySteps;

	@Nullable
	SqlQuery finalQuery;

	@Nullable
	QueryStep stratificationTable;

	/**
	 * An optional date restriction range. Is set when converting a {@link CQDateRestriction}.
	 */
	@Nullable
	CDateRange dateRestrictionRange;

	/**
	 * An optional secondary id to group results by in addition to the primary id. Only set when converting {@link SecondaryIdQuery}s.
	 */
	@Nullable
	SecondaryIdDescription secondaryIdDescription;

	boolean negation;

	boolean isGroupBy;

	public boolean dateRestrictionActive() {
		return this.dateRestrictionRange != null;
	}

	public boolean isWithStratification() {
		return this.stratificationTable != null;
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

	@Override
	public ConversionContext getConversionContext() {
		return this;
	}

	/**
	 * Get the last query {@link QueryStep} that has been added to this context query steps.
	 */
	public QueryStep getLastConvertedStep() {
		return this.querySteps.get(this.querySteps.size() - 1);
	}

}
