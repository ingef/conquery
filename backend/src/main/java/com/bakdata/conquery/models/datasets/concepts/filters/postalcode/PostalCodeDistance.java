package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import lombok.Data;

@Data
public class PostalCodeDistance {
	/**
	 * First postal code
	 */
	final private int left;

	/**
	 * Second postal code
	 */
	final private int right;

	/**
	 * Distance between both postal codes
	 */
	final private double distanceInKm;
}
