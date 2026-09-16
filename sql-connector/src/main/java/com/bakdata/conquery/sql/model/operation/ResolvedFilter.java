package com.bakdata.conquery.sql.model.operation;

import jakarta.validation.constraints.NotBlank;

/**
 * Marker for an immutable, validated filter operation.
 *
 * <p>Implementations may be supplied by extensions. They must contain resolved columns and typed values rather than
 * repository identifiers.</p>
 */
public interface ResolvedFilter {

	/** Stable technical name used to derive internal SQL aliases; it is not a presentation label. */
	@NotBlank
	String name();
}
