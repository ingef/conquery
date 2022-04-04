package com.bakdata.conquery.apiv1.frontend;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * This class contains the types of filters that the frontend knows.
 * It is only accessed using the FieldNameConstants when building the frontend configuration and for the deserialization of {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue}.
 * <p>
 * This way, the filter types may be extended and their deserialization id can be referenced instead of being loosely hardcoded.
 */
@FieldNameConstants
@NoArgsConstructor(access = AccessLevel.NONE)
public final class FEFilterType {
	private int SELECT;
	private int MULTI_SELECT;
	private int BIG_MULTI_SELECT;
	private int INTEGER;
	private int INTEGER_RANGE;
	private int REAL;
	private int REAL_RANGE;
	private int NONE;
	private int STRING;
	private int MONEY_RANGE;
	private int GROUP;
}
