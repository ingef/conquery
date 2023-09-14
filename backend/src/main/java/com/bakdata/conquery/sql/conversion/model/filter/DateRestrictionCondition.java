package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
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
