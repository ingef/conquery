package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.NonNull;

@Data
public class PostalCodeSearchEntity {

	@NonNull
	@NotEmpty
	@Size(min = 5, max = 5)
	final private String plz;

	@Min(0)
	final private double radius;

}
