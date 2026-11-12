package com.bakdata.conquery.sql.model.operation;

import jakarta.validation.constraints.NotBlank;

/**
 * Marker for an immutable, validated select operation.
 *
 * <p>Implementations may be supplied by extensions and are dispatched to matching SQL converters.</p>
 */
public interface ResolvedSelect {

	/** Stable technical name used to derive SQL aliases; it is not a presentation label. */
	@NotBlank
	String name();
}
