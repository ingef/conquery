package com.bakdata.conquery.sql.compiler.ir.condition;

import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;

/** Matches validity dates that overlap a date restriction with exclusive range ends. */
@RequiredArgsConstructor
public class DateRestrictionCondition implements WhereCondition {

	private final ColumnDateRange dateRestriction;
	private final ColumnDateRange validityDate;

	@Override
	public Condition condition() {
		Condition restrictionStartsBeforeValidityEnds = dateRestriction.getStart().lessThan(validityDate.getEnd());
		Condition restrictionEndsAfterValidityStarts = dateRestriction.getEnd().greaterThan(validityDate.getStart());

		return restrictionStartsBeforeValidityEnds.and(restrictionEndsAfterValidityStarts);
	}
}
