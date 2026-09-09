package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.sql.compiler.ir.condition.InclusiveRangeCondition;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Adapts legacy backend ranges to the connector-owned inclusive range condition.
 *
 * <p>TODO Remove this adapter when backend conversion produces connector model ranges directly.</p>
 */
@RequiredArgsConstructor
public final class LegacyInclusiveRangeCondition implements WhereCondition {

	private final Field<?> field;
	private final IRange<?, ?> range;

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Condition condition() {
		return new InclusiveRangeCondition((Field) field, range).condition();
	}
}
