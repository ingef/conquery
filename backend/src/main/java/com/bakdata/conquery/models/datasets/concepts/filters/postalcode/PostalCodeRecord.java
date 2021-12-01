package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import lombok.Data;

@Data
public class PostalCodeRecord {
	final private int plz1;
	final private int plz2;
	final private double distanceInKm;
}
