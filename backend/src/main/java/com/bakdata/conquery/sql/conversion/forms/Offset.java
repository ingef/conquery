package com.bakdata.conquery.sql.conversion.forms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * When calculating ranges by adding intervals, Offset is used so jump to the start or end of an interval by adding the offset.
 * <p>
 * Example for index = 1:
 * '2012-01-01'::date + (index + offset.getOffset()) * interval 3 month => 2012-01-01 for Offset.MINUS_ONE, 2012-04-01 for Offset.NONE
 */
@Getter
@RequiredArgsConstructor
enum Offset {
	MINUS_ONE(-1),
	NONE(0),
	ONE(1);
	private final int offset;
}
