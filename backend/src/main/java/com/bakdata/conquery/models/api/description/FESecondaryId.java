package com.bakdata.conquery.models.api.description;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * SecondaryId structured as the frontend expects it.
 */
@Data
@RequiredArgsConstructor
public class FESecondaryId {
	public final String id;
	public final String label;
	public final String description;
}
