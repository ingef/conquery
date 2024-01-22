package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;

/**
 * Marker for a conversion context.
 */
public interface Context {
	NameGenerator getNameGenerator();
}
