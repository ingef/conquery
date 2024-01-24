package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;

@RequiredArgsConstructor
public class DateRestrictionCondition implements WhereCondition {

	private final SqlFunctionProvider functionProvider;
	private final ColumnDateRange dateRestriction;
	private final ColumnDateRange validityDate;

	@Override
	public Condition condition() {
		return this.functionProvider.dateRestriction(dateRestriction, validityDate);
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}
