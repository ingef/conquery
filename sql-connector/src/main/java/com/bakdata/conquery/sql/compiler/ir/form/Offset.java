package com.bakdata.conquery.sql.compiler.ir.form;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Offset applied when a form compiler moves a date to an interval boundary.
 * <p>
 * Example for index = 1:
 * '2012-01-01'::date + (index + offset.getOffset()) * interval 3 month => 2012-01-01 for Offset.MINUS_ONE, 2012-04-01 for Offset.NONE
 */
@Getter
@RequiredArgsConstructor
public enum Offset {
	MINUS_ONE(-1),
	NONE(0),
	ONE(1);
	private final int offset;
}
