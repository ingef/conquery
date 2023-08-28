package com.bakdata.conquery.sql.conversion.cqelement.concept.model.filter;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterCondition;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.FilterType;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;

@RequiredArgsConstructor
public class DateRestrictionCondition implements FilterCondition {

	private final SqlFunctionProvider functionProvider;
	private final ColumnDateRange dateRestriction;
	private final ColumnDateRange validityDate;

	@Override
	public Condition filterCondition() {
		return this.functionProvider.dateRestriction(dateRestriction, validityDate);
	}

	@Override
	public FilterType type() {
		return FilterType.EVENT;
	}

}
