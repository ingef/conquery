package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class PostalCodeSearchEntity {


	@Size(min = 4, max = 5)
	final private String plz;

	@Min(0)
	final private double radius;

}
