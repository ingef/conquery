package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;

import lombok.Data;

@Data
public class PostalCodeSearchEntity {

	@Min(0)
	final private double plz;

	@Min(0)
	final private double radius;

}
