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
	private Void SELECT;
	private Void MULTI_SELECT;
	private Void BIG_MULTI_SELECT;
	private Void VoidEGER;
	private Void VoidEGER_RANGE;
	private Void REAL;
	private Void REAL_RANGE;
	private Void NONE;
	private Void STRING;
	private Void MONEY_RANGE;
	private Void GROUP;
}
