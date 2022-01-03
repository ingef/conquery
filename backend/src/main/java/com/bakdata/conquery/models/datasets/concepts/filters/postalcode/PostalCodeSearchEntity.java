package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.bakdata.conquery.io.jackson.InternalOnly;
import lombok.Data;

@Data
public class PostalCodeSearchEntity {


	@Size(min = 4, max = 5)
	@NotNull
	private String plz;

	@Min(0)
	private double radius;

}
