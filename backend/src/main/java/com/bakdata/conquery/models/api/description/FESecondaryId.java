package com.bakdata.conquery.models.api.description;

import lombok.Data;

/**
 * SecondaryId structured as the frontend expects it.
 */
@Data
public class FESecondaryId {
	public final String id;
	public final String description;
}
